@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

setlocal
set topdir=%~d0%~p0..\..

if "x%tc_install_dir%"=="x" set tc_install_dir=%topdir%\..
set catalina_home=%tc_install_dir%\vendors\tomcat5.5

call "%topdir%\libexec\tc-functions.bat" tc_install_dir "%tc_install_dir%" true
if "%exitflag%"=="true" goto end

call "%topdir%\bin\dso-env.bat" -q tc-config.xml
set java_opts=%tc_java_opts% -Dcounter.log.prefix="CounterService-Tomcat-Node-2:"
set catalina_base=tomcat2
set java_home=%tc_java_home%
 
rem you can run tomcat in a debugger by using the following options
rem set jpda_transport=dt_socket
rem set jpda_address=8096
rem start "terracotta for spring: thread coordination sample: tomcat server node 2" "%catalina_home%\bin\catalina.bat" jpda run

start "terracotta for spring: thread coordination sample: tomcat server node 2" "%catalina_home%\bin\catalina.bat" run

:end
endlocal
