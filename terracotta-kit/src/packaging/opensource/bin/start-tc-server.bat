@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

if "%1" == "--help" goto :printHelp
if "%1" == "-h" goto :printHelp
if "%1" == "-?" goto :printHelp
goto :start

:printHelp
echo Syntax: %~n0 [-f /path/to/tc-config.xml] [-n server_name]
echo.
echo -f : start the server with your own Terracotta configuration instead of the default one
echo -n : specify which server you want to start when you have more than one servers configured
exit /b 0


:start
setlocal
set TC_INSTALL_DIR=%~d0%~p0..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

set JAVA_HOME="%JAVA_HOME:"=%"

for %%C in ("\bin\java -d64 -server -XX:MaxDirectMemorySize=64g" "\bin\java -server -XX:MaxDirectMemorySize=1g" "\bin\java -d64 -client  -XX:MaxDirectMemorySize=64g" "\bin\java -client -XX:MaxDirectMemorySize=1g" "\bin\java") do (
  set JAVA_COMMAND=%JAVA_HOME%%%~C
  %JAVA_HOME%%%~C -version > NUL 2>&1
  if not errorlevel 1 (
    goto found_command
  )
)

rem rmi.dgc.server.gcInterval is set an year to avoid system gc in case authentication is enabled
rem users may change it accordingly

:found_command
set CLASSPATH=%TC_INSTALL_DIR%\lib\tc.jar
set OPTS=%SERVER_OPT% -Xms512m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError
set OPTS=%OPTS% -Dcom.sun.management.jmxremote
set OPTS=%OPTS% -Dsun.rmi.dgc.server.gcInterval=31536000
set OPTS=%OPTS% -Dtc.install-root=%TC_INSTALL_DIR%

rem set to true if you want Terracotta server uses Derby DB as a store
set USE_DERBYDB="false"

if %USE_DERBYDB% == "true" (
  set DERBY_DB="-Dcom.tc.l2.db.factory.name=com.tc.objectserver.storage.derby.DerbyDBFactory"
  set OPTS=%OPTS% %DERBY_DB%
  echo Starting Terracotta server with Derby DB
)

set JAVA_OPTS=%OPTS% %JAVA_OPTS%
:START_TCSERVER
%JAVA_COMMAND% %JAVA_OPTS% -cp %CLASSPATH% com.tc.server.TCServerMain %*
if %ERRORLEVEL% EQU 11 (
	ECHO start-tc-server: Restarting the server...
	GOTO START_TCSERVER
)
exit /b %ERRORLEVEL%
endlocal
