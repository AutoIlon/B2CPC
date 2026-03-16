param (
    [string]$version = ""
)

# ============================================================
#  B2CPC 로컬 빌드 & GitHub Release 자동 배포 스크립트 (강화판)
# ============================================================

$token = "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"
$owner = "AutoIlon"
$repo = "B2CPC"

# ── 색상 헬퍼 함수 ──
function Write-Step($step, $msg) { Write-Host "`n[$step] $msg" -ForegroundColor Cyan }
function Write-Ok($msg)          { Write-Host "  OK: $msg" -ForegroundColor Green }
function Write-Err($msg)         { Write-Host "  ERROR: $msg" -ForegroundColor Red }
function Write-Warn($msg)        { Write-Host "  WARNING: $msg" -ForegroundColor Yellow }
function Write-Info($msg)        { Write-Host "  $msg" -ForegroundColor Gray }

# ── 헤더 출력 ──
Write-Host ""
Write-Host "  =============================================" -ForegroundColor Cyan
Write-Host "    B2CPC Local Build & Deploy (Enhanced)" -ForegroundColor White
Write-Host "  =============================================" -ForegroundColor Cyan
Write-Host ""

# ── 0단계: 최신 버전 자동 감지 ──
Write-Step "0/6" "최신 버전 확인 중..."
try {
    $headers = @{
        "Authorization" = "token $token"
        "Accept" = "application/vnd.github.v3+json"
    }
    $releases = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases?per_page=5" -Method Get -Headers $headers -ErrorAction SilentlyContinue
    if ($releases -and $releases.Count -gt 0) {
        $latestTag = $releases[0].tag_name
        Write-Ok "현재 최신 릴리즈: $latestTag"
        
        # 자동 다음 버전 제안
        $cleanVersion = $latestTag -replace '^v', ''
        $parts = $cleanVersion.Split('.')
        if ($parts.Count -ge 3) {
            $parts[2] = [string]([int]$parts[2] + 1)
            $suggestedVersion = $parts -join '.'
            Write-Info "다음 추천 버전: $suggestedVersion"
        }
    } else {
        Write-Info "기존 릴리즈 없음. 첫 배포입니다."
        $suggestedVersion = "1.0.0"
    }
} catch {
    Write-Warn "릴리즈 정보를 가져올 수 없습니다. 네트워크를 확인하세요."
    $suggestedVersion = "1.0.0"
}

# ── 1단계: 버전 입력 ──
Write-Host ""
if ($version -eq "") {
    $defaultMsg = if ($suggestedVersion) { " (Enter시 추천: $suggestedVersion)" } else { "" }
    $version = Read-Host "  배포할 버전을 입력하세요$defaultMsg"
    if ($version -eq "" -and $suggestedVersion) {
        $version = $suggestedVersion
    }
}

if ($version -eq "") {
    Write-Err "버전을 입력하지 않았습니다!"
    Read-Host "Enter를 눌러 종료"
    exit 1
}

# 버전 형식 검증
if ($version -notmatch '^\d+\.\d+\.\d+$') {
    Write-Err "버전 형식이 올바르지 않습니다! (예: 1.0.11)"
    Read-Host "Enter를 눌러 종료"
    exit 1
}

Write-Host ""
Write-Host "  ==============================================" -ForegroundColor Yellow
Write-Host "    배포 버전: v$version" -ForegroundColor White
Write-Host "  ==============================================" -ForegroundColor Yellow
Write-Host ""

$confirm = Read-Host "  위 버전으로 배포를 진행할까요? (Y/N)"
if ($confirm -ne "Y" -and $confirm -ne "y") {
    Write-Host "`n  배포가 취소되었습니다." -ForegroundColor Gray
    Read-Host "Enter를 눌러 종료"
    exit 0
}

# ── 2단계: 이전 빌드 정리 ──
Write-Step "1/6" "이전 빌드 파일 정리 중..."
$buildDir = "build\compose\binaries"
if (Test-Path $buildDir) {
    Remove-Item -Path $buildDir -Recurse -Force -ErrorAction SilentlyContinue
    Write-Ok "이전 빌드 파일 삭제 완료"
} else {
    Write-Info "정리할 빌드 파일 없음"
}

# ── 3단계: EXE 빌드 ──
Write-Step "2/6" "Windows EXE 빌드 시작 (v$version)..."
Write-Info "이 단계는 몇 분 정도 소요될 수 있습니다..."
$buildStart = Get-Date

.\gradlew.bat packageReleaseExe "-PappVersion=$version"

$buildEnd = Get-Date
$buildTime = ($buildEnd - $buildStart).TotalSeconds

if ($LASTEXITCODE -ne 0) {
    Write-Err "빌드 실패! Gradle 로그를 확인하세요."
    Read-Host "Enter를 눌러 종료"
    exit 1
}

