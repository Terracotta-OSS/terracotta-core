@echo off

rem
rem  All content copyright (c) 2003-2008 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

rem -------------------------------------
rem - start.bat 908{1,2} [nodso]
rem -------------------------------------

setlocal
cd %~d0%~p0..
set SANDBOX=%CD%
set SANDBOX="%SANDBOX:"=%"

set TC_INSTALL_DIR=%SANDBOX%\..\..\..

rem --------------------------------------------------------------------
rem - The Configurator passes 'nodso' as the second argument to this
rem - script if you've disabled DSO in its GUI...
rem --------------------------------------------------------------------

if "%2" == "nodso" goto runJetty

set TC_CONFIG_PATH=%SANDBOX%\jetty6.0\tc-config.xml
call %TC_INSTALL_DIR%\bin\dso-env.bat -q "%TC_CONFIG%"

if "%EXITFLAG%"=="TRUE" goto end

set OPTS=%TC_JAVA_OPTS% -Dwebserver.log.name=jetty-%1
set OPTS=%OPTS% -Dcom.sun.management.jmxremote
set OPTS=%OPTS% -Dproject.name=Configurator
set JAVA_OPTS=%OPTS% %JAVA_OPTS%

:runJetty

cd %SANDBOX%
set CLASSPATH=%CLASSPATH:"=%
set JAVA_HOME=%JAVA_HOME:"=%
set JETTY_CP=%TC_INSTALL_DIR\lib\jetty-6.1.8.jar;%TC_INSTALL_DIR%\lib\servlet-api-2.4.jar

call "%JAVA_HOME%\bin\java" -cp %JETTY_CP% org.mortbay.jetty.Server %1/conf.xml

:end
exit /b %ERRORLEVEL%
endlocal
