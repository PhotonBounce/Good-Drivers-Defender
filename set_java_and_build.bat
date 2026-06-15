@echo off
rem Set JAVA_HOME to bundled JDK inside the project
set "JAVA_HOME=%~dp0jdk17\jdk-17.0.19+10"
rem Prepend JDK bin to PATH
set "PATH=%JAVA_HOME%\bin;%PATH%"
rem Execute Gradle wrapper to build the app
call gradlew.bat clean assembleDebug
