@echo off
set "JAVA_HOME=D:\good-drivers-defender\jdk17\jdk-17.0.19+10"
set "PATH=%JAVA_HOME%\bin;%PATH%"
gradlew.bat assembleDebug
