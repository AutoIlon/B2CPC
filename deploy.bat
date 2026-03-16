@echo off
chcp 65001 >nul
echo ==========================================
echo       B2CPC Auto Deploy Script
echo   (GitHub Actions Auto Build)
echo ==========================================
echo.
set /p APP_VERSION=Version (ex: 1.0.11): 
if "%APP_VERSION%"=="" (
    echo Please enter version!
    pause
    exit /b
)
set /p COMMIT_MSG=Commit message (Enter for default): 
if "%COMMIT_MSG%"=="" (
    set "COMMIT_MSG=release: v%APP_VERSION%"
)
echo.
echo  Version: v%APP_VERSION%
echo  Message: %COMMIT_MSG%
echo.
set /p CONFIRM=Deploy? (Y/N): 
if /i not "%CONFIRM%"=="Y" (
    echo Cancelled.
    pause
    exit /b
)
echo.
echo [1/4] git add...
git add .
echo [2/4] git commit...
git commit -m "%COMMIT_MSG%"
echo [3/4] git push...
git push origin main
if errorlevel 1 (
    echo Push failed!
    pause
    exit /b
)
echo [4/4] tag v%APP_VERSION%...
git tag v%APP_VERSION%
if errorlevel 1 (
    echo Tag failed! Version may already exist.
    pause
    exit /b
)
git push origin v%APP_VERSION%
if errorlevel 1 (
    echo Tag push failed!
    pause
    exit /b
)
echo.
echo ==========================================
echo  Deploy complete!
echo  GitHub Actions will build EXE automatically.
echo  Check: https://github.com/AutoIlon/B2CPC/actions
echo ==========================================
echo.
pause