@echo off

rem
rem All content copyright (c) 2003-2008 Terracotta, Inc.,
rem except as may otherwise be noted in a separate copyright notice.
rem All rights reserved.
rem

rem -------------------------------------
rem stop.cmd 908{1,2}
rem -------------------------------------

setlocal
if not defined BEA_HOME (
   echo BEA_HOME must be set to a Weblogic Server 8.1 installation.
   exit /b 1
   endlocal  
)
set BEA_HOME="%BEA_HOME:"=%"
set WL_HOME=%BEA_HOME%\weblogic81
set WL_HOME="%WL_HOME:"=%"

set PRODUCTION_MODE=
set ADMIN_URL=t3://localhost:%1
set JAVA_VENDOR=Sun
set SERVER_NAME=myserver

if not defined JAVA_HOME set JAVA_HOME=%BEA_HOME%\jdk142_11
set JAVA_HOME="%JAVA_HOME:"=%"

if not exist %JAVA_HOME% (
   echo JAVA_HOME %JAVA_HOME% does not exist.
   exit /b 1
   endlocal  
)

call %WL_HOME%\common\bin\commEnv.cmd
set CLASSPATH=%WEBLOGIC_CLASSPATH%;%POINTBASE_CLASSPATH%;%JAVA_HOME%\jre\lib\rt.jar;%WL_HOME%\server\lib\webservices.jar;%CLASSPATH%

echo Stopping Weblogic Server...
%JAVA_HOME%\bin\java -cp "%CLASSPATH%" weblogic.Admin FORCESHUTDOWN -url %ADMIN_URL% -username tc -password tc %SERVER_NAME%

echo Done
exit /b %ERRORLEVEL%
endlocal
