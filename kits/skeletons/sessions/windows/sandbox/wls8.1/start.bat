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

IF "%TC_JAVA_HOME%" == "" (
  SET TC_JAVA_HOME=%BEA_HOME%\jdk142_11
)

IF NOT EXIST "%TC_JAVA_HOME%" (
  ECHO TC_JAVA_HOME '%TC_JAVA_HOME%' does not exist.
  EXIT 1
  ENDLOCAL  
)

set JAVA_HOME=%TC_JAVA_HOME%

"%JAVA_HOME%\bin\java" -classpath "%TC_INSTALL_DIR%\common\lib\tc.jar" com.tc.CheckForJava14
IF NOT ERRORLEVEL 0 (
  ECHO Weblogic Server 8.1 requires Java 1.4. Exiting.
  GOTO END
)

IF ""%2"" == ""nodso"" GOTO doRunWLS

CALL "..\libexec\tc-functions.bat" tc_install_dir "%TC_INSTALL_DIR%"
CALL "..\libexec\tc-functions.bat" tc_set_dso_boot_jar

IF "%EXITFLAG%"=="TRUE" GOTO END

SET JAVA_OPTIONS=%JAVA_OPTS% -Xbootclasspath/p:"%DSO_BOOT_JAR%"
SET JAVA_OPTIONS=%JAVA_OPTIONS% -Dtc.install-root="%TC_INSTALL_DIR%"
SET JAVA_OPTIONS=%JAVA_OPTIONS% -Dtc.config="%SANDBOX%\wls8.1\tc-config.xml"
SET JAVA_OPTIONS=%JAVA_OPTIONS% -Dwebserver.log.name=%1
SET JAVA_OPTIONS=%JAVA_OPTIONS% -Dcom.sun.management.jmxremote
SET JAVA_OPTS=%JAVA_OPTS% -Dtc.node-name=weblogic-%1

:doRunWLS
CD %~d0%~p0%1
DEL /Q SerializedSystemIni.dat
RMDIR /S /Q myserver
RMDIR /S /Q applications\.wlnotdelete
COPY tmpls\*.* .

CALL ..\startWLS.bat

:END
EXIT %ERRORLEVEL%
ENDLOCAL
