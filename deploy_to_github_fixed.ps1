param (
    [string]$version = ""
)

# ============================================================
#  B2CPC 濡쒖뺄 鍮뚮뱶 & GitHub Release ?먮룞 諛고룷 ?ㅽ겕由쏀듃 (媛뺥솕??
# ============================================================

$token = "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"
$owner = "AutoIlon"
$repo = "B2CPC"

# ?? ?됱긽 ?ы띁 ?⑥닔 ??
function Write-Step($step, $msg) { Write-Host "`n[$step] $msg" -ForegroundColor Cyan }
function Write-Ok($msg)          { Write-Host "  OK: $msg" -ForegroundColor Green }
function Write-Err($msg)         { Write-Host "  ERROR: $msg" -ForegroundColor Red }
function Write-Warn($msg)        { Write-Host "  WARNING: $msg" -ForegroundColor Yellow }
function Write-Info($msg)        { Write-Host "  $msg" -ForegroundColor Gray }

# ?? ?ㅻ뜑 異쒕젰 ??
Write-Host ""
Write-Host "  =============================================" -ForegroundColor Cyan
Write-Host "    B2CPC Local Build & Deploy (Enhanced)" -ForegroundColor White
Write-Host "  =============================================" -ForegroundColor Cyan
Write-Host ""

# ?? 0?④퀎: 理쒖떊 踰꾩쟾 ?먮룞 媛먯? ??
Write-Step "0/6" "理쒖떊 踰꾩쟾 ?뺤씤 以?.."
try {
    $headers = @{
        "Authorization" = "token $token"
        "Accept" = "application/vnd.github.v3+json"
    }
    $releases = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases?per_page=5" -Method Get -Headers $headers -ErrorAction SilentlyContinue
    if ($releases -and $releases.Count -gt 0) {
        $latestTag = $releases[0].tag_name
        Write-Ok "?꾩옱 理쒖떊 由대━利? $latestTag"
        
        # ?먮룞 ?ㅼ쓬 踰꾩쟾 ?쒖븞
        $cleanVersion = $latestTag -replace '^v', ''
        $parts = $cleanVersion.Split('.')
        if ($parts.Count -ge 3) {
            $parts[2] = [string]([int]$parts[2] + 1)
            $suggestedVersion = $parts -join '.'
            Write-Info "?ㅼ쓬 異붿쿇 踰꾩쟾: $suggestedVersion"
        }
    } else {
        Write-Info "湲곗〈 由대━利??놁쓬. 泥?諛고룷?낅땲??"
        $suggestedVersion = "1.0.0"
    }
} catch {
    Write-Warn "由대━利??뺣낫瑜?媛?몄삱 ???놁뒿?덈떎. ?ㅽ듃?뚰겕瑜??뺤씤?섏꽭??"
    $suggestedVersion = "1.0.0"
}

# ?? 1?④퀎: 踰꾩쟾 ?낅젰 ??
Write-Host ""
if ($version -eq "") {
    $defaultMsg = if ($suggestedVersion) { " (Enter??異붿쿇: $suggestedVersion)" } else { "" }
    $version = Read-Host "  諛고룷??踰꾩쟾???낅젰?섏꽭??defaultMsg"
    if ($version -eq "" -and $suggestedVersion) {
        $version = $suggestedVersion
    }
}

if ($version -eq "") {
    Write-Err "踰꾩쟾???낅젰?섏? ?딆븯?듬땲??"
    Read-Host "Enter瑜??뚮윭 醫낅즺"
    exit 1
}

# 踰꾩쟾 ?뺤떇 寃利?if ($version -notmatch '^\d+\.\d+\.\d+$') {
    Write-Err "踰꾩쟾 ?뺤떇???щ컮瑜댁? ?딆뒿?덈떎! (?? 1.0.11)"
    Read-Host "Enter瑜??뚮윭 醫낅즺"
    exit 1
}

Write-Host ""
Write-Host "  ==============================================" -ForegroundColor Yellow
Write-Host "    諛고룷 踰꾩쟾: v$version" -ForegroundColor White
Write-Host "  ==============================================" -ForegroundColor Yellow
Write-Host ""

$confirm = Read-Host "  ??踰꾩쟾?쇰줈 諛고룷瑜?吏꾪뻾?좉퉴?? (Y/N)"
if ($confirm -ne "Y" -and $confirm -ne "y") {
    Write-Host "`n  諛고룷媛 痍⑥냼?섏뿀?듬땲??" -ForegroundColor Gray
    Read-Host "Enter瑜??뚮윭 醫낅즺"
    exit 0
}

