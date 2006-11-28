@echo off

rem @COPYRIGHT@

SETLOCAL
SET TOPDIR=%~d0%~p0..\..
IF "x%TC_INSTALL_DIR%"=="x" SET TC_INSTALL_DIR=%TOPDIR%\..
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_install_dir "%TC_INSTALL_DIR%" TRUE

set cp=%cp%;lib\commons-logging-1.0.4.jar
set cp=%cp%;lib\spring-1.2.8.jar
set cp=%cp%;target\inventory.jar

CALL "%TOPDIR%\libexec\tc-functions.bat" tc_classpath "%cp%" FALSE
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_java_opts ""
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_config tc-config.xml
CALL "%TOPDIR%\libexec\tc-functions.bat" run_dso_java -classpath "'%TC_CLASSPATH%'" "%D_TC_CONFIG%" "%TC_ALL_JAVA_OPTS%" test.inventory.Client "%*"
ENDLOCAL
