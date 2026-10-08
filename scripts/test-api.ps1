param(
    [string] $ApiUrl = 'http://localhost:8080/api/v1',
    [Parameter(Mandatory = $true)] [string] $ExcelPath,
    [Parameter(Mandatory = $true)] [string] $MenuImagePath
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

if (!(Test-Path -LiteralPath $ExcelPath)) { throw "Không tìm thấy Excel: $ExcelPath" }
if (!(Test-Path -LiteralPath $MenuImagePath)) { throw "Không tìm thấy ảnh menu: $MenuImagePath" }

function Assert-Success($response, [string] $step) {
    if (!$response.success) { throw "$step thất bại: $($response.message)" }
}

function Assert-StatusCode($response, [int] $expected, [string] $step) {
    if ($response.StatusCode -ne $expected) {
        throw "$step phải trả HTTP $expected, nhận được HTTP $($response.StatusCode)"
    }
}

function Wait-ForApi([string] $baseUrl, [int] $timeoutSeconds = 60) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -Method Get -Uri "$baseUrl/auth/me" -TimeoutSec 5 -UseBasicParsing
            if ($response.StatusCode -eq 200) { return }
        }
        catch {
            # A 502 means nginx is up but Spring Boot is still booting. Keep
            # waiting rather than registering a false test failure.
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "API did not become ready within $timeoutSeconds seconds: $baseUrl"
}

function Invoke-HttpWithStatus(
    [string] $Method,
    [string] $Uri,
    [Microsoft.PowerShell.Commands.WebRequestSession] $WebSession,
    [string] $ContentType = $null,
    [string] $Body = $null
) {
    $parameters = @{
        Method = $Method
        Uri = $Uri
        WebSession = $WebSession
        ErrorAction = 'Stop'
    }
    if ($ContentType) { $parameters.ContentType = $ContentType }
    if ($PSBoundParameters.ContainsKey('Body')) { $parameters.Body = $Body }

    try {
        return Invoke-WebRequest @parameters
    }
    catch {
        $response = $_.Exception.Response
        if ($null -eq $response) { throw }

        $content = ''
        if ($response.GetResponseStream) {
            $reader = [System.IO.StreamReader]::new($response.GetResponseStream())
            try { $content = $reader.ReadToEnd() }
            finally { $reader.Dispose() }
        }
        return [pscustomobject]@{
            StatusCode = [int] $response.StatusCode
            Content = $content
        }
    }
}

function Invoke-MultipartUpload(
    [string] $Uri,
    [Microsoft.PowerShell.Commands.WebRequestSession] $WebSession,
    [hashtable] $Files
) {
    $targetUri = [Uri] $Uri
    $cookieContainer = [System.Net.CookieContainer]::new()
    foreach ($cookie in $WebSession.Cookies.GetCookies($targetUri)) {
        $cookieContainer.Add($targetUri, [System.Net.Cookie]::new($cookie.Name, $cookie.Value, $cookie.Path, $targetUri.Host))
    }

    $handler = [System.Net.Http.HttpClientHandler]::new()
    $handler.CookieContainer = $cookieContainer
    $client = [System.Net.Http.HttpClient]::new($handler)
    $form = [System.Net.Http.MultipartFormDataContent]::new()

    try {
        foreach ($entry in $Files.GetEnumerator()) {
            $file = Get-Item -LiteralPath $entry.Value
            $content = [System.Net.Http.StreamContent]::new([System.IO.File]::OpenRead($file.FullName))
            $content.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('application/octet-stream')
            $form.Add($content, $entry.Key, $file.Name)
        }

        $response = $client.PostAsync($targetUri, $form).GetAwaiter().GetResult()
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (!$response.IsSuccessStatusCode) {
            throw "Upload failed with HTTP $([int] $response.StatusCode): $body"
        }
        return $body | ConvertFrom-Json
    }
    finally {
        $form.Dispose()
        $client.Dispose()
        $handler.Dispose()
    }
}

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Wait-ForApi -baseUrl $ApiUrl
$email = "integration_$([guid]::NewGuid().ToString('N').Substring(0, 12))@smartmenu.test"
$password = 'TestPassword123!'

