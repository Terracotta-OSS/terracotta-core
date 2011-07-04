@echo off

setlocal

set root=%~d0%~p0..
set root="%root:"=%"

cd %root%

call bin/stop-jetty.bat 9081
call bin/stop-jetty.bat 9082

endlocal