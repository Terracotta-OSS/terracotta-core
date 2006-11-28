echo off
rem @COPYRIGHT@

SETLOCAL
cd %~d0%~p0..

SET EXIT_ON_ERROR=TRUE
CALL "..\bin\make-boot-jar.bat" -o ..\..\common\lib\dso-boot -f %1\tc-config.xml
CALL "..\bin\start-tc-server.bat" -f %1\tc-config.xml
EXIT %ERRORLEVEL%
ENDLOCAL
