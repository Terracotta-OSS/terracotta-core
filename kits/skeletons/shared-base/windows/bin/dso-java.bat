echo off
rem @COPYRIGHT@

SETLOCAL
SET TOPDIR=%~d0%~p0..
IF "x%TC_INSTALL_DIR%"=="x" SET TC_INSTALL_DIR=%TOPDIR%\..
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_install_dir "%TC_INSTALL_DIR%" TRUE
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_classpath "" FALSE
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_java_opts
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_set_dso_boot_jar
CALL "%TOPDIR%\libexec\tc-functions.bat" tc_java "-Xbootclasspath/p:%DSO_BOOT_JAR%" "-Dtc.install-root=%TC_INSTALL_DIR%" %TC_ALL_JAVA_OPTS% %*
ENDLOCAL
