@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

if not ""%1""=="""" goto haveArgs
echo usage: dso-java [-options] class [args...]
goto end

:haveArgs
setlocal
if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

set JAVA_HOME="%JAVA_HOME:"=%"

set TC_INSTALL_DIR=%~d0%~p0..\..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

call %TC_INSTALL_DIR%\platform\bin\dso-env.bat -q
echo Starting Terracotta client...
%JAVA_HOME%\bin\java %TC_JAVA_OPTS% %JAVA_OPTS% %*
exit /b %ERRORLEVEL%
endlocal

:end
