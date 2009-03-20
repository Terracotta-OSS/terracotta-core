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
set JAVA_OPTS=%JAVA_OPTS% -Dcom.sun.management.jmxremote
set JAVA_OPTS=%JAVA_OPTS% -Dcom.sun.management.jmxremote.port=8091
set JAVA_OPTS=%JAVA_OPTS% -Dcom.sun.management.jmxremote.authenticate=false
set JAVA_OPTS=%JAVA_OPTS% -Dcom.sun.management.jmxremote.ssl=false
set JAVA_OPTS=%JAVA_OPTS% -Dtc.node-name=Node1
set JAVA_OPTS=%JAVA_OPTS% -Djetty.home=%JETTY_HOME%
set JAVA_OPTS=%JAVA_OPTS% -Djetty.home=%JETTY_HOME%\lib\terracotta\jetty-terracotta-sessions-6.1.15.jar
set JAVA_OPTS=%JAVA_OPTS% -Djetty.port=8081
set JAVA_OPTS=%JAVA_OPTS% -DSTOP.PORT=8181
set JAVA_OPTS=%JAVA_OPTS% -DSTOP.KEY=secret
start "terracotta for spring: webflow sample: 8081" %TC_HOME%\bin\dso-java.bat %JAVA_OPTS% -jar %JETTY_HOME%\start.jar jetty-conf.xml
endlocal
