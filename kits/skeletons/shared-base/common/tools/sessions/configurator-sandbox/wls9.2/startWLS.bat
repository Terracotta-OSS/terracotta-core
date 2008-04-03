@echo off
rem
rem All content copyright (c) 2003-2008 Terracotta, Inc.,
rem except as may otherwise be noted in a separate copyright notice.
rem All rights reserved.
rem

setlocal

if not defined BEA_HOME (
  echo BEA_HOME must be set to a Weblogic Server 9.2 installation.
  endlocal  
  exit 1
)
set BEA_HOME="%BEA_HOME:"=%"

if not exist %BEA_HOME% (
  echo BEA_HOME %BEA_HOME% does not exist.
  endlocal
  exit 1
)

if not defined WL_HOME set WL_HOME=%BEA_HOME%\weblogic92
set WL_HOME="%WL_HOME:"=%"

if not exist %WL_HOME% (
  echo WL_HOME %WL_HOME% does not exist.
  endlocal
  exit 1
)

set PRODUCTION_MODE=
set JAVA_VENDOR=Sun

call %WL_HOME%\common\bin\commEnv.cmd

set SERVER_NAME=myserver
set WLS_USER=weblogic
set WLS_PW=weblogic

if not defined JAVA_HOME set JAVA_HOME=%BEA_HOME%\jdk150_10
set JAVA_HOME="%JAVA_HOME:"=%"

if not exist %JAVA_HOME% (
  echo JAVA_HOME %JAVA_HOME% does not exist.
  exit 1
  endlocal  
)

set CLASSPATH=%WEBLOGIC_CLASSPATH%;%POINTBASE_CLASSPATH%;%JAVA_HOME%\jre\lib\rt.jar;%WL_HOME%\server\lib\webservices.jar;%CLASSPATH%

%JAVA_HOME%\bin\java %JAVA_VM% %MEM_ARGS% %JAVA_OPTIONS% -classpath %CLASSPATH% -Dweblogic.Name=%SERVER_NAME% -Dweblogic.management.username=%WLS_USER% -Dweblogic.management.password=%WLS_PW% -Dweblogic.ProductionModeEnabled=%PRODUCTION_MODE% -Djava.security.policy="%WL_HOME%\server\lib\weblogic.policy" weblogic.Server
endlocal

