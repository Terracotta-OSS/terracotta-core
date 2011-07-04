@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

if not "x%1"=="x" goto show_usage

cd "%~d0%~p0"
start "Terracotta Demo Server" "..\..\bin\start-tc-server.bat"
goto :done

:show_usage
  set tc_tmp=%~p0start-demo-server.bat
  echo Usage:
  echo %tc_tmp%

:done

