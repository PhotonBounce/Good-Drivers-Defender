rem =============================
rem build_and_publish.bat
rem =============================
@echo off

rem --- Set JDK path (matching gradle.properties) ---
set "JAVA_HOME=D:\good-drivers-defender\jdk17\jdk-17.0.19+10"

rem --- Add JDK bin to PATH ---
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem --- Verify Java is available ---
java -version
if errorlevel 1 (
    echo ERROR: Java not found in PATH. Exiting.& exit /b 1
)

rem --- Change to project directory ---
cd /d "D:\good-drivers-defender"

rem --- Clean and build the release bundle (AAB) ---
call gradlew.bat clean bundleRelease
if errorlevel 1 (
    echo ERROR: Gradle build failed. Exiting.& exit /b 1
)

rem --- Publish the bundle to Google Play using the Play Publisher plugin ---
rem call gradlew.bat publishBundle
if errorlevel 1 (
    echo ERROR: Publishing failed. Check the service‑account JSON and Play Console permissions.& exit /b 1
)

echo.
echo Build and publish completed successfully.
pause
