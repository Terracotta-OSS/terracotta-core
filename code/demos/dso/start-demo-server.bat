@echo off

rem @COPYRIGHT@

IF NOT "x%1"=="x" GOTO invalid_args

cd "%~d0%~p0"
start "Terracotta Demo Server" "..\bin\start-tc-server.bat"

:invalid_args
	SET tc_tmp=%~p0start-demo-server.bat
	echo Usage:
	echo %tc_tmp%
