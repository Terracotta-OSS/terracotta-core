@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

if not ""%1""=="""" goto haveArgs
echo usage: dso-java [-options] class [args...]
goto end

:haveArgs
setlocal
set TC_INSTALL_DIR=%~d0%~p0..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

call %TC_INSTALL_DIR%\bin\dso-env.bat -q
echo Starting Terracotta client...
%TC_JAVA_HOME%\bin\java %TC_JAVA_OPTS% %JAVA_OPTS% %*
endlocal

:end
