echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

SETLOCAL

REM -------------------------------------
REM - start.bat 908{1,2} [nodso]
REM -------------------------------------

cd %~d0%~p0..
SET SANDBOX=%CD%
SET TC_INSTALL_DIR=%SANDBOX%\..\..

SET JAVA_HOME=%TC_INSTALL_DIR%\jre
IF NOT "x%TC_JAVA_HOME%" == "x" (
  IF NOT EXIST "%TC_JAVA_HOME%" (
    ECHO TC_JAVA_HOME of '%TC_JAVA_HOME%' does not exist.
    EXIT 1
  )
  SET JAVA_HOME=%TC_JAVA_HOME%
)

IF "x%CATALINA_HOME%" == "x" (
  SET CATALINA_HOME=%TC_INSTALL_DIR%\vendors\tomcat5.5
  ECHO Using CATALINA_HOME of '%TC_INSTALL_DIR%\vendors\tomcat5.5'.
) else (
  IF NOT EXIST "%CATALINA_HOME%" (
    ECHO CATALINA_HOME of '%CATALINA_HOME%' does not exist.
    EXIT 1
  )
)

SET CATALINA_BASE=%SANDBOX%\tomcat5.5\%1

REM --------------------------------------------------------------------
REM - The Configurator passes 'nodso' as the second argument to this
REM - script if you've disabled DSO in its GUI...
REM --------------------------------------------------------------------

IF "%2" == "nodso" goto runCatalina

CALL "..\libexec\tc-functions.bat" tc_install_dir "%TC_INSTALL_DIR%"
CALL "..\libexec\tc-functions.bat" tc_set_dso_boot_jar

IF "%EXITFLAG%"=="TRUE" GOTO END

SET JAVA_OPTS=%JAVA_OPTS% -Xbootclasspath/p:"%DSO_BOOT_JAR%"
SET JAVA_OPTS=%JAVA_OPTS% -Dtc.install-root="%TC_INSTALL_DIR%"
SET JAVA_OPTS=%JAVA_OPTS% -Dtc.config="%SANDBOX%\tomcat5.5\tc-config.xml"
SET JAVA_OPTS=%JAVA_OPTS% -Dwebserver.log.name=%1
SET JAVA_OPTS=%JAVA_OPTS% -Dcom.sun.management.jmxremote
SET JAVA_OPTS=%JAVA_OPTS% -Dtc.node-name=tomcat-%1

:runCatalina

cd "%SANDBOX%"
CALL "%CATALINA_HOME%\bin\catalina.bat" run

:END
EXIT %ERRORLEVEL%
ENDLOCAL
