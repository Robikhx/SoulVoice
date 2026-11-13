# SoulVoice Rebuild Script for Windows PowerShell
# Cleans, builds, and packages the application with jpackage

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  SoulVoice Rebuild Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Clean old SerialMonitor folder
Write-Host "[1/6] Cleaning old build..." -ForegroundColor Yellow
if (Test-Path "SerialMonitor") {
    Remove-Item -Recurse -Force "SerialMonitor"
    Write-Host "  Removed SerialMonitor folder" -ForegroundColor Green
} else {
    Write-Host "  No previous build found" -ForegroundColor Green
}
Write-Host ""

# Step 2: Maven clean and package
Write-Host "[2/6] Running Maven clean package..." -ForegroundColor Yellow
mvn clean package
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Maven build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "  Maven build successful" -ForegroundColor Green
Write-Host ""

# Step 3: Create target/libs directory
Write-Host "[3/6] Creating libs directory..." -ForegroundColor Yellow
if (-not (Test-Path "target/libs")) {
    New-Item -ItemType Directory -Path "target/libs" | Out-Null
}
Write-Host "  target/libs created" -ForegroundColor Green
Write-Host ""

# Step 4: Copy dependencies
Write-Host "[4/6] Copying dependencies..." -ForegroundColor Yellow
mvn dependency:copy-dependencies -DoutputDirectory=target/libs
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Failed to copy dependencies!" -ForegroundColor Red
    exit 1
}
Write-Host "  Dependencies copied" -ForegroundColor Green
Write-Host ""

# Step 5: Copy main JAR to libs
Write-Host "[5/6] Copying main JAR..." -ForegroundColor Yellow
Copy-Item "target/transmitterapp-1.0-SNAPSHOT.jar" "target/libs/" -Force
Write-Host "  Main JAR copied to target/libs" -ForegroundColor Green
Write-Host ""

# Step 6: Package with jpackage
Write-Host "[6/6] Creating application package with jpackage..." -ForegroundColor Yellow
jpackage --input target/libs --name SerialMonitor --main-jar transmitterapp-1.0-SNAPSHOT.jar --main-class com.transmitter.Launcher --type app-image

if ($LASTEXITCODE -ne 0) {
    Write-Host "  jpackage failed!" -ForegroundColor Red
    exit 1
}
Write-Host "  Application packaged successfully" -ForegroundColor Green
Write-Host ""

# Success message
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Build Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Executable location: SerialMonitor\SerialMonitor.exe" -ForegroundColor Yellow
Write-Host ""
Write-Host "You can now run the application by executing:" -ForegroundColor Cyan
Write-Host "  .\SerialMonitor\SerialMonitor.exe" -ForegroundColor White
Write-Host ""
