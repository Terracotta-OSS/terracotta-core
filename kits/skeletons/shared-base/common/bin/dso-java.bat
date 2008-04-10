@echo off

rem
rem  All content copyright (c) 2003-2008 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

if not ""%1""=="""" goto haveArgs
echo usage: dso-java [-options] class [args...]
goto end

:haveArgs
setlocal
set TC_INSTALL_DIR=%~d0%~p0..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

call %TC_INSTALL_DIR%\bin\dso-env.bat -q
%TC_JAVA_HOME%\bin\java %TC_JAVA_OPTS% %JAVA_OPTS% %*
endlocal

:end
