@echo off
rem @COPYRIGHT@

setlocal
set topdir=%~d0%~p0..\..

if "x%tc_install_dir%"=="x" set tc_install_dir=%topdir%\..
set catalina_home=%tc_install_dir%\vendors\tomcat5.5

call "%topdir%\libexec\tc-functions.bat" tc_install_dir "%tc_install_dir%" true
if "%exitflag%"=="true" goto end

call "%topdir%\bin\dso-env.bat" -q tc-config.xml
set java_opts=%tc_java_opts% -Dcom.sun.management.jmxremote
set java_opts=%java_opts% -Dcom.sun.management.jmxremote.port=8091
set java_opts=%java_opts% -Dcom.sun.management.jmxremote.authenticate=false
set java_opts=%java_opts% -Dcom.sun.management.jmxremote.ssl=false

set java_opts=%java_opts% -Dtc.node-name=Node1

set catalina_base=tomcat1
set java_home=%tc_java_home%
 
start "terracotta for spring: webflow sample: 8081" "%catalina_home%\bin\catalina.bat" run

:end
endlocal
