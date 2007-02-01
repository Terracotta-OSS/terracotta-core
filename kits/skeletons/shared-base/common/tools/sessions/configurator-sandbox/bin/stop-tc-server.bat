@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

SETLOCAL
cd %~d0%~p0..\..\..
SET SANDBOX=%CD%\sessions\configurator-sandbox\%1
CALL "..\bin\stop-tc-server.bat" -f %SANDBOX%\tc-config.xml
EXIT %ERRORLEVEL%
ENDLOCAL
