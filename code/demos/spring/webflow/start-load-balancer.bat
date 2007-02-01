@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

setlocal
set local_dir=%~d0%~p0
set topdir=%local_dir%..\..\..
if "x%tc_install_dir%"=="x" set tc_install_dir=%topdir%

call "%topdir%\bin\tc-functions.bat" tc_install_dir "%tc_install_dir%" true
if "%exitflag%"=="true" goto end

set java_home=%tc_java_home%
start "terracotta for spring: webflow sample: load balancer" call "%topdir%\bin\tc-functions.bat" tc_java -classpath "%tc_install_dir%\lib\tc.jar" -Ddaemon=true com.tc.net.proxy.TCPProxy 8080 localhost:8081,localhost:8082,localhost:8083

:end
endlocal
