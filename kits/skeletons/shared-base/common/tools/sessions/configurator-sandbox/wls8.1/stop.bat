rem
rem All content copyright (c) 2003-2006 Terracotta, Inc.,
rem except as may otherwise be noted in a separate copyright notice.
rem All rights reserved.
rem

echo off

REM -------------------------------------
REM stop.cmd 908{1,2}
REM -------------------------------------

SETLOCAL

REM for %%i in ("%BEA_HOME%") do set BEA_HOME=%%~fsi

set WL_HOME=%BEA_HOME%\weblogic81
REM for %%i in ("%WL_HOME%") do set WL_HOME=%%~fsi

set PRODUCTION_MODE=
set ADMIN_URL=t3://localhost:%1
set JAVA_VENDOR=Sun
set SERVER_NAME=myserver

IF "%TC_JAVA_HOME%" == "" (
  SET TC_JAVA_HOME=%BEA_HOME%\jdk142_11
)
set JAVA_HOME=%TC_JAVA_HOME%

REM for %%i in ("%JAVA_HOME%") do set JAVA_HOME=%%~fsi

call "%WL_HOME%\common\bin\commEnv.cmd"

set CLASSPATH=%WEBLOGIC_CLASSPATH%;%POINTBASE_CLASSPATH%;%JAVA_HOME%\jre\lib\rt.jar;%WL_HOME%\server\lib\webservices.jar;%CLASSPATH%

echo Stopping Weblogic Server...

"%JAVA_HOME%\bin\java" -cp "%CLASSPATH%" weblogic.Admin FORCESHUTDOWN -url %ADMIN_URL% -username tc -password tc %SERVER_NAME%

echo Done

exit

ENDLOCAL
