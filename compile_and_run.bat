@echo off
TITLE Vehicle Rental Management System - KTU S3 OOP Project
echo =========================================================================
echo               VEHICLE RENTAL MANAGEMENT SYSTEM (KTU S3 OOP)               
echo =========================================================================
echo.

if not exist bin (
    mkdir bin
)

echo [1/3] Downloading dependencies if required...
powershell -ExecutionPolicy Bypass -File .\download_deps.ps1

echo [2/3] Compiling Java source files...
javac --release 8 -encoding UTF-8 -cp "lib/*" -d bin src/*.java src/model/*.java src/dao/*.java src/servlet/*.java src/util/*.java
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed! Please inspect error messages above.
    pause
    exit /b %errorlevel%
)

echo [3/3] Launching Java Web Server...
echo Site will open at: http://localhost:8080/
echo Press Ctrl+C in this terminal to stop the server.
echo -------------------------------------------------------------------------

java -cp "bin;lib/*" ServerRunner

pause
