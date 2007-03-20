@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

setlocal
set TC_INSTALL_DIR=%~d0%~p0..\..
if not exist "%JAVA_HOME%" set JAVA_HOME=%TC_INSTALL_DIR%\jre
set CLASSPATH=%TC_INSTALL_DIR%\lib\tc.jar
set OPTS=-Djava.awt.Window.locationByPlatform=true
set OPTS=%OPTS% -Dtc.install-root="%TC_INSTALL_DIR%"
set JAVA_OPTS=%OPTS% %JAVA_OPTS%
start "Pojo Samples" /B "%JAVA_HOME%\bin\java" %JAVA_OPTS% -cp "%CLASSPATH%" com.tc.welcome.DSOSamplesFrame %*
endlocal
