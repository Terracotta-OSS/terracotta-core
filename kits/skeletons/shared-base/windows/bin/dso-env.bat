@echo off
rem @COPYRIGHT@

SET TOPDIR=%~d0%~p0..
SET __CONFIG=%~1

CALL "%TOPDIR%\libexec\tc-functions.bat" tc_install_dir "%TOPDIR%"\.. TRUE
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_config "%__CONFIG%"
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_set_dso_boot_jar
 
SET TC_JAVA_OPTS=-Xbootclasspath/p:"%DSO_BOOT_JAR%" -Dtc.install-root="%TC_INSTALL_DIR%"
IF NOT "%__CONFIG%" == "" SET TC_JAVA_OPTS=%TC_JAVA_OPTS% %D_TC_CONFIG%

ECHO %TC_JAVA_OPTS%
