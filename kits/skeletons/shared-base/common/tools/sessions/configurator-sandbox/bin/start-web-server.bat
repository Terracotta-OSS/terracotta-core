@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

REM ----------------------------------------------------------
REM - start.bat {tomcat5.0|tomcat5.5|wls8.1} 908{1,2} [nodso]
REM ----------------------------------------------------------

setlocal
set EXIT_ON_ERROR=TRUE
call "%~d0%~p0..\%1\start.bat" %2 %3
exit %ERRORLEVEL%
endlocal
