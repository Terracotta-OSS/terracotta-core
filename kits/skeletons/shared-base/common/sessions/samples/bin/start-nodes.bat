@echo off

setlocal

set root=%~d0%~p0..
set root="%root:"=%"

call %root%\bin\start-jetty.bat 9081
sleep 3
call %root%\bin\start-jetty.bat 9082

endlocal
