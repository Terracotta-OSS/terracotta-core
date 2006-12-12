echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

SETLOCAL
cd %~d0%~p0..
CALL "..\bin\stop-tc-server.bat" -f %1\tc-config.xml
EXIT %ERRORLEVEL%
ENDLOCAL
