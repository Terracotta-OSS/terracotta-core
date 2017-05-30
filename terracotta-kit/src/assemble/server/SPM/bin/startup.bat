@echo off
set TC_SERVER_DIR=%~d0%~p0..\..
set TC_SERVER_DIR="%TC_SERVER_DIR:"=%"

set SPM_DIR=%~d0%~p0..
set "SPM_DIR=%SPM_DIR:"=%"

set PID_FILE=%SPM_DIR%\instance\run.pid
set "PID_FILE=%PID_FILE:"=%"

call %TC_SERVER_DIR%\bin\start-tc-server.bat -f %SPM_DIR%\conf\tc-config.xml

timeout /t 5 /nobreak > NUL
setlocal
REM remove pid file if server startup failed
set /p PID=< %PID_FILE%
tasklist /FI "PID eq %PID%" 2>NUL | find "%PID%">NUL
if %ERRORLEVEL% NEQ 0 (
	del %PID_FILE%
)
endlocal