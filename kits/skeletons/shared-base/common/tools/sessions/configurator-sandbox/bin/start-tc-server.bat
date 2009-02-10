@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

rem
rem This script is for use by the Session Configurator and not meant to be
rem executed manually.
rem

setlocal
cd %~d0%~p0..\..\..
set SANDBOX=%CD%\sessions\configurator-sandbox\%1
if not exist ..\lib\dso-boot mkdir ..\lib\dso-boot
call "..\bin\make-boot-jar.bat" -o ..\lib\dso-boot -f "%SANDBOX%\tc-config.xml"
if %ERRORLEVEL% neq 0 goto end
call "..\bin\start-tc-server.bat" -f "%SANDBOX%\tc-config.xml"

:end
exit %ERRORLEVEL%
endlocal
