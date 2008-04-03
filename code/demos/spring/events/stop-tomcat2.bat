@echo off

rem
rem  All content copyright (c) 2003-2008 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

setlocal
cd %~d0%~p0
set TC_INSTALL_DIR=..\..\..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

set CATALINA_HOME=%TC_INSTALL_DIR%\vendors\tomcat5.5

if not defined JAVA_HOME set JAVA_HOME="%TC_INSTALL_DIR%\jre"
set JAVA_HOME="%JAVA_HOME:"=%"

set CATALINA_BASE=tomcat2
echo "stopping terracotta for spring: event sample: tomcat server node 2" 

set JAVA_HOME=%JAVA_HOME:"=%
%CATALINA_HOME%\bin\catalina.bat stop
endlocal
