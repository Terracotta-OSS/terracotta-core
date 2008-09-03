@echo off

rem
rem  All content copyright (c) 2003-2008 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

setlocal
cd %~d0%~p0..\..\..
set SANDBOX=%CD%\sessions\configurator-sandbox\%1
call "..\bin\stop-tc-server.bat" -f "%SANDBOX%\tc-config.xml"
exit /b %ERRORLEVEL%
endlocal
