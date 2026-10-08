param(
    # A new trycloudflare hostname may need more than a minute for DNS to
    # propagate. The script only prints a URL after an HTTPS request succeeds.
    [int] $TimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $projectRoot 'docker-compose.prod.yml'

function Wait-ForApplication([string] $baseUrl, [datetime] $deadline) {
    do {
        try {
            # This reaches Spring Boot through nginx, so the tunnel is not
            # published until both the frontend and backend are usable.
            $response = Invoke-WebRequest -Uri "$baseUrl/api/v1/auth/me" -TimeoutSec 5 -MaximumRedirection 0 -UseBasicParsing
            if ($response.StatusCode -eq 200) { return }
        }
        catch {
            # Containers can be running while Spring Boot is still starting.
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "SmartMenu did not become ready before the $TimeoutSeconds-second timeout. Check: docker compose -f `"$composeFile`" logs backend"
}

docker info 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop is not ready. Start Docker Desktop, wait for Running, then run this script again.'
}

Write-Host 'Starting SmartMenu...'
docker compose -f $composeFile up -d db backend frontend
if ($LASTEXITCODE -ne 0) {
    throw 'Could not start Docker Compose.'
}

$publishedPort = docker compose -f $composeFile port frontend 80
if ($LASTEXITCODE -ne 0 -or $publishedPort -notmatch ':(\d+)$') {
    throw 'Could not determine the frontend port from Docker Compose.'
}
$localBaseUrl = "http://localhost:$($Matches[1])"
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
Write-Host 'Waiting for SmartMenu to become ready...'
Wait-ForApplication -baseUrl $localBaseUrl -deadline $deadline

Write-Host 'Creating a fresh Cloudflare Quick Tunnel URL...'
# Quick Tunnels do not have a permanent hostname.  Recreating only this
# container prevents the command from returning a stale URL after a network
# interruption or a reboot, without touching the database/application data.
docker compose -f $composeFile --profile quick-tunnel up -d --force-recreate quick-tunnel
if ($LASTEXITCODE -ne 0) {
    throw 'Could not start Cloudflare Quick Tunnel.'
}

do {
    $logs = docker compose -f $composeFile --profile quick-tunnel logs --no-color quick-tunnel 2>&1
    $logText = $logs -join "`n"

    if ($logText -match 'precheck complete hard_fail=true') {
        throw 'Cloudflare Tunnel is blocked by this network. Allow outbound UDP or TCP port 7844, or switch to a network that permits Cloudflare Tunnel.'
    }

    $urls = [regex]::Matches($logText, 'https://[a-z0-9-]+\.trycloudflare\.com')

    if ($urls.Count -gt 0) {
        $url = $urls[$urls.Count - 1].Value
        try {
            $response = Invoke-WebRequest -Uri $url -TimeoutSec 10 -MaximumRedirection 0
            if ($response.StatusCode -eq 200) {
                Write-Host ''
                Write-Host 'SmartMenu public URL:' -ForegroundColor Green
                Write-Host $url -ForegroundColor Cyan
                Write-Host ''
                Write-Host 'This URL works only while Docker Desktop and the tunnel are running.' -ForegroundColor Yellow
                exit 0
            }
        }
        catch {
            # cloudflared prints the URL before its first edge connection is
            # always ready, so continue waiting until the public request works.
        }
    }

    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)

throw "Cloudflare did not create a reachable URL after $TimeoutSeconds seconds. Check logs with: docker compose -f `"$composeFile`" --profile quick-tunnel logs quick-tunnel"
