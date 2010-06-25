@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

set TC_INSTALL_DIR=%~d0%~p0..\..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

if "%1" == "-q" GOTO tc_dso_env_0
setlocal
set __DSO_ENV_QUIET=false
goto tc_dso_env_1

:tc_dso_env_0
  shift
  set __DSO_ENV_QUIET=true
  
:tc_dso_env_1
  if "%1" == "-f" shift
  if "%1" == "--config" shift
  if not "%~1" == "" set TC_CONFIG_PATH=%~1

  call %TC_INSTALL_DIR%\platform\bin\boot-jar-path.bat
  if %ERRORLEVEL% neq 0 (
    pause
    exit /b %ERRORLEVEL%
  )

  set TC_JAVA_OPTS=-Xbootclasspath/p:%DSO_BOOT_JAR% -Dtc.install-root=%TC_INSTALL_DIR%
  if defined TC_CONFIG_PATH set TC_JAVA_OPTS=%TC_JAVA_OPTS% -Dtc.config=%TC_CONFIG_PATH%
  if defined TC_SERVER set TC_JAVA_OPTS=%TC_JAVA_OPTS% -Dtc.server="%TC_SERVER%"
  if not "%__DSO_ENV_QUIET%" == "true" goto tc_dso_env_2
  goto tc_dso_env_3

:tc_dso_env_2
  echo %TC_JAVA_OPTS%
  endlocal

:tc_dso_env_3
