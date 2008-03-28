@echo off

rem
rem  All content copyright (c) 2003-2008 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

rem --------------------------------------------------------------------
rem - stop.bat 908{1,2}
rem --------------------------------------------------------------------

setlocal
cd %~d0%~p0..
set SANDBOX=%CD%
set SANDBOX="%SANDBOX:"=%"

set TC_INSTALL_DIR=%SANDBOX%\..\..\..

if not defined JAVA_HOME set JAVA_HOME="%TC_INSTALL_DIR%\jre"
set JAVA_HOME="%JAVA_HOME:"=%"

if not defined CATALINA_HOME (
  echo CATALINA_HOME must be set to a Tomcat5.0 installation.
  exit 1
) else (
  set CATALINA_HOME="%CATALINA_HOME:"=%"
  
  if not exist %CATALINA_HOME% (
    echo CATALINA_HOME %CATALINA_HOME% does not exist.
    exit 1
  )
)

set CATALINA_BASE=%SANDBOX%\tomcat5.0\%1
set CATALINA_HOME=%CATALINA_HOME:"=%
set CATALINA_BASE=%CATALINA_BASE:"=%
set JAVA_HOME=%JAVA_HOME:"=%
call "%CATALINA_HOME%\bin\shutdown.bat"
exit %ERRORLEVEL%
endlocal
