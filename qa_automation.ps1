# PowerShell QA Automation Script for DriverSHIELD
# This script downloads a portable OpenJDK, sets JAVA_HOME, builds the Android app, installs it on a connected device, launches it, captures a screenshot and a short video, and pulls the artifacts.

# ----- Step 1: Ensure Java (JDK) -----
# Try to locate an existing Java installation first
$javaPath = $null
try {
    $javaExe = (where.exe java 2>$null | Select-Object -First 1)
    if ($javaExe) {
        $javaPath = Split-Path $javaExe -Parent
        $env:JAVA_HOME = Split-Path $javaPath -Parent
        Write-Host "Found existing Java at $env:JAVA_HOME"
    }
} catch {}
# If no suitable Java is found, download portable OpenJDK 17 (Windows x64 zip)
if (-not $env:JAVA_HOME) {
    Write-Host "Downloading portable OpenJDK 17 (Windows x64 zip)..."
    $jdkUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.19%2B10/OpenJDK17U-jdk_x64_windows_hotspot_17.0.19_10.zip"
    $zipPath = "$PSScriptRoot\openjdk17.zip"
    Invoke-WebRequest -Uri $jdkUrl -OutFile $zipPath -UseBasicParsing
    Write-Host "Extracting OpenJDK..."
    Expand-Archive -Path $zipPath -DestinationPath "$PSScriptRoot\jdk17" -Force
    $extractedDir = Get-ChildItem "$PSScriptRoot\jdk17" -Directory | Select-Object -First 1
    $env:JAVA_HOME = $extractedDir.FullName
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    Write-Host "JAVA_HOME set to $env:JAVA_HOME"
}
# ----- Step 2: Build the Android app -----
Write-Host "Running Gradle build..."
& "$PSScriptRoot\gradlew.bat" assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed. Exiting script."
    exit $LASTEXITCODE
}

# Locate the generated APK
$apk = Get-ChildItem -Path "$PSScriptRoot\app\build\outputs\apk\debug" -Filter *.apk | Select-Object -First 1
if (-not $apk) {
    Write-Error "APK not found after build. Exiting."
    exit 1
}
Write-Host "APK found at $($apk.FullName)"

# ----- Step 3: Install on connected device -----
Write-Host "Checking for connected Android device..."
adb devices
if ((adb get-state) -ne "device") {
    Write-Error "No device detected via adb. Please connect a device and ensure USB debugging is enabled."
    exit 1
}
Write-Host "Installing APK..."
adb install -r $apk.FullName

# ----- Step 4: Launch the app -----
# Replace the package name and main activity if different
$package = "com.example"  # TODO: adjust to actual package name if different
$activity = "com.example.MainActivity"  # TODO: adjust if needed
Write-Host "Launching app..."
adb shell am start -n "$package/.MainActivity"
Start-Sleep -Seconds 5

# ----- Step 5: Capture splash screen screenshot -----
$screenshotPath = "$PSScriptRoot\qa_screenshot.png"
adb exec-out screencap -p > $screenshotPath
Write-Host "Screenshot saved to $screenshotPath"

# ----- Step 5b: Capture each main UI screen -----
# Get device screen dimensions
$sizeInfo = adb shell wm size
if ($sizeInfo -match "Physical size: (\d+)x(\d+)") {
    $width = [int]$matches[1]
    $height = [int]$matches[2]
    $tabY = [int]($height * 0.9)
    $tabWidth = [int]($width / 4)
    $tabs = @(
        @{name="dashboard"; x = [int]($tabWidth/2)},
        @{name="locker"; x = [int]($tabWidth * 1.5)},
        @{name="report_builder"; x = [int]($tabWidth * 2.5)},
        @{name="upgrade"; x = [int]($tabWidth * 3.5)}
    )
    foreach ($t in $tabs) {
        Write-Host "Navigating to $($t.name) tab"
        adb shell input tap $($t.x) $tabY
        Start-Sleep -Seconds 3
        $path = "$PSScriptRoot\qa_$($t.name).png"
        adb exec-out screencap -p > $path
        Write-Host "Saved $($t.name) screenshot to $path"
    }
}

# ----- Step 6: Record short video (15 seconds) -----
$remoteVideo = "/sdcard/qa_video.mp4"
Write-Host "Recording a 15‑second video..."
# Record a 15‑second video (blocking)
adb shell screenrecord --time-limit 15 $remoteVideo
# Wait a short moment for the file to be finalized
Start-Sleep -Seconds 5
# Pull the video
$localVideo = "$PSScriptRoot\qa_video.mp4"
adb pull $remoteVideo $localVideo
Write-Host "Video saved to $localVideo"

# ----- Step 7: Pull any app‑generated evidence (optional) -----
$evidenceRemote = "/sdcard/DriverSHIELD_Evidence"
$evidenceLocal = "$PSScriptRoot\Evidence"
if ((adb shell "[ -d $evidenceRemote ] && echo exists") -eq "exists") {
    Write-Host "Pulling evidence folder..."
    adb pull $evidenceRemote $evidenceLocal
    Write-Host "Evidence saved to $evidenceLocal"
} else {
    Write-Host "No evidence folder found on device."
}

Write-Host "QA automation completed successfully."
