@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

setlocal
cd %~d0%~p0..\..\..
set EXIT_ON_ERROR=TRUE
set SANDBOX=%CD%\sessions\configurator-sandbox\%1
call "..\bin\make-boot-jar.bat" -o ..\lib\dso-boot -f "%SANDBOX%\tc-config.xml"
call "..\bin\start-tc-server.bat" -f "%SANDBOX%\tc-config.xml"
exit %ERRORLEVEL%
endlocal
