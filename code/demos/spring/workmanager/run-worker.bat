@echo off

rem @COPYRIGHT@

SETLOCAL
SET TOPDIR=%~d0%~p0..\..
IF "x%TC_INSTALL_DIR%"=="x" SET TC_INSTALL_DIR=%TOPDIR%\..
CD %~d0%~p0

CALL "%TOPDIR%\libexec\tc-functions.bat" tc_install_dir "%TC_INSTALL_DIR%" TRUE
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_classpath "classes;lib\servlet-api.jar;lib\commonj-twm.jar;lib\commons-logging-1.0.4.jar;lib\spring-2.0.jar" FALSE
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_java_opts
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_config "tc-config.xml"
START CALL "%TOPDIR%\libexec\tc-functions.bat" run_dso_java -classpath "'%TC_CLASSPATH%'" "%D_TC_CONFIG%" "%TC_ALL_JAVA_OPTS%" demo.workmanager.Main worker "%*"
ENDLOCAL
