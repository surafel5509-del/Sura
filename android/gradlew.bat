@echo off
:: Minimal Gradle wrapper shim for Windows: delegate to system gradle
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
) else (
  echo Gradle not found in PATH. Please install Gradle and try again.
  exit /b 1
)
