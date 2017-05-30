@echo off

REM 
REM The contents of this file are subject to the Terracotta Public License Version
REM 2.0 (the "License"); You may not use this file except in compliance with the
REM License. You may obtain a copy of the License at 
REM 
REM      http://terracotta.org/legal/terracotta-public-license.
REM 
REM Software distributed under the License is distributed on an "AS IS" basis,
REM WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
REM the specific language governing rights and limitations under the License.
REM 
REM The Covered Software is Terracotta Platform.
REM 
REM The Initial Developer of the Covered Software is 
REM     Terracotta, Inc., a Software AG company
REM

if "%1" == "--help" goto :printHelp
if "%1" == "-h" goto :printHelp
if "%1" == "-?" goto :printHelp
goto :start

:printHelp
echo Syntax: %~n0 [-f \path\to\tc-config.xml] [-n server_name]
echo
echo -f : start the server with your own Terracotta configuration instead of the default one
echo -n : specify which server you want to start when you have more than one servers configured
exit /b 0


:start
setlocal enabledelayedexpansion enableextensions

set TC_INSTALL_DIR=%~d0%~p0..\..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"
set PLUGIN_LIB_DIR=%TC_INSTALL_DIR%\server\plugins\lib
set PLUGIN_API_DIR=%TC_INSTALL_DIR%\server\plugins\api
set SPM_DIR=%TC_INSTALL_DIR%\server\SPM
set SPM_DIR="%SPM_DIR:"=%"

if exist %TC_INSTALL_DIR%\server\bin\setenv.bat (
  call %TC_INSTALL_DIR%\server\bin\setenv.bat
)

if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

set JAVA_HOME="%JAVA_HOME:"=%"
for %%C in ("\bin\java -d64 -server -XX:MaxDirectMemorySize=9223372036854775807" ^
			"\bin\java -server -XX:MaxDirectMemorySize=9223372036854775807" ^
			"\bin\java -d64 -client  -XX:MaxDirectMemorySize=9223372036854775807" ^
			"\bin\java -client -XX:MaxDirectMemorySize=9223372036854775807" ^
			"\bin\java -XX:MaxDirectMemorySize=9223372036854775807") ^
do (

  set JAVA_COMMAND=%JAVA_HOME%%%~C
  %JAVA_HOME%%%~C -version > NUL 2>&1

  if not errorlevel 1 (
	goto setJavaOptsAndClasspath
  )
)
exit /b 1

:setJavaOptsAndClasspath

REM $ENV{NULLGLOB} = 1; 
REM todo do i still need to enable nullglob?

REM fixes bug when command length exceeds max windows command length of 8191
set PLUGIN_CLASSPATH="%PLUGIN_LIB_DIR%\*;%PLUGIN_API_DIR%\*"
REM allows whitespace in directories
set PLUGIN_CLASSPATH="%PLUGIN_CLASSPATH:"=%"

REM   Adding SLF4j libraries to the classpath of the server to 
REM   support services that may use SLF4j for logging
if exist %TC_INSTALL_DIR%\server\lib (
	for %%K in (%TC_INSTALL_DIR%\server\lib\slf4j*.jar) do (
		set PLUGIN_CLASSPATH=!PLUGIN_CLASSPATH!;"%%K"
	)
) else (
		echo %TC_INSTALL_DIR%\server\lib does not exist!
	)
	
REM todo do i still need to enable nullglob?
REM $ENV{NULLGLOB} = 0;

set CLASSPATH=%TC_INSTALL_DIR%\server\lib\tc.jar;%PLUGIN_CLASSPATH%
set OPTS=%SERVER_OPT% -Xms256m -Xmx2g -XX:+HeapDumpOnOutOfMemoryError
set OPTS=%OPTS% -Dcom.sun.management.jmxremote
rem rmi.dgc.server.gcInterval is set as year to avoid system gc in case authentication is enabled
rem users may change it accordingly
set OPTS=%OPTS% -Dsun.rmi.dgc.server.gcInterval=31536000000
set OPTS=%OPTS% -Dtc.install-root=%TC_INSTALL_DIR%
set JAVA_OPTS=%OPTS% %JAVA_OPTS%


:START_TCSERVER
rem call START_TCSERVER_AND_SAVE_PID when SPM directory exists, i.e. Terracotta is being used as an SPM plugin
set cmd=%JAVA_COMMAND% %JAVA_OPTS% -cp %CLASSPATH% com.tc.server.TCServerMain %*

if exist %SPM_DIR% (
	call :START_TCSERVER_AND_SAVE_PID %cmd%
) else (
	%cmd%
)

if %ERRORLEVEL% EQU 11 (
	echo start-tc-server: Restarting the server...
	goto START_TCSERVER
)
exit /b %ERRORLEVEL%
endlocal

:START_TCSERVER_AND_SAVE_PID
setlocal enabledelayedexpansion enableextensions
rem Change the startup behavior of Winmgmt service to automatic
sc config Winmgmt start= auto > nul
set PID=
set tab=
set COMMAND=%*
set cur_dir=%cd%
set "COMMAND=%COMMAND:"=%"

for /F "usebackq tokens=1,2 delims=;=%tab% " %%i in (
    `wmic process call create "%COMMAND%"^, "%cur_dir%"`
) do (
    if /I %%i EQU ProcessId (
        set PID=%%j
    )
)
if not exist %SPM_DIR%\instance (
	mkdir %SPM_DIR%\instance
)
echo %PID% > %SPM_DIR%\instance\run.pid

exit /b %ERRORLEVEL%
endlocal