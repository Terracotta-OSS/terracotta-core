@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

SETLOCAL

REM --------------------------------------------------------------------
REM - stop.bat 908{1,2}
REM --------------------------------------------------------------------

cd %~d0%~p0..
SET SANDBOX=%CD%
SET TC_INSTALL_DIR=%SANDBOX%\..\..

SET JAVA_HOME=%TC_INSTALL_DIR%\jre
IF NOT "x%TC_JAVA_HOME%" == "x" (
  IF NOT EXIST "%TC_JAVA_HOME%" (
    ECHO TC_JAVA_HOME of '%TC_JAVA_HOME%' does not exist.
    EXIT 1
  )
  SET JAVA_HOME=%TC_JAVA_HOME%
)

IF "x%CATALINA_HOME%" == "x" (
  SET CATALINA_HOME=%TC_INSTALL_DIR%\vendors\tomcat5.5
  ECHO Using CATALINA_HOME of '%CATALINA_HOME%'.
) else (
  IF NOT EXIST "%CATALINA_HOME%" (
    ECHO CATALINA_HOME of '%CATALINA_HOME%' does not exist.
    EXIT 1
  )
)

SET CATALINA_BASE=%SANDBOX%\tomcat5.5\%1

call "%CATALINA_HOME%\bin\shutdown.bat"
EXIT %ERRORLEVEL%
ENDLOCAL
