@echo off

if not defined JAVA_HOME (
  echo JAVA_HOME is not defined
  exit /b 1
)

setlocal

set JAVA_HOME="%JAVA_HOME:"=%"
set root=%~d0%~p0..
set root="%root:"=%"

cd %root%
set h2_jar=%root%\src\main\webapp\WEB-INF\lib\h2-1.1.116.jar

start /b "H2" %JAVA_HOME%\bin\java -cp %h2_jar% org.h2.tools.Server -tcp -tcpAllowOthers

endlocal
