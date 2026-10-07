param(
    [int] $TimeoutSeconds = 60
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $projectRoot 'docker-compose.prod.yml'

docker info 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop chưa sẵn sàng. Hãy mở Docker Desktop, chờ trạng thái Running rồi chạy lại lệnh này.'
}

Write-Host 'Đang khởi động SmartMenu và Cloudflare Quick Tunnel...'
docker compose -f $composeFile --profile quick-tunnel up -d
if ($LASTEXITCODE -ne 0) {
    throw 'Không thể khởi động Docker Compose.'
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
do {
    $logs = docker compose -f $composeFile --profile quick-tunnel logs --no-color quick-tunnel 2>&1
    $urls = [regex]::Matches(($logs -join "`n"), 'https://[a-z0-9-]+\.trycloudflare\.com')

    if ($urls.Count -gt 0) {
        $url = $urls[$urls.Count - 1].Value
        Write-Host ''
        Write-Host 'SmartMenu đang public tại:' -ForegroundColor Green
        Write-Host $url -ForegroundColor Cyan
        Write-Host ''
        Write-Host 'URL chỉ hoạt động khi Docker Desktop và tunnel còn chạy.' -ForegroundColor Yellow
        exit 0
    }

    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)

throw "Cloudflare chưa tạo URL sau $TimeoutSeconds giây. Kiểm tra log bằng: docker compose -f `"$composeFile`" --profile quick-tunnel logs quick-tunnel"
