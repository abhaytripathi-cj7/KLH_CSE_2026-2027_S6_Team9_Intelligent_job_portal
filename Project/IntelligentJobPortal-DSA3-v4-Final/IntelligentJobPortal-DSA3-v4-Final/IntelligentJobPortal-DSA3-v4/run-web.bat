@echo off
setlocal
cd /d "%~dp0"

where javac >nul 2>&1
if errorlevel 1 (
  echo ERROR: javac was not found. Install JDK 17 or later and restart VS Code.
  pause
  exit /b 1
)
where java >nul 2>&1
if errorlevel 1 (
  echo ERROR: java was not found. Install JDK 17 or later and restart VS Code.
  pause
  exit /b 1
)

for /f "delims=" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do (
  echo Using %%V
  goto :version_done
)
:version_done

if not exist out mkdir out

dir /s /b "src\*.java" > "%TEMP%\novahire_sources.txt"
echo Compiling Java + DSA engine + local web server...
javac --add-modules jdk.httpserver -encoding UTF-8 -d out @"%TEMP%\novahire_sources.txt"
if errorlevel 1 goto :fail

echo.
echo Running full project self-test...
java --add-modules jdk.httpserver -cp out com.dsa.jobportal.SelfTest
if errorlevel 1 goto :fail

echo.
echo Starting NovaHire Web Edition...
if "%~1"=="" (
  java --add-modules jdk.httpserver -cp out com.dsa.jobportal.web.WebMain
) else (
  java --add-modules jdk.httpserver -cp out com.dsa.jobportal.web.WebMain %~1
)
exit /b %ERRORLEVEL%

:fail
echo.
echo Startup failed. Review the error above.
pause
exit /b 1
