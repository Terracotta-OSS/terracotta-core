@echo off

rem
rem  All content copyright (c) 2003-2008 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

setlocal
cd %~d0%~p0
set TC_HOME=..\..\..
set JETTY_HOME=%TC_HOME%\vendors\jetty-6.1.15
set JAVA_OPTS=%JAVA_OPTS% -Djetty.home=%JETTY_HOME%
set JAVA_OPTS=%JAVA_OPTS% -DSTOP.PORT=8182
set JAVA_OPTS=%JAVA_OPTS% -DSTOP.KEY=secret
set JAVA_HOME="%JAVA_HOME:"=%"
%JAVA_HOME%\bin\java %JAVA_OPTS% -jar %JETTY_HOME%\start.jar --stop
endlocal
