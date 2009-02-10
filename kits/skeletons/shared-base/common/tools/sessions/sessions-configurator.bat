@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

setlocal
set TC_INSTALL_DIR=%~d0%~p0..\..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

if not defined JAVA_HOME set JAVA_HOME="%TC_INSTALL_DIR%\jre"
set JAVA_HOME="%JAVA_HOME:"=%"

set SVT_JAR=
set CLASSPATH=%TC_INSTALL_DIR%\lib\tc.jar

for %%i in (%TC_INSTALL_DIR%\lib\svt*.jar) do set SVT_JAR=%%i
if exist "%SVT_JAR%"  set CLASSPATH=%CLASSPATH%;%SVT_JAR%

set CLASSPATH="%CLASSPATH:"=%"

set JAVA_OPTS=-Dtc.install-root=%TC_INSTALL_DIR% %JAVA_OPTS%
start "session configurator" /B %JAVA_HOME%\bin\java %JAVA_OPTS% -cp %CLASSPATH% org.terracotta.ui.session.SessionIntegrator %*
endlocal
