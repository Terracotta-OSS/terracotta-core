rem
rem All content copyright (c) 2003-2006 Terracotta, Inc.,
rem except as may otherwise be noted in a separate copyright notice.
rem All rights reserved.
rem

rem -------------------------------------
rem stop.cmd 908{1,2}
rem -------------------------------------

echo off

setlocal

rem for %%i in ("%BEA_HOME%") do set BEA_HOME=%%~fsi

set WL_HOME=%BEA_HOME%\weblogic92
rem for %%i in ("%WL_HOME%") do set WL_HOME=%%~fsi

set PRODUCTION_MODE=
set ADMIN_URL=t3://localhost:%1
set JAVA_VENDOR=Sun
set SERVER_NAME=myserver

if "%JAVA_HOME%" == "" (
  set JAVA_HOME=%BEA_HOME%\jdk150_10
)

rem for %%i in ("%JAVA_HOME%") do set JAVA_HOME=%%~fsi

call "%WL_HOME%\common\bin\commEnv.cmd"

set CLASSPATH=%WEBLOGIC_CLASSPATH%;%POINTBASE_CLASSPATH%;%JAVA_HOME%\jre\lib\rt.jar;%WL_HOME%\server\lib\webservices.jar;%CLASSPATH%

echo Stopping Weblogic Server...

"%JAVA_HOME%\bin\java" -cp "%CLASSPATH%" weblogic.Admin FORCESHUTDOWN -url %ADMIN_URL% -username weblogic -password weblogic %SERVER_NAME%

echo Done
exit
endlocal
