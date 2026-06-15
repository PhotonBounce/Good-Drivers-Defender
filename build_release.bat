@echo off
rem Set Java home to the detected JDK
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem Verify Java is available
echo Java version:
java -version

rem Run the Gradle build for a release AAB
call gradlew.bat bundleRelease

if %ERRORLEVEL% NEQ 0 (
  echo Build failed with exit code %ERRORLEVEL%
  exit /b %ERRORLEVEL%
) else (
  echo Build succeeded.
)
