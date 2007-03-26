@echo off
:: ======================================================================
:: THIS FILE MUST BE SAVED USING DOS LF WESTERN LATIN 1 FORMATTING
:: (WINDOWS DEFAULT)
:: ======================================================================

::
rem
rem All content copyright (c) 2003-2007 Terracotta, Inc.,
rem except as may otherwise be noted in a separate copyright notice.
rem All rights reserved.
rem
::
:: Helper script that sets DSO_BOOT_JAR location, creating if necessary
:: Used by the dso-env script.
::
:: JAVA_HOME -- JVM to use when checking for/creating the bootjar
:: TC_INSTALL_DIR -- root of Terracotta installation
:: TC_CONFIG_PATH -- [optional] config file to use when creating bootjar
:: DSO_BOOT_JAR -- [optional] path to DSO bootjar; will be created iff it
::                  doesn't exist; if unspecified, is set to default VM-
::                  specific location under %TC_INSTALL_DIR%\lib\dso-boot.
::

if not exist "%JAVA_HOME%" set JAVA_HOME=%TC_INSTALL_DIR%\jre
set JAVACMD=%JAVA_HOME%\bin\java

set TC_JAR=%TC_INSTALL_DIR%\lib\tc.jar

if not "x%DSO_BOOT_JAR%"=="x" goto tc_set_dso_boot_jar__1_1
goto tc_set_dso_boot_jar__1_2

 :tc_set_dso_boot_jar__1_1
   if not exist "%DSO_BOOT_JAR%" goto tc_set_dso_boot_jar__1_3
   goto return

 :tc_set_dso_boot_jar__1_2
   if not defined TMPFILE set TMPFILE=%TEMP%\var~
   "%JAVACMD%" -cp "%TC_JAR%" com.tc.object.tools.BootJarSignature >%TMPFILE%
   for /F %%i in (%TMPFILE%) do @set DSO_BOOT_JAR_NAME=%%i
   del %TMPFILE%
   set __BOOT_JAR_SIG_EXIT_CODE=%errorlevel%
   if %ERRORLEVEL% NEQ 0 goto tc_set_dso_boot_jar__1_2_1
   goto tc_set_dso_boot_jar__1_2_2

   :tc_set_dso_boot_jar__1_2_1
     echo We were unable to determine the correct
     echo name of the DSO boot JAR using the following command:
     echo %JAVACMD% -cp "%TC_JAR%" com.tc.object.tools.BootJarSignature
     echo ...but we got exit code %__BOOT_JAR_SIG_EXIT_CODE%. Stop.
     goto error

   :tc_set_dso_boot_jar__1_2_2
     set DSO_BOOT_JAR=%TC_INSTALL_DIR%\lib\dso-boot\%DSO_BOOT_JAR_NAME%
     if not exist "%DSO_BOOT_JAR%" goto tc_set_dso_boot_jar__1_3
     goto return

 :tc_set_dso_boot_jar__1_3
   if not defined TC_CONFIG_PATH goto tc_set_dso_boot_jar__1_3_1
   "%JAVACMD%" -Dtc.install-root="%TC_INSTALL_DIR%" -cp "%TC_JAR%" com.tc.object.tools.BootJarTool -o "%DSO_BOOT_JAR%" -f "%TC_CONFIG_PATH%"
    if %ERRORLEVEL% NEQ 0 goto error
    goto return

   :tc_set_dso_boot_jar__1_3_1
    "%JAVACMD%" -Dtc.install-root="%TC_INSTALL_DIR%" -cp "%TC_JAR%" com.tc.object.tools.BootJarTool -o "%DSO_BOOT_JAR%"
    if %ERRORLEVEL% NEQ 0 goto error
    goto return

:error
exit %ERRORLEVEL%

:return
