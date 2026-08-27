#!/bin/bash
echo "========================================================================="
echo "              VEHICLE RENTAL MANAGEMENT SYSTEM (KTU S3 OOP)              "
echo "========================================================================="

mkdir -p bin

echo "[1/3] Downloading dependencies if required..."
pwsh ./download_deps.ps1 2>/dev/null || true

echo "[2/3] Compiling Java source files..."
javac -encoding UTF-8 -cp "lib/*" -d bin src/*.java src/model/*.java src/dao/*.java src/servlet/*.java src/util/*.java

if [ $? -ne 0 ]; then
    echo "[ERROR] Compilation failed!"
    exit 1
fi

echo "[3/3] Launching Java Web Server at http://localhost:8080/..."
java -cp "bin:lib/*" ServerRunner
