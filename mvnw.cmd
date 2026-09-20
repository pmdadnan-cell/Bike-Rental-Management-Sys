@echo off
setlocal EnableDelayedExpansion

if "%JAVA_HOME%"=="" (
  for /d %%i in ("C:\Program Files\Java\jdk-*") do set "JAVA_HOME=%%i"
)

if "%JAVA_HOME%"=="" (
  echo JAVA_HOME is not set and no JDK was found under "C:\Program Files\Java".
  exit /b 1
)

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "PORTABLE_MVN=%USERPROFILE%\.m2\wrapper\apache-maven-3.9.9\bin\mvn.cmd"

if exist "%PORTABLE_MVN%" (
  "%PORTABLE_MVN%" %*
  exit /b %ERRORLEVEL%
)

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
if not exist "%WRAPPER_JAR%" (
  echo Maven wrapper JAR is missing: %WRAPPER_JAR%
  exit /b 1
)

"%JAVA_HOME%\bin\java.exe" --class-path "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