$registerBody = @{ fullName = 'Integration Test'; email = $email; phone = '0900000000'; password = $password } | ConvertTo-Json
$registered = Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/register" -ContentType 'application/json' -Body $registerBody -WebSession $session
Assert-Success $registered 'Đăng ký'

$loginBody = @{ email = $email; password = $password; rememberMe = $false } | ConvertTo-Json
$loggedIn = Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/login" -ContentType 'application/json' -Body $loginBody -WebSession $session
Assert-Success $loggedIn 'Đăng nhập'

$currentUser = Invoke-RestMethod -Method Get -Uri "$ApiUrl/auth/me" -WebSession $session
Assert-Success $currentUser 'Lấy người dùng hiện tại'

$access = Invoke-RestMethod -Method Get -Uri "$ApiUrl/service/check-access" -WebSession $session
Assert-Success $access 'Kiểm tra quyền truy cập'
if ($access.data.type -ne 'FREE') { throw "Tài khoản mới phải có lượt FREE, nhận được: $($access.data.type)" }

$uploaded = Invoke-MultipartUpload -Uri "$ApiUrl/upload/menu-analysis" -WebSession $session -Files @{
    excelFile = $ExcelPath
    menuImage = $MenuImagePath
}
Assert-Success $uploaded 'Tải dữ liệu'
$sessionId = $uploaded.data.sessionId
if (!$sessionId) { throw 'Tải dữ liệu không trả về sessionId' }

$analysis = Invoke-RestMethod -Method Post -Uri "$ApiUrl/smartcoffee/analyze-full/$sessionId" -ContentType 'application/json' -Body '{}' -WebSession $session
Assert-Success $analysis 'Phân tích menu'
if (!$analysis.data.products -or $analysis.data.products.Count -lt 1) { throw 'Phân tích không trả về sản phẩm' }

$storedAnalysis = Invoke-RestMethod -Method Get -Uri "$ApiUrl/smartcoffee/analysis/$sessionId" -WebSession $session
Assert-Success $storedAnalysis 'Lấy lại kết quả phân tích'

$strategy = Invoke-RestMethod -Method Get -Uri "$ApiUrl/smartcoffee/menu/$sessionId/strategy" -WebSession $session
Assert-Success $strategy 'Lấy chiến lược AI'

$blueprint = Invoke-RestMethod -Method Get -Uri "$ApiUrl/smartcoffee/menu/$sessionId/blueprint" -WebSession $session
Assert-Success $blueprint 'Lấy blueprint menu'

$modernTemplateStyle = Invoke-RestMethod -Method Post -Uri "$ApiUrl/smartcoffee/menu/$sessionId/style" -ContentType 'application/json' -Body '{"styleId":"COFFEE_MODERN_01"}' -WebSession $session
Assert-Success $modernTemplateStyle 'Dùng mã template Hiện Đại cho tài khoản miễn phí'

$modernArtwork = Invoke-RestMethod -Method Post -Uri "$ApiUrl/smartcoffee/menu/$sessionId/generate-ai-menu-artwork" -ContentType 'application/json' -Body '{"styleId":"COFFEE_MODERN_01"}' -WebSession $session
Assert-Success $modernArtwork 'Tạo artwork Hiện Đại bằng mã template'
if (!$modernArtwork.data.imageUrl) { throw 'Artwork Hiện Đại không trả về imageUrl' }

$style = Invoke-RestMethod -Method Post -Uri "$ApiUrl/smartcoffee/menu/$sessionId/style" -ContentType 'application/json' -Body '{"styleId":"HIEN_DAI"}' -WebSession $session
Assert-Success $style 'Đổi phong cách miễn phí'

$finalMenu = Invoke-RestMethod -Method Post -Uri "$ApiUrl/smartcoffee/menu/$sessionId/generate-final-menu" -ContentType 'application/json' -Body '{"styleId":"HIEN_DAI"}' -WebSession $session
Assert-Success $finalMenu 'Tạo menu cuối cùng'
if (!$finalMenu.data.finalImageUrl) { throw 'Tạo menu cuối cùng không trả về finalImageUrl' }

