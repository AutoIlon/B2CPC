@echo off
chcp 65001 >nul
echo ==========================================
echo       B2CPC 자동 배포 스크립트 실행
echo ==========================================
echo.
set /p APP_VERSION="배포할 새 버전을 입력하세요 (예: 1.0.1) [엔터시 기본값 1.0.0]: "
if "%APP_VERSION%"=="" (
    set APP_VERSION=1.0.0
)

echo.
echo 지정된 배포 버전: %APP_VERSION%
echo ------------------------------------------
echo 빌드 및 GitHub Releases 업로드를 시작합니다...
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy_to_github.ps1" -version "%APP_VERSION%"

echo.
pause
