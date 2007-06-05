@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

rem -------------------------------------
rem - start.bat -debug 908{1,2} [nodso]
rem -------------------------------------

setlocal

cd %~d0%~p0
echo %~d0%~p0
set WAS_SANDBOX=%CD%
set TC_INSTALL_DIR=%WAS_SANDBOX%\..\..\..\..

echo WAS_SANDBOX : %WAS_SANDBOX%
echo TC_INSTALL_DIR : %TC_INSTALL_DIR%

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

set TC_CONFIG_PATH=%WAS_SANDBOX%\tc-config.xml
call "%TC_INSTALL_DIR%\bin\dso-env.bat" -q "%TC_CONFIG%"
if %ERRORLEVEL% neq 0 goto end


if ""%2"" == ""nodso"" (
  set NODSO=true
  goto doCheckProfile
)

set NODSO=false

set OPTS=%TC_JAVA_OPTS% -Dwebserver.log.name=%PORT%
set OPTS=%OPTS% -Dcom.sun.management.jmxremote
set OPTS=%OPTS% -Dtc.node-name=was-%PORT%
set JAVA_OPTIONS=%OPTS% %JAVA_OPTS%


:doCheckProfile

set WAS_POLICY="%WAS_HOME%/profiles/tc-%PORT%/properties/server.policy"

@rem call "%WAS_HOME%/bin/manageprofiles.bat" -listProfiles | grep -q "tc-%PORT%"
@rem if %ERRORLEVEL% neq 0 (
if EXIST "%WAS_HOME%\profiles\tc-%PORT%" (
  echo WebSphere profile "tc-%PORT%" already exists, skipping profile creation for port "%PORT%"
  goto doRunWAS
)

echo Creating profile "tc-%PORT%" 'for' port "%PORT%"...
echo. 
echo  "	==> THIS CAN TAKE A LONG TIME SO PLEASE BE PATIENT <=="
echo.

call "%WAS_HOME%/bin/manageprofiles.bat" -create -templatePath "%WAS_HOME%/profileTemplates/default" -portsFile "%WAS_SANDBOX%/profiles/%PORT%.port-defs" -profileName "tc-%PORT%" -enableAdminSecurity false -isDeveloperServer
if %ERRORLEVEL% neq 0 goto end


echo Adding Terracotta codebase to server.policy in profile "tc-%PORT%" 'for' port "%PORT%"...
echo. >> %WAS_POLICY%
echo grant codeBase "file:%TC_INSTALL_DIR%/lib/-" { >> %WAS_POLICY%
echo   permission java.security.AllPermission; >> %WAS_POLICY%
echo }; >> %WAS_POLICY%
echo. >> %WAS_POLICY%
echo grant codeBase "file:%TC_INSTALL_DIR%/lib/dso-boot/-" { >> %WAS_POLICY%
echo   permission java.security.AllPermission; >> %WAS_POLICY%
echo }; >> %WAS_POLICY%
echo. >> %WAS_POLICY%


:doRunWAS

echo WAS_SANDBOX=%WAS_SANDBOX%
call "%WAS_HOME%/bin/wsadmin.bat" -lang jython -connType NONE -profileName "tc-%PORT%" -f "%WAS_SANDBOX%/deployApps.py"
if %ERRORLEVEL% neq 0 goto end


if "%NODSO%" == "true" goto disableDSO

echo Instrument WebSphere for use with Terracotta

call "%WAS_HOME%/bin/wsadmin.bat" -connType NONE -profileName "tc-%PORT%" -f "%WAS_SANDBOX%/toggle-dso.py" "true"
if %ERRORLEVEL% neq 0 goto end


goto doStartWAS


:disableDSO

echo Make sure DSO is not enabled in WebSphere

call "%WAS_HOME%/bin/wsadmin.bat" -connType NONE -profileName "tc-%PORT%" -f "%WAS_SANDBOX%/toggle-dso.py" "false"
if %ERRORLEVEL% neq 0 goto end


:doStartWAS

echo Starting WebSphere Application Server on port "%PORT%"...

call "%WAS_HOME%/bin/startServer.bat" server1 -profileName "tc-%PORT%"
if %ERRORLEVEL% neq 0 goto doStopserver
goto doWaitForshutdown


:doStopServer
echo Unable to start WebSphere Application Server on port "%PORT%"
call "%WAS_HOME%/bin/stopServer.bat" server1 -profileName "tc-%PORT%"
set ERROR_LEVEL=1
goto end


:doWaitForshutdown

call "%WAS_HOME%/bin/wsadmin.bat" -profileName "tc-%PORT%" -f "%WAS_SANDBOX%/wait-for-shutdown.py"


:end
echo ERROR %ERRORLEVEL%
endlocal
