@echo off
rem @COPYRIGHT@

SET TOPDIR=%~d0%~p0..

IF "%1" == "-q" GOTO tc_dso_env_0
SETLOCAL
SET __DSO_ENV_QUIET=false
GOTO tc_dso_env_1

:tc_dso_env_0
  SHIFT
  SET __DSO_ENV_QUIET=false
  
:tc_dso_env_1
  SET __DSO_ENV_CONFIG=%~1

  IF "x%TC_INSTALL_DIR%" == "x" SET TC_INSTALL_DIR=%TOPDIR%\..
  CALL "%TOPDIR%\libexec\tc-functions.bat" tc_install_dir "%TC_INSTALL_DIR%" TRUE
  CALL "%TOPDIR%\libexec\tc-functions.bat" tc_config "%__DSO_ENV_CONFIG%"
  CALL "%TOPDIR%\libexec\tc-functions.bat" tc_set_dso_boot_jar
 
  SET TC_JAVA_OPTS=-Xbootclasspath/p:"%DSO_BOOT_JAR%" -Dtc.install-root="%TC_INSTALL_DIR%"
  IF NOT "%__DSO_ENV_CONFIG%" == "" SET TC_JAVA_OPTS=%TC_JAVA_OPTS% %D_TC_CONFIG%
  IF NOT "%__DSO_ENV_QUIET%" == "" GOTO tc_dso_env_2
  GOTO tc_dso_env_3

:tc_dso_env_2
  ECHO %TC_JAVA_OPTS%
  ENDLOCAL

:tc_dso_env_3
  REM dso-env completes

