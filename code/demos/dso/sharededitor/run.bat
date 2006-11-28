@echo off
rem @COPYRIGHT@

setlocal
set topdir=%~d0%~p0..\..
if "x%tc_install_dir%"=="x" set tc_install_dir=%topdir%\..
cd %~d0%~p0
call "%topdir%\libexec\tc-functions.bat" tc_install_dir "%tc_install_dir%" true
call "%topdir%\bin\dso-env.bat" tc-config.xml
start call "%topdir%\libexec\tc-functions.bat" tc_java %tc_java_opts% -cp "classes" "-Djava.awt.Window.locationByPlatform=true" demo.sharededitor.Main %*
endlocal
