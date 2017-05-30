@echo off
set SPM_DIR=%~d0%~p0..
set "SPM_DIR=%SPM_DIR:"=%"

set PID_FILE=%SPM_DIR%\instance\run.pid
set "PID_FILE=%PID_FILE:"=%"

set /p PIDTOKILL=< %PID_FILE%
rem Kill the server and remove the PID file
taskkill /F /PID %PIDTOKILL%>NUL 2>NUL

rem If taskkill returns with a non-zero status, i.e., server crashed at some point
rem we would still remove the PID file to be consistent
del %PID_FILE%