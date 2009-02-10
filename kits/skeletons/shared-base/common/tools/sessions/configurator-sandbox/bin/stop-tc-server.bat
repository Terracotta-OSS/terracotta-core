@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

rem
rem This script is for use by the Session Configurator and not meant to be
rem executed manually.
rem

setlocal
cd %~d0%~p0..\..\..
set SANDBOX=%CD%\sessions\configurator-sandbox\%1
call "..\bin\stop-tc-server.bat" -f "%SANDBOX%\tc-config.xml"
exit %ERRORLEVEL%
endlocal