$finalImageUrl = [string] $finalMenu.data.finalImageUrl
if ($finalImageUrl -match '^data:image/') {
    if ($finalImageUrl.Length -lt 1000) { throw 'Artwork menu dạng data URL quá ngắn hoặc không hợp lệ' }
}
else {
    if ($finalImageUrl -notmatch '^https?://') {
    $apiUri = [Uri] $ApiUrl
    $finalImageUrl = "$($apiUri.Scheme)://$($apiUri.Authority)$finalImageUrl"
    }
    $finalImage = Invoke-HttpWithStatus -Method Get -Uri $finalImageUrl -WebSession $session
    Assert-StatusCode $finalImage 200 'Tải artwork menu qua domain đang kiểm thử'
}

$generatedMenu = Invoke-RestMethod -Method Get -Uri "$ApiUrl/smartcoffee/menu/$sessionId/generated" -WebSession $session
Assert-Success $generatedMenu 'Lấy menu đã tạo'

$premiumStyle = Invoke-HttpWithStatus -Method Post -Uri "$ApiUrl/smartcoffee/menu/$sessionId/style" -ContentType 'application/json' -Body '{"styleId":"SANG_TRONG"}' -WebSession $session
Assert-StatusCode $premiumStyle 402 'Chặn phong cách Plus đối với tài khoản FREE'

$accessAfterFree = Invoke-RestMethod -Method Get -Uri "$ApiUrl/service/check-access" -WebSession $session
Assert-Success $accessAfterFree 'Kiểm tra quyền sau lượt miễn phí'
if ($accessAfterFree.data.type -ne 'PAYMENT_REQUIRED') { throw "Sau lượt miễn phí phải yêu cầu thanh toán, nhận được: $($accessAfterFree.data.type)" }

$payment = Invoke-RestMethod -Method Post -Uri "$ApiUrl/payment/create" -WebSession $session
Assert-Success $payment 'Tạo giao dịch thanh toán'
$paymentStatus = Invoke-RestMethod -Method Get -Uri "$ApiUrl/payment/check-status/$($payment.data.paymentId)" -WebSession $session
Assert-Success $paymentStatus 'Kiểm tra giao dịch thanh toán'
if ($paymentStatus.data.status -ne 'PENDING') { throw "Giao dịch mới phải ở PENDING, nhận được: $($paymentStatus.data.status)" }

$unauthenticatedWebhook = Invoke-HttpWithStatus -Method Post -Uri "$ApiUrl/payment/callback" -ContentType 'application/json' -Body '{}'
Assert-StatusCode $unauthenticatedWebhook 401 'Chặn webhook thanh toán không có token'

$mockConfirmation = Invoke-HttpWithStatus -Method Post -Uri "$ApiUrl/payment/mock-confirm/$($payment.data.paymentId)" -WebSession $session
Assert-StatusCode $mockConfirmation 404 'Tắt xác nhận thanh toán giả ở production'

$otherSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$otherEmail = "forbidden_$([guid]::NewGuid().ToString('N').Substring(0, 12))@smartmenu.test"
$otherRegister = @{ fullName = 'Other User'; email = $otherEmail; phone = '0900000001'; password = $password } | ConvertTo-Json
$otherRegistered = Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/register" -ContentType 'application/json' -Body $otherRegister -WebSession $otherSession
Assert-Success $otherRegistered 'Đăng ký tài khoản kiểm tra phân quyền'
$otherLogin = @{ email = $otherEmail; password = $password; rememberMe = $false } | ConvertTo-Json
$otherLoggedIn = Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/login" -ContentType 'application/json' -Body $otherLogin -WebSession $otherSession
Assert-Success $otherLoggedIn 'Đăng nhập tài khoản kiểm tra phân quyền'
$forbidden = Invoke-HttpWithStatus -Method Get -Uri "$ApiUrl/smartcoffee/analysis/$sessionId" -WebSession $otherSession
Assert-StatusCode $forbidden 403 'Chặn truy cập session của người dùng khác'

$logout = Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/logout" -WebSession $session
Assert-Success $logout 'Đăng xuất'

Write-Output "PASS: xác thực, phân quyền, upload 2 dữ liệu thô, phân tích, blueprint/menu, giới hạn Plus, thanh toán PENDING và cách ly dữ liệu. SessionId=$sessionId"
