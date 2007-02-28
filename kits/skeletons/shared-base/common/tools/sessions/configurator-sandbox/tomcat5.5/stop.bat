@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

rem --------------------------------------------------------------------
rem - stop.bat 908{1,2}
rem --------------------------------------------------------------------

setlocal

cd %~d0%~p0..
set SANDBOX=%CD%
set TC_INSTALL_DIR=%SANDBOX%\..\..\..

if not exist "%JAVA_HOME%" set JAVA_HOME=%TC_INSTALL_DIR%\jre

if "x%CATALINA_HOME%" == "x" (
  set CATALINA_HOME=%TC_INSTALL_DIR%\vendors\tomcat5.5
  echo Using CATALINA_HOME of '%CATALINA_HOME%'.
) else (
  if not exist "%CATALINA_HOME%" (
    echo CATALINA_HOME of '%CATALINA_HOME%' does not exist.
    exit 1
  )
)

set CATALINA_BASE=%SANDBOX%\tomcat5.5\%1

call "%CATALINA_HOME%\bin\shutdown.bat"
exit %ERRORLEVEL%
endlocal
