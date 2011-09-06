@echo off

if not defined JAVA_HOME (
  echo JAVA_HOME is not defined
  exit /b 1
)

setlocal

set jetty_instance=%1

if  "%jetty_instance%" == "" ( 
  echo Need to specify which instance of Jetty: 9081 or 9082
  exit /b 1
)

set JAVA_HOME="%JAVA_HOME:"=%"
set root=%~d0%~p0..
set root="%root:"=%"
set jetty_work_dir=%root%\jetty6.1\%jetty_instance%
set jetty_home=%root%\..\..\..\third-party\jetty-6.1.15
set start_jar=%jetty_home%\start.jar
set /a stop_port=jetty_instance + 2

cd %jetty_work_dir%
echo Stopping Jetty %jetty_instance%...
%JAVA_HOME%\bin\java -Djetty.home=%jetty_home% -DSTOP.PORT=%stop_port% -DSTOP.KEY=secret -jar %start_jar% --stop 

endlocal
