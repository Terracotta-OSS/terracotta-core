@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

setlocal
set TC_INSTALL_DIR=%~d0%~p0..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

set JAVA_HOME="%JAVA_HOME:"=%"

set CLASSPATH=%TC_INSTALL_DIR%\lib\tc.jar
set CLASSPATH="%CLASSPATH:"=%"

set JAVA_OPTS=-Xmx256m -Dsun.java2d.pmoffscreen=false -Dtc.install-root=%TC_INSTALL_DIR% %JAVA_OPTS%
start "DevConsole" /B %JAVA_HOME%\bin\java %JAVA_OPTS% -cp %CLASSPATH% com.tc.admin.EnterpriseAdminClient %*
endlocal
