@echo off
rem Dat cac file .class vao thu muc build.
setlocal
cd /d "%~dp0"
if not exist build mkdir build
javac -encoding UTF-8 -d build Net.java
if errorlevel 1 exit /b 1
echo Build thanh cong: build\Net.class