# EXE 파일 자동 탐색
$exeFiles = Get-ChildItem -Path "build\compose\binaries" -Filter "*.exe" -Recurse -ErrorAction SilentlyContinue
if ($exeFiles -eq $null -or $exeFiles.Count -eq 0) {
    Write-Err "빌드된 EXE 파일을 찾을 수 없습니다!"
    Read-Host "Enter를 눌러 종료"
    exit 1
}

$exePath = $exeFiles[0].FullName
$exeSize = [math]::Round($exeFiles[0].Length / 1MB, 1)
Write-Ok "빌드 완료! (소요시간: $([math]::Round($buildTime))초)"
Write-Ok "파일: $exePath"
Write-Ok "용량: ${exeSize}MB"

# ── 4단계: Git 커밋 & 푸시 ──
Write-Step "3/6" "Git 변경사항 확인 중..."
$gitStatus = git status --short
if ($null -eq $gitStatus -or $gitStatus -eq "") {
    Write-Info "커밋할 변경사항이 없습니다."
} else {
    Write-Host "`n[ Git 현재 상태 ]" -ForegroundColor Yellow
    Write-Info "$gitStatus"
    $gitConfirm = Read-Host "`n  위 변경사항을 커밋하고 푸시할까요? (Y/N)"
    if ($gitConfirm -eq "Y" -or $gitConfirm -eq "y") {
        git add .
        git commit -m "release: v$version"
        git push origin main
        Write-Ok "Git 동기화 완료"
    } else {
        Write-Warn "Git 동기화를 건너뜁니다."
    }
}

# ── 5단계: Git 태그 생성 ──
Write-Step "4/6" "Git 태그 확인 중..."
$existingTag = git tag -l "v$version"
if ($null -ne $existingTag -and $existingTag -ne "") {
    Write-Warn "태그 v$version 이(가) 이미 존재합니다."
    $tagConfirm = Read-Host "  기존 태그를 삭제하고 재생성할까요? (Y/N)"
    if ($tagConfirm -eq "Y" -or $tagConfirm -eq "y") {
        git tag -d "v$version"
        git push --delete origin "v$version"
        git tag "v$version"
        git push origin "v$version"
        Write-Ok "태그 v$version 재생성 및 푸시 완료"
    } else {
        Write-Warn "태그 작업을 건너뜁니다."
    }
} else {
    $tagConfirm = Read-Host "  태그 v$version 을(를) 생성하고 푸시할까요? (Y/N)"
    if ($tagConfirm -eq "Y" -or $tagConfirm -eq "y") {
        git tag "v$version"
        git push origin "v$version"
        Write-Ok "태그 v$version 생성 및 푸시 완료"
    } else {
        Write-Warn "태그 작업을 건너뜁니다."
    }
}

# ── 6단계: GitHub Release 생성 ──
Write-Step "5/6" "GitHub Release v$version 생성 중..."
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
    Write-Ok "릴리즈 생성 완료! (ID: $($response.id))"
} catch {
    Write-Warn "릴리즈가 이미 존재합니다. 기존 릴리즈에 업로드합니다..."
    try {
        $existing = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/tags/v$version" -Method Get -Headers $releaseHeaders
        $uploadUrl = [string]$existing.upload_url
        $uploadUrl = $uploadUrl -replace "\{.*\}", ""

        # 기존 동일한 이름의 asset 삭제
        foreach ($asset in $existing.assets) {
            if ($asset.name -like "*.exe") {
                Write-Info "기존 EXE 파일 삭제: $($asset.name)"
                Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/assets/$($asset.id)" -Method Delete -Headers $releaseHeaders
            }
        }
        Write-Ok "기존 릴리즈 사용 준비 완료"
    } catch {
        Write-Err "릴리즈를 생성하거나 찾을 수 없습니다: $_"
        Read-Host "Enter를 눌러 종료"
        exit 1
    }
}

# ── 7단계: EXE 업로드 ──
Write-Step "6/6" "EXE 파일 업로드 중... (${exeSize}MB - 시간이 걸릴 수 있습니다)"

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
    Write-Ok "업로드 완료! (소요시간: $([math]::Round($uploadTime))초)"
} catch {
    Write-Err "업로드 실패: $_"
    Read-Host "Enter를 눌러 종료"
    exit 1
}

# ── 완료 ──
Write-Host ""
Write-Host "  ==============================================" -ForegroundColor Green
Write-Host "    B2CPC v$version 배포 완료!" -ForegroundColor White
Write-Host "  ==============================================" -ForegroundColor Green
Write-Host ""
Write-Host "  빌드 시간: $([math]::Round($buildTime))초" -ForegroundColor Gray
Write-Host "  파일 크기: ${exeSize}MB" -ForegroundColor Gray
Write-Host "  업로드 시간: $([math]::Round($uploadTime))초" -ForegroundColor Gray
Write-Host ""
Write-Host "  릴리즈 확인: https://github.com/$owner/$repo/releases/tag/v$version" -ForegroundColor Cyan
Write-Host "  기존 사용자 앱이 자동으로 업데이트를 감지합니다." -ForegroundColor Gray
Write-Host ""
Read-Host "  Enter를 눌러 종료"
