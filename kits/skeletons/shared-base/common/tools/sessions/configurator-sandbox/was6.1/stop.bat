@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

rem -------------------------------------
rem - start.bat -debug 908{1,2}
rem -------------------------------------

setlocal

cd %~d0%~p0
set WAS_SANDBOX=%CD%
set TC_INSTALL_DIR=%WAS_SANDBOX%\..\..\..\..

if ""%1"" == ""-debug"" (
  set DEBUG=true
)

set PORT=%1

if "-%WAS_HOME%-" == "--" (
  echo WAS_HOME must point to a valid WebSphere Application Server 6.1 installation
  set ERROR_LEVEL=1
  goto end
)

IF NOT EXIST "%WAS_HOME%\java" (
  echo Unable to find IBM JRE at "%WAS_HOME%\java"
  set ERROR_LEVEL=1
  goto end
)

set JAVA_HOME=%WAS_HOME%\java

echo Stopping WebSphere Application Server on port "%PORT%"...
"%WAS_HOME%/bin/stopServer.bat" server1 -profileName "tc-%PORT%"


:end
endlocal
echo ERROR %ERRORLEVEL%
