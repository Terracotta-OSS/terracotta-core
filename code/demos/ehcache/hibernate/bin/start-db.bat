@echo off

setlocal

set root=%~d0%~p0..
set root="%root:"=%"

cd %root%
h2_jar=%root%\src\main\webapp\WEB-INF\lib\h2-1.1.116.jar

call %JAVA_HOME%\bin\java -cp $h2_jar org.h2.tools.Server -tcp -tcpAllowOthers

endlocal
