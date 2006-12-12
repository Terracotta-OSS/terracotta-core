echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

SETLOCAL
cd %~d0%~p0..

SET EXIT_ON_ERROR=TRUE
CALL "..\bin\make-boot-jar.bat" -o ..\..\common\lib\dso-boot -f %1\tc-config.xml
CALL "..\bin\start-tc-server.bat" -f %1\tc-config.xml
EXIT %ERRORLEVEL%
ENDLOCAL
