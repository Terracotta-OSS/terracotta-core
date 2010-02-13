@echo off

setlocal

set root=%~d0%~p0..
set root="%root:"=%"

cd %root%
call bin\package.bat

call bin\start-jetty.bat 9081
echo http://localhost:9081/Cart
echo

sleep 3
call bin\start-jetty.bat 9082
echo http://localhost:9081/Cart
echo

endlocal
