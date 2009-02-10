@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

rem
rem This script is for use by the Session Configurator and not meant to be
rem executed manually.
rem

REM ------------------------------------------------------------
REM - stop-web-server.bat {tomcat5.0|tomcat5.5|wls8.1} 908{1,2}
REM ------------------------------------------------------------

setlocal
set EXIT_ON_ERROR=TRUE
call "%~d0%~p0..\%1\stop.bat" %2
exit %ERRORLEVEL%
endlocal
