param(
    [string] $ApiUrl = 'http://localhost:8080/api/v1',
    [Parameter(Mandatory = $true)] [string] $ExcelPath,
    [Parameter(Mandatory = $true)] [string] $MenuImagePath
)

$ErrorActionPreference = 'Stop'

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

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
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

$form = @{
    excelFile = Get-Item -LiteralPath $ExcelPath
    menuImage = Get-Item -LiteralPath $MenuImagePath
}
$uploaded = Invoke-RestMethod -Method Post -Uri "$ApiUrl/upload/menu-analysis" -Form $form -WebSession $session
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

$style = Invoke-RestMethod -Method Post -Uri "$ApiUrl/smartcoffee/menu/$sessionId/style" -ContentType 'application/json' -Body '{"styleId":"HIEN_DAI"}' -WebSession $session
Assert-Success $style 'Đổi phong cách miễn phí'

$finalMenu = Invoke-RestMethod -Method Post -Uri "$ApiUrl/smartcoffee/menu/$sessionId/generate-final-menu" -ContentType 'application/json' -Body '{"styleId":"HIEN_DAI"}' -WebSession $session
Assert-Success $finalMenu 'Tạo menu cuối cùng'

$generatedMenu = Invoke-RestMethod -Method Get -Uri "$ApiUrl/smartcoffee/menu/$sessionId/generated" -WebSession $session
Assert-Success $generatedMenu 'Lấy menu đã tạo'

$premiumStyle = Invoke-WebRequest -Method Post -Uri "$ApiUrl/smartcoffee/menu/$sessionId/style" -ContentType 'application/json' -Body '{"styleId":"SANG_TRONG"}' -WebSession $session -SkipHttpErrorCheck
Assert-StatusCode $premiumStyle 402 'Chặn phong cách Plus đối với tài khoản FREE'

$accessAfterFree = Invoke-RestMethod -Method Get -Uri "$ApiUrl/service/check-access" -WebSession $session
Assert-Success $accessAfterFree 'Kiểm tra quyền sau lượt miễn phí'
if ($accessAfterFree.data.type -ne 'PAYMENT_REQUIRED') { throw "Sau lượt miễn phí phải yêu cầu thanh toán, nhận được: $($accessAfterFree.data.type)" }

$payment = Invoke-RestMethod -Method Post -Uri "$ApiUrl/payment/create" -WebSession $session
Assert-Success $payment 'Tạo giao dịch thanh toán'
$paymentStatus = Invoke-RestMethod -Method Get -Uri "$ApiUrl/payment/check-status/$($payment.data.paymentId)" -WebSession $session
Assert-Success $paymentStatus 'Kiểm tra giao dịch thanh toán'
if ($paymentStatus.data.status -ne 'PENDING') { throw "Giao dịch mới phải ở PENDING, nhận được: $($paymentStatus.data.status)" }

$otherSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$otherEmail = "forbidden_$([guid]::NewGuid().ToString('N').Substring(0, 12))@smartmenu.test"
$otherRegister = @{ fullName = 'Other User'; email = $otherEmail; phone = '0900000001'; password = $password } | ConvertTo-Json
$otherRegistered = Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/register" -ContentType 'application/json' -Body $otherRegister -WebSession $otherSession
Assert-Success $otherRegistered 'Đăng ký tài khoản kiểm tra phân quyền'
$otherLogin = @{ email = $otherEmail; password = $password; rememberMe = $false } | ConvertTo-Json
$otherLoggedIn = Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/login" -ContentType 'application/json' -Body $otherLogin -WebSession $otherSession
Assert-Success $otherLoggedIn 'Đăng nhập tài khoản kiểm tra phân quyền'
$forbidden = Invoke-WebRequest -Method Get -Uri "$ApiUrl/smartcoffee/analysis/$sessionId" -WebSession $otherSession -SkipHttpErrorCheck
Assert-StatusCode $forbidden 403 'Chặn truy cập session của người dùng khác'

$logout = Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/logout" -WebSession $session
Assert-Success $logout 'Đăng xuất'

Write-Output "PASS: xác thực, phân quyền, upload 2 dữ liệu thô, phân tích, blueprint/menu, giới hạn Plus, thanh toán PENDING và cách ly dữ liệu. SessionId=$sessionId"
