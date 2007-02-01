@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

if not "x%1"=="x" goto show_usage

cd "%~d0%~p0"
start "Terracotta Demo Server" "..\bin\start-tc-server.bat"
goto :done

:show_usage
  set tc_tmp=%~p0start-demo-server.bat
  echo Usage:
  echo %tc_tmp%

:done

