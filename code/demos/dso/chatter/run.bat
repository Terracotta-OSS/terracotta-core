@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

setlocal
set topdir=%~d0%~p0..\..
if "x%tc_install_dir%"=="x" set tc_install_dir=%topdir%\..
cd %~d0%~p0
call "%topdir%\libexec\tc-functions.bat" tc_install_dir "%tc_install_dir%" true
call "%topdir%\bin\dso-env.bat" -q tc-config.xml
start call "%topdir%\libexec\tc-functions.bat" tc_java %tc_java_opts% -cp "classes" "-Djava.awt.Window.locationByPlatform=true" demo.chatter.Main %*
endlocal
