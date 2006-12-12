echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

SETLOCAL

REM ------------------------------------------------------------
REM - stop-web-server.bat {tomcat5.0|tomcat5.5|wls8.1} 908{1,2}
REM ------------------------------------------------------------

SET EXIT_ON_ERROR=TRUE
CALL "%~d0%~p0..\%1\stop.bat" %2

EXIT %ERRORLEVEL%
ENDLOCAL
