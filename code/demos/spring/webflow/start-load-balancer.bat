@echo off
rem @COPYRIGHT@

setlocal
set local_dir=%~d0%~p0
set topdir=%local_dir%..\..
if "x%tc_install_dir%"=="x" set tc_install_dir=%topdir%\..

call "%topdir%\libexec\tc-functions.bat" tc_install_dir "%tc_install_dir%" true
if "%exitflag%"=="true" goto end

set java_home=%tc_java_home%
start "terracotta for spring: webflow sample: load balancer" call "%topdir%\libexec\tc-functions.bat" tc_java -classpath "%tc_install_dir%\common\lib\tc.jar" -Ddaemon=true com.tc.net.proxy.TCPProxy 8080 localhost:8081,localhost:8082,localhost:8083

:end
endlocal
