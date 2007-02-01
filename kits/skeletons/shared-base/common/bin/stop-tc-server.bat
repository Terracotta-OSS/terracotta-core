@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

SETLOCAL
SET TOPDIR=%~d0%~p0..
IF "x%TC_INSTALL_DIR%"=="x" SET TC_INSTALL_DIR=%TOPDIR%
CALL "%TOPDIR%\bin\tc-functions.bat" tc_install_dir "%TC_INSTALL_DIR%"
CALL "%TOPDIR%\bin\tc-functions.bat" tc_classpath "" TRUE
CALL "%TOPDIR%\bin\tc-functions.bat" tc_java_opts "-Dtc.install-root=%TC_INSTALL_DIR%"
CALL "%TOPDIR%\bin\tc-functions.bat" tc_java -classpath "%TC_CLASSPATH%" %TC_ALL_JAVA_OPTS% com.tc.admin.TCStop %*
ENDLOCAL
