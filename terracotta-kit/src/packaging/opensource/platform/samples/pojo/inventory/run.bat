@echo off

rem
rem  All content copyright Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

rem
rem samples\pojos\inventory
rem
rem Environment variables required by dso-env helper script:
rem  JAVA_HOME: root of Java Development Kit installation
rem  TC_INSTALL_DIR: root of Terracotta installation
rem
rem Arguments to dso-env helper script:
rem  -q: do not print value of TC_JAVA_OPTS
rem  tc-config.xml: path to DSO config file
rem
rem Environment variable set by dso-env helper script:
rem  TC_JAVA_OPTS: Java options needed to activate DSO
rem

setlocal
set TC_INSTALL_DIR=%~d0%~p0..\..\..\..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

cd %~d0%~p0

if not defined JAVA_HOME set JAVA_HOME="%TC_INSTALL_DIR%\jre"
set JAVA_HOME="%JAVA_HOME:"=%"

set TC_CONFIG_PATH=tc-config.xml
call %TC_INSTALL_DIR%\platform\bin\dso-env.bat -q "%TC_CONFIG%"
set JAVA_OPTS=%TC_JAVA_OPTS% %JAVA_OPTS%
start "inventory" %JAVA_HOME%\bin\java %JAVA_OPTS% -cp classes demo.inventory.Main %*
endlocal