# ?? 2?④퀎: ?댁쟾 鍮뚮뱶 ?뺣━ ??
Write-Step "1/6" "?댁쟾 鍮뚮뱶 ?뚯씪 ?뺣━ 以?.."
$buildDir = "build\compose\binaries"
if (Test-Path $buildDir) {
    Remove-Item -Path $buildDir -Recurse -Force -ErrorAction SilentlyContinue
    Write-Ok "?댁쟾 鍮뚮뱶 ?뚯씪 ??젣 ?꾨즺"
} else {
    Write-Info "?뺣━??鍮뚮뱶 ?뚯씪 ?놁쓬"
}

# ?? 3?④퀎: EXE 鍮뚮뱶 ??
Write-Step "2/6" "Windows EXE 鍮뚮뱶 ?쒖옉 (v$version)..."
Write-Info "???④퀎??紐?遺??뺣룄 ?뚯슂?????덉뒿?덈떎..."
$buildStart = Get-Date

.\gradlew.bat packageReleaseExe "-PappVersion=$version"

$buildEnd = Get-Date
$buildTime = ($buildEnd - $buildStart).TotalSeconds

if ($LASTEXITCODE -ne 0) {
    Write-Err "鍮뚮뱶 ?ㅽ뙣! Gradle 濡쒓렇瑜??뺤씤?섏꽭??"
    Read-Host "Enter瑜??뚮윭 醫낅즺"
    exit 1
}

# EXE ?뚯씪 ?먮룞 ?먯깋
$exeFiles = Get-ChildItem -Path "build\compose\binaries" -Filter "*.exe" -Recurse -ErrorAction SilentlyContinue
if ($exeFiles -eq $null -or $exeFiles.Count -eq 0) {
    Write-Err "鍮뚮뱶??EXE ?뚯씪??李얠쓣 ???놁뒿?덈떎!"
    Read-Host "Enter瑜??뚮윭 醫낅즺"
    exit 1
}

$exePath = $exeFiles[0].FullName
$exeSize = [math]::Round($exeFiles[0].Length / 1MB, 1)
Write-Ok "鍮뚮뱶 ?꾨즺! (?뚯슂?쒓컙: $([math]::Round($buildTime))珥?"
Write-Ok "?뚯씪: $exePath"
Write-Ok "?⑸웾: ${exeSize}MB"

# ?? 4?④퀎: Git 而ㅻ컠 & ?몄떆 ??
Write-Step "3/6" "Git 蹂寃쎌궗???뺤씤 以?.."
$gitStatus = git status --short
if ($null -eq $gitStatus -or $gitStatus -eq "") {
    Write-Info "而ㅻ컠??蹂寃쎌궗??씠 ?놁뒿?덈떎."
} else {
    Write-Host "`n[ Git ?꾩옱 ?곹깭 ]" -ForegroundColor Yellow
    Write-Info "$gitStatus"
    $gitConfirm = Read-Host "`n  ??蹂寃쎌궗??쓣 而ㅻ컠?섍퀬 ?몄떆?좉퉴?? (Y/N)"
    if ($gitConfirm -eq "Y" -or $gitConfirm -eq "y") {
        git add .
        git commit -m "release: v$version"
        git push origin main
        Write-Ok "Git ?숆린???꾨즺"
    } else {
        Write-Warn "Git ?숆린?붾? 嫄대꼫?곷땲??"
    }
}

# ?? 5?④퀎: Git ?쒓렇 ?앹꽦 ??
Write-Step "4/6" "Git ?쒓렇 ?뺤씤 以?.."
$existingTag = git tag -l "v$version"
if ($null -ne $existingTag -and $existingTag -ne "") {
    Write-Warn "?쒓렇 v$version ??媛) ?대? 議댁옱?⑸땲??"
    $tagConfirm = Read-Host "  湲곗〈 ?쒓렇瑜???젣?섍퀬 ?ъ깮?깊븷源뚯슂? (Y/N)"
    if ($tagConfirm -eq "Y" -or $tagConfirm -eq "y") {
        git tag -d "v$version"
        git push --delete origin "v$version"
        git tag "v$version"
        git push origin "v$version"
        Write-Ok "?쒓렇 v$version ?ъ깮??諛??몄떆 ?꾨즺"
    } else {
        Write-Warn "?쒓렇 ?묒뾽??嫄대꼫?곷땲??"
    }
} else {
    $tagConfirm = Read-Host "  ?쒓렇 v$version ??瑜? ?앹꽦?섍퀬 ?몄떆?좉퉴?? (Y/N)"
    if ($tagConfirm -eq "Y" -or $tagConfirm -eq "y") {
        git tag "v$version"
        git push origin "v$version"
        Write-Ok "?쒓렇 v$version ?앹꽦 諛??몄떆 ?꾨즺"
    } else {
        Write-Warn "?쒓렇 ?묒뾽??嫄대꼫?곷땲??"
    }
}

