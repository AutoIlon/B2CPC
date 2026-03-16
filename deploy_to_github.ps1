param (
    [string]$version = "1.0.0"
)

$token = "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"
$owner = "AutoIlon"
$repo = "B2CPC"

Write-Host "=========================================" -ForegroundColor Green
Write-Host " B2CPC Build & GitHub Release Automation " -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""

# 1. EXE Build
Write-Host "[1/3] Starting Windows EXE packaging for v$version..." -ForegroundColor Cyan
.\gradle-8.5\bin\gradle.bat packageReleaseExe "-PappVersion=$version"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Build failed! Check the Gradle logs." -ForegroundColor Red
    exit
}

$exePath = "build\compose\binaries\main-release\exe\B2CPC-${version}.exe"
if (-Not (Test-Path $exePath)) {
    Write-Host "ERROR: EXE file not found at: $exePath" -ForegroundColor Red
    exit
}
Write-Host "SUCCESS: Build complete: $exePath" -ForegroundColor Green

# 2. Create GitHub Release
Write-Host "`n[2/3] Creating GitHub Release v$version..." -ForegroundColor Cyan
$headers = @{
    "Authorization" = "token $token"
    "Accept" = "application/vnd.github.v3+json"
}

$body = @{
    "tag_name" = "v$version"
    "name" = "B2CPC v$version"
    "body" = "Automated release v$version for B2CPC."
    "draft" = $false
    "prerelease" = $false
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases" -Method Post -Headers $headers -Body $body
    $uploadUrl = [string]$response.upload_url
    $uploadUrl = $uploadUrl -replace "\{.*\}", ""
    Write-Host "SUCCESS: Release created! (ID: $($response.id))" -ForegroundColor Green
} catch {
    Write-Host "WARNING: Release tag might already exist. Attempting to update existing release..." -ForegroundColor Yellow
    
    try {
        $existing = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/tags/v$version" -Method Get -Headers $headers
        $uploadUrl = [string]$existing.upload_url
        $uploadUrl = $uploadUrl -replace "\{.*\}", ""
        
        # Delete existing assets if they have the same name
        foreach ($asset in $existing.assets) {
            if ($asset.name -eq "B2CPC-${version}.exe") {
                Write-Host " - Deleting existing B2CPC-${version}.exe from release..."
                Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/assets/$($asset.id)" -Method Delete -Headers $headers
            }
        }
    } catch {
        Write-Host "ERROR: Failed to retrieve or create release: $_" -ForegroundColor Red
        exit
    }
}

# 3. Upload EXE Asset
Write-Host "`n[3/3] Uploading EXE file... (This may take some time depending on file size)" -ForegroundColor Cyan

$uploadHeaders = @{
    "Authorization" = "token $token"
    "Accept" = "application/vnd.github.v3+json"
    "Content-Type" = "application/octet-stream"
}

try {
    $cleanUrl = $uploadUrl.Trim() + "?name=B2CPC-${version}.exe"
    Invoke-RestMethod -Uri $cleanUrl -Method Post -Headers $uploadHeaders -InFile $exePath -TimeoutSec 1200 | Out-Null
    Write-Host "SUCCESS: File uploaded successfully!" -ForegroundColor Green
} catch {
    Write-Host "ERROR: File upload failed: $_" -ForegroundColor Red
    exit
}

Write-Host ""
Write-Host "All deployment steps completed! The PC app will now auto-update." -ForegroundColor Green
Read-Host "Press Enter to exit" 
