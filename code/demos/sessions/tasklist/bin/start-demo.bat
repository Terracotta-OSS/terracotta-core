@echo off

setlocal

set root=%~d0%~p0..
set root="%root:"=%"

cd %root%
call bin\package.bat

call ..\bin\start-jetty.bat 9081
sleep 3
call ..\bin\start-jetty.bat 9082

endlocal
