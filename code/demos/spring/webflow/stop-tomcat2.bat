@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

setlocal
set topdir=%~d0%~p0..\..\..

if "x%tc_install_dir%"=="x" set tc_install_dir=%topdir%
set catalina_home=%tc_install_dir%\vendors\tomcat5.5

call "%topdir%\bin\tc-functions.bat" tc_install_dir "%tc_install_dir%" true
if "%exitflag%"=="true" goto end

set catalina_base=tomcat2
set java_home=%tc_java_home%

echo "stopping terracotta for spring: webflow sample: 8082" 
"%catalina_home%\bin\catalina.bat" stop

:end
endlocal
