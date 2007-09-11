@echo off
rem
rem All content copyright (c) 2003-2006 Terracotta, Inc.,
rem except as may otherwise be noted in a separate copyright notice.
rem All rights reserved.
rem

SETLOCAL

if "x%BEA_HOME%" == "x" (
  echo BEA_HOME must be set to a 9.2 installation.
  exit 1
  endlocal
)

if not exist "%BEA_HOME%" (
  echo BEA_HOME '%BEA_HOME%' does not exist.
  exit 1
  endlocal
)

if NOT "x%WL_HOME%" == "x" goto haveWebLogicHome
set WL_HOME=%BEA_HOME%\weblogic92

if not exist "%WL_HOME%" (
  echo WL_HOME '%WL_HOME%' does not exist. Did you set BEA_HOME correctly?
  exit 1
  endlocal
)

:haveWebLogicHome

set PRODUCTION_MODE=
set JAVA_VENDOR=Sun

call "%WL_HOME%\common\bin\commEnv.cmd"

set SERVER_NAME=myserver

set WLS_USER=weblogic
set WLS_PW=weblogic

set CLASSPATH=%WEBLOGIC_CLASSPATH%;%POINTBASE_CLASSPATH%;%JAVA_HOME%\jre\lib\rt.jar;%WL_HOME%\server\lib\webservices.jar;%CLASSPATH%

"%JAVA_HOME%\bin\java" %JAVA_VM% %MEM_ARGS% %JAVA_OPTIONS% -classpath "%CLASSPATH%" -Dweblogic.Name=%SERVER_NAME% -Dweblogic.management.username=%WLS_USER% -Dweblogic.management.password=%WLS_PW% -Dweblogic.ProductionModeEnabled=%PRODUCTION_MODE% -Djava.security.policy="%WL_HOME%\server\lib\weblogic.policy" weblogic.Server

ENDLOCAL
