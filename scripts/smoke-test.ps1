param(
    [string]$BaseUrl = "http://localhost:8081",
    [int]$MaxRetries = 24,
    [int]$SleepSeconds = 5
)

$ErrorActionPreference = "Stop"

Write-Host "[smoke-test] Checking health probe at $BaseUrl/actuator/health ..."
$healthy = $false
for ($i = 1; $i -le $MaxRetries; $i++) {
    try {
        $resp = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get -TimeoutSec 5
        if ($resp.status -eq "UP") {
            Write-Host "[smoke-test] Health check PASSED (attempt $i/$MaxRetries)."
            $healthy = $true
            break
        }
    } catch {
        # Retry until ready
    }
    Start-Sleep -Seconds $SleepSeconds
}

if (-not $healthy) {
    Write-Error "[smoke-test] ERROR: Health check failed after $MaxRetries attempts."
    exit 1
}

$info = Invoke-RestMethod -Uri "$BaseUrl/actuator/info" -Method Get
Write-Host "[smoke-test] Deployed info: $($info | ConvertTo-Json -Compress)"

$loginResp = Invoke-WebRequest -Uri "$BaseUrl/login" -UseBasicParsing
if ($loginResp.StatusCode -ne 200) {
    Write-Error "[smoke-test] ERROR: /login returned $($loginResp.StatusCode)"
    exit 1
}

$booksResp = Invoke-WebRequest -Uri "$BaseUrl/api/books" -UseBasicParsing
if ($booksResp.StatusCode -ne 200) {
    Write-Error "[smoke-test] ERROR: /api/books returned $($booksResp.StatusCode)"
    exit 1
}

Write-Host "[smoke-test] ALL SMOKE TESTS PASSED on $BaseUrl"
