echo off
rem @COPYRIGHT@

SETLOCAL
cd %~d0%~p0..
CALL "..\bin\stop-tc-server.bat" -f %1\tc-config.xml
EXIT %ERRORLEVEL%
ENDLOCAL
