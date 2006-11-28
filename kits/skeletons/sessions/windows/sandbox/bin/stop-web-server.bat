echo off
rem @COPYRIGHT@

SETLOCAL

REM ------------------------------------------------------------
REM - stop-web-server.bat {tomcat5.0|tomcat5.5|wls8.1} 908{1,2}
REM ------------------------------------------------------------

SET EXIT_ON_ERROR=TRUE
CALL "%~d0%~p0..\%1\stop.bat" %2

EXIT %ERRORLEVEL%
ENDLOCAL
