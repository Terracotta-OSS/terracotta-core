echo off
rem @COPYRIGHT@

SETLOCAL

REM ----------------------------------------------------------
REM - start.bat {tomcat5.0|tomcat5.5|wls8.1} 908{1,2} [nodso]
REM ----------------------------------------------------------

SET EXIT_ON_ERROR=TRUE
CALL "%~d0%~p0..\%1\start.bat" %2 %3
EXIT %ERRORLEVEL%
ENDLOCAL
