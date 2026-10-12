@echo off
rem Goi build.bat roi truyen cac tham so cho chuong trinh Java.
setlocal
cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b 1
java -cp build Net %*
exit /b %errorlevel%
