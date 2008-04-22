@echo off

rem
rem  All content copyright (c) 2003-2008 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

setlocal
set TC_INSTALL_DIR=%~d0%~p0.
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

if not defined JAVA_HOME set JAVA_HOME="%TC_INSTALL_DIR%\jre"
set JAVA_HOME="%JAVA_HOME:"=%"

set SVT_JAR=
set CLASSPATH=%TC_INSTALL_DIR%\lib\tc.jar

for %%i in (%TC_INSTALL_DIR%\lib\svt*.jar) do set SVT_JAR=%%i
if exist "%SVT_JAR%" set CLASSPATH=%CLASSPATH%;%SVT_JAR%

set OPTS=-Djava.awt.Window.locationByPlatform=true
set OPTS=%OPTS% -Dtc.install-root=%TC_INSTALL_DIR%
set JAVA_OPTS=%OPTS% %JAVA_OPTS%
start "Terracotta Welcome" /B %JAVA_HOME%\bin\java %JAVA_OPTS% -cp %CLASSPATH% com.tc.welcome.WelcomeFrame %*
endlocal
