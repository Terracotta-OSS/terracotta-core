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
set java_opts=%java_opts% -Dcom.sun.management.jmxremote.port=8092
set java_opts=%java_opts% -Dcom.sun.management.jmxremote.authenticate=false
set java_opts=%java_opts% -Dcom.sun.management.jmxremote.ssl=false

set java_opts=%java_opts% -Dtc.node-name=Node2

set catalina_base=tomcat2
set java_home=%tc_java_home%
 
start "terracotta for spring: webflow sample: 8082" "%catalina_home%\bin\catalina.bat" run

:end
endlocal