# ?? 6?④퀎: GitHub Release ?앹꽦 ??
Write-Step "5/6" "GitHub Release v$version ?앹꽦 以?.."
$releaseHeaders = @{
    "Authorization" = "token $token"
    "Accept" = "application/vnd.github.v3+json"
}

$releaseBody = @{
    "tag_name" = "v$version"
    "name" = "B2CPC v$version"
    "body" = "B2CPC v$version`n`n- Build time: $([math]::Round($buildTime))s`n- File size: ${exeSize}MB`n- Built on: $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
    "draft" = $false
    "prerelease" = $false
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases" -Method Post -Headers $releaseHeaders -Body $releaseBody
    $uploadUrl = [string]$response.upload_url
    $uploadUrl = $uploadUrl -replace "\{.*\}", ""
    Write-Ok "由대━利??앹꽦 ?꾨즺! (ID: $($response.id))"
} catch {
    Write-Warn "由대━利덇? ?대? 議댁옱?⑸땲?? 湲곗〈 由대━利덉뿉 ?낅줈?쒗빀?덈떎..."
    try {
        $existing = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/tags/v$version" -Method Get -Headers $releaseHeaders
        $uploadUrl = [string]$existing.upload_url
        $uploadUrl = $uploadUrl -replace "\{.*\}", ""

        # 湲곗〈 ?숈씪???대쫫??asset ??젣
        foreach ($asset in $existing.assets) {
            if ($asset.name -like "*.exe") {
                Write-Info "湲곗〈 EXE ?뚯씪 ??젣: $($asset.name)"
                Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/assets/$($asset.id)" -Method Delete -Headers $releaseHeaders
            }
        }
        Write-Ok "湲곗〈 由대━利??ъ슜 以鍮??꾨즺"
    } catch {
        Write-Err "由대━利덈? ?앹꽦?섍굅??李얠쓣 ???놁뒿?덈떎: $_"
        Read-Host "Enter瑜??뚮윭 醫낅즺"
        exit 1
    }
}

# ?? 7?④퀎: EXE ?낅줈????
Write-Step "6/6" "EXE ?뚯씪 ?낅줈??以?.. (${exeSize}MB - ?쒓컙??嫄몃┫ ???덉뒿?덈떎)"

$uploadHeaders = @{
    "Authorization" = "token $token"
    "Accept" = "application/vnd.github.v3+json"
    "Content-Type" = "application/octet-stream"
}

$exeFileName = [System.IO.Path]::GetFileName($exePath)
try {
    $cleanUrl = $uploadUrl.Trim() + "?name=$exeFileName"
    $uploadStart = Get-Date
    Invoke-RestMethod -Uri $cleanUrl -Method Post -Headers $uploadHeaders -InFile $exePath -TimeoutSec 1800 | Out-Null
    $uploadTime = ((Get-Date) - $uploadStart).TotalSeconds
    Write-Ok "?낅줈???꾨즺! (?뚯슂?쒓컙: $([math]::Round($uploadTime))珥?"
} catch {
    Write-Err "?낅줈???ㅽ뙣: $_"
    Read-Host "Enter瑜??뚮윭 醫낅즺"
    exit 1
}

# ?? ?꾨즺 ??
Write-Host ""
Write-Host "  ==============================================" -ForegroundColor Green
Write-Host "    B2CPC v$version 諛고룷 ?꾨즺!" -ForegroundColor White
Write-Host "  ==============================================" -ForegroundColor Green
Write-Host ""
Write-Host "  鍮뚮뱶 ?쒓컙: $([math]::Round($buildTime))珥? -ForegroundColor Gray
Write-Host "  ?뚯씪 ?ш린: ${exeSize}MB" -ForegroundColor Gray
Write-Host "  ?낅줈???쒓컙: $([math]::Round($uploadTime))珥? -ForegroundColor Gray
Write-Host ""
Write-Host "  由대━利??뺤씤: https://github.com/$owner/$repo/releases/tag/v$version" -ForegroundColor Cyan
Write-Host "  湲곗〈 ?ъ슜???깆씠 ?먮룞?쇰줈 ?낅뜲?댄듃瑜?媛먯??⑸땲??" -ForegroundColor Gray
Write-Host ""
Read-Host "  Enter瑜??뚮윭 醫낅즺"
