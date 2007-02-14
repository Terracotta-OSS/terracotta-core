@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

setlocal
set topdir=%~d0%~p0..\..\..
set catalina_home=%topdir%\vendors\tomcat5.5
call "%topdir%\bin\dso-env.bat" -q tc-config.xml
set java_opts=%tc_java_opts% -Dcom.sun.management.jmxremote
set jpda_transport=dt_socket
set jpda_address=localhost:8001

set catalina_base=tomcat1
set java_home=%tc_java_home%
 
start "terracotta for spring: event sample: 8081" "%catalina_home%\bin\catalina.bat" jpda run

:end
endlocal
