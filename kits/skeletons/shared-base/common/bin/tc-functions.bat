@echo off
:: ======================================================================
:: THIS FILE MUST BE SAVED USING DOS LF WESTERN LATIN 1 FORMATTING
:: (WINDOWS DEFAULT)
:: ======================================================================


::
rem @COPYRIGHT@
::
:: This file sets up various shell variables used by the other Terracotta
:: scripts.
::
:: Environment variables you can use to control Terracotta scripts' behavior:
::
::    TC_JAVA_HOME -- Uses an alternate JVM to run the Terracotta software;
::                    note that only the VM included in this kit is officially
::                    supported by Terracotta for running the server and
::                    database listener
::
::    TC_JAVA_OPTS -- Adds these Java VM options to the 'java' command line, after
::                    any options otherwise passed by the script
::
::    TC_ADDITIONAL_CLASSPATH -- Will add classes specified in this path to the
::                               end of the CLASSPATH used for the program
::
::    TC_VERBOSE_SCRIPTS -- If set to any non-empty value, will cause all commands
::                          to be echoed to the screen before being run
::


:: ======================================================================
:: Implementation follows
:: ======================================================================

:: Fake Unix Shell functions
IF "x"%EXITFLAG%=="x" SET EXITFLAG=FALSE
IF %EXITFLAG%==TRUE GOTO END
SET SUBROUTINE=%1
GOTO %SUBROUTINE%

::
:: Usage:
::    tc_install_dir <tc-install-dir> [true]
::
:: The first parameter must be the path to the Terracotta installation directory;
:: the second is optional, but must be 'true' if you are using DSO.
::
:: Prerequisites:
::    None.
::
:: Input:
::    TC_INSTALL_DIR -- Overrides the location of the TC installation directory
::
:: Sets:
::
::    TC_INSTALL_DIR -- location of the Terracotta installation directory; this
::                should only be used by other parts of this script
::    TC_USING_DSO -- The value of the second argument
::
:: Also see :tc_find_java, below.
:: Also see :tc_dso_setup, below, if you're using DSO.
::
:tc_install_dir

   SET __DEFAULT_TC_INSTALL_DIR=%2
   SET __DEFAULT_TC_INSTALL_DIR=%__DEFAULT_TC_INSTALL_DIR:"=%
   SET __USING_DSO=%3
   IF "x"%__USING_DSO%=="x" SET __USING_DSO=FALSE

   IF "x%TC_INSTALL_DIR%"=="x" GOTO tc_install_dir__1
   GOTO tc_install_dir__2

   :tc_install_dir__1
      SET TC_INSTALL_DIR=%__DEFAULT_TC_INSTALL_DIR%
      IF NOT EXIST %TC_INSTALL_DIR% GOTO tc_install_dir__1_1
      IF NOT EXIST %TC_INSTALL_DIR%\lib\tc.jar GOTO tc_install_dir__1_2

      PUSHD %TC_INSTALL_DIR%
      SET TC_INSTALL_DIR=%CD%
      POPD
      GOTO tc_install_dir__3

      :tc_install_dir__1_1
         echo The root of the Terracotta installation, %TC_INSTALL_DIR% doesn't seem to exist. Stop.
         SET EXITFLAG=TRUE
         GOTO END

      :tc_install_dir__1_2
         echo The Terracotta installation directory, %TC_INSTALL_DIR%, doesn't contain lib/tc.jar. Stop.
         SET EXITFLAG=TRUE
         GOTO END

   :tc_install_dir__2
      IF NOT EXIST "%TC_INSTALL_DIR%" GOTO tc_install_dir__2_1
      IF NOT EXIST "%TC_INSTALL_DIR%/lib/tc.jar" GOTO tc_install_dir__2_2
      GOTO tc_install_dir__2_3

      :tc_install_dir__2_1
         echo The Terracotta installation directory you specified via the
         echo environment variable TC_INSTALL_DIR, %TC_INSTALL_DIR%,"
         echo doesn't actually exist. Stop.
         SET EXITFLAG=TRUE
         GOTO END

      :tc_install_dir__2_2
         echo The Terracotta installation directory you specified via the
         echo environment variable TC_INSTALL_DIR, %TC_INSTALL_DIR%,"
         echo doesn't seem to contain lib/tc.jar. We are proceeding,
         echo but this could cause problems.
         GOTO tc_install_dir__3

      :tc_install_dir__2_3
         PUSHD %TC_INSTALL_DIR%
         SET TC_INSTALL_DIR=%CD%
         POPD

         IF NOT "x%TC_VERBOSE_SCRIPTS%"=="x" echo TC_INSTALL_DIR: %TC_INSTALL_DIR%
         GOTO tc_install_dir__3

   :tc_install_dir__3
      call "%~f0" tc_find_java
      IF %__USING_DSO%==TRUE CALL "%~f0" tc_dso_setup
      SET TC_USING_DSO=%__USING_DSO%

GOTO END

::
:: Usage:
::    tc_dso_setup
::
:: Prerequisites:
::    Should only be called from :tc_install_dir directly.
::
:: Input:
::    DSO_JAVA -- Overrides the location of the 'dso-java' script
::    DSO_BOOT_JAR -- Overrides the location of the DSO boot JAR
::
:: Output:
::    DSO_JAVA -- The location of the 'dso-java' script
::    DSO_BOOT_JAR -- The location of the DSO boot JAR
::
:tc_dso_setup

   IF "x%DSO_JAVA%"=="x" GOTO END

   IF NOT EXIST "%DSO_JAVA%" GOTO tc_dso_setup__1_1
   GOTO tc_dso_setup__1_2

   :tc_dso_setup__1_1
      echo Your custom dso-java command, as specified in the DSO_JAVA
      echo environment variable, %DSO_JAVA%, does not exist
      echo or is not executable. Stop.
      SET EXITFLAG=TRUE
      GOTO END

   :tc_dso_setup__1_2
      echo DSO_JAVA: %DSO_JAVA%
      SET DSO_JAVA_IS_CUSTOM=TRUE
      GOTO END

GOTO END

::
:: Usage:
::    tc_classpath <default-application-classpath> (true|false)
::
:: The first argument is the CLASSPATH that the application needs for its own
:: classes; the second argument is whether to include the Terracotta classes
:: in the CLASSPATH or not.
::
:: The CLASSPATH in the first argument should *always* be in colon notation
:: (i.e., using ':' as the separator between components); this script will take
:: care of replacing the colons with semicolons if required (for cygwin). Note
:: that this does not apply to the 'input' variables, below, as they will be
:: expected to be in whatever notation is correct for the platform in question.
::
:: Prerequisites:
::    ':tc_install_dir' must be called first.
::
:: Input:
::    TC_CLASSPATH -- Overrides the ENTIRE class path for this application
::    TC_ADDITIONAL_CLASSPATH -- Added to the end of the class path, if present;
::                               ignored if TC_CLASSPATH is set
::    TC_JAR_CLASSES -- Uses the classes in this path instead of tc.jar
::                      (only if second argument is 'true')
::
:: Output:
::    TC_CLASSPATH -- The CLASSPATH this application should use, in its entirety
::    TC_CLASSPATH_WITH_TC_JAR -- The CLASSPATH this application should use,
::                                in its entirety, but always with the 'tc.jar'
::                                (or equivalent)
::    TC_CLASSPATH_WITHOUT_TC_JAR -- The CLASSPATH this application should use,
::                                   in its entirety, but always without the
::                                   'tc.jar' (or equivalent)
::
:tc_classpath

   SET __DEFAULT_TC_APPLICATION_CLASSPATH=%2
   SET __DEFAULT_TC_APPLICATION_CLASSPATH=%__DEFAULT_TC_APPLICATION_CLASSPATH:"=%
   SET __INCLUDE_TC_JAR=%3

   IF "x%TC_INSTALL_DIR%"=="x" GOTO tc_classpath__1
   IF "x%TC_CLASSPATH%"=="x" GOTO tc_classpath__2
   GOTO tc_classpath__3

   :tc_classpath__1
      echo Error: You must call tc_install_dir before calling this function. Stop.
      SET EXITFLAG=TRUE
      GOTO END

   :tc_classpath__2
      IF NOT "x%TC_JAR_CLASSES%"=="x" GOTO tc_classpath__2_1
      IF NOT EXIST "%TC_INSTALL_DIR%\lib\tc.jar" GOTO tc_classpath__2_2
      SET TC_JAR_CLASSES=%TC_INSTALL_DIR%\lib\tc.jar
      GOTO tc_classpath__2_3

      :tc_classpath__2_1
         IF NOT "x%TC_VERBOSE_SCRIPTS%"=="x" echo TC_JAR_CLASSES: %TC_JAR_CLASSES%
         GOTO :tc_classpath__2_3

      :tc_classpath__2_2
         echo There is no 'tc.jar' in TC_INSTALL_DIR/lib/tc.jar
         echo (%TC_INSTALL_DIR%/lib/tc.jar). Your Terracotta
         echo installation directory may be specified incorrectly or corrupt.
         echo (You may also specify a location for tc.jar manually, in the
         echo TC_JAR_CLASSES environment variable.) Stop.
         SET EXITFLAG=TRUE
         GOTO END

      :tc_classpath__2_3
         IF "x%__DEFAULT_TC_APPLICATION_CLASSPATH%"=="x" GOTO tc_classpath__2_3_1
         SET TC_CLASSPATH_WITH_TC_JAR=%__DEFAULT_TC_APPLICATION_CLASSPATH%;%TC_JAR_CLASSES%
         SET TC_CLASSPATH_WITHOUT_TC_JAR=%__DEFAULT_TC_APPLICATION_CLASSPATH%
         GOTO tc_classpath__2_3_2

      :tc_classpath__2_3_1
         SET TC_CLASSPATH_WITH_TC_JAR=%TC_JAR_CLASSES%
         SET TC_CLASSPATH_WITHOUT_TC_JAR=

      :tc_classpath__2_3_2
         IF NOT "x%TC_ADDITIONAL_CLASSPATH%"=="x" GOTO tc_classpath__2_3_3
         IF %__INCLUDE_TC_JAR%==TRUE GOTO tc_classpath__2_3_4
         SET TC_CLASSPATH=%TC_CLASSPATH_WITHOUT_TC_JAR%
         GOTO END

         :tc_classpath__2_3_3
            IF NOT "x%TC_VERBOSE_SCRIPTS%"=="x" echo TC_ADDITIONAL_CLASSPATH: %TC_ADDITIONAL_CLASSPATH%

            SET TC_CLASSPATH_WITH_TC_JAR="%TC_CLASSPATH_WITH_TC_JAR%;%TC_ADDITIONAL_CLASSPATH%
            SET TC_CLASSPATH_WITHOUT_TC_JAR=%TC_CLASSPATH_WITHOUT_TC_JAR%;%TC_ADDITIONAL_CLASSPATH%
            GOTO END

         :tc_classpath__2_3_4
            SET TC_CLASSPATH=%TC_CLASSPATH_WITH_TC_JAR%
            GOTO END

   :tc_classpath__3
      IF NOT "x%TC_VERBOSE_SCRIPTS%"=="x" echo TC_CLASSPATH: %TC_CLASSPATH%

GOTO END

::
:: Usage:
::    tc_find_java
::
:: Prerequisites:
::    Should only be called from the 'tc_install_dir' script.
::
:: Input:
::    TC_JAVA_HOME -- Overrides the Java VM that's used to run the application
::
:: Output:
::    TC_JAVA -- The path to the 'java' executable that we should use to run the application
::
:tc_find_java

   IF "x%TC_JAVA_HOME%"=="x" GOTO tc_find_java__1
   GOTO tc_find_java__2

   :tc_find_java__1
      SET TC_JAVA=%TC_INSTALL_DIR%\jre\bin\java.exe
      IF NOT EXIST "%TC_JAVA%" GOTO tc_find_java__1_1
      SET TC_JAVA_HOME=%TC_INSTALL_DIR%\jre
      IF NOT "x%TC_VERBOSE_SCRIPTS%"=="x" echo TC_JAVA_HOME: %TC_JAVA_HOME%
      GOTO END

      :tc_find_java__1_1
         echo You haven't specified a JRE installation to use
         echo (via the TC_JAVA_HOME environment variable), and
         echo there is no JRE included in this Terracotta kit. Stop.
         SET EXITFLAG=TRUE
         GOTO END

   :tc_find_java__2
      SET TC_TMP=FALSE
      IF NOT EXIST "%TC_JAVA_HOME%" SET TC_TMP=TRUE
      IF NOT EXIST "%TC_JAVA_HOME%\bin\java.exe" SET TC_TMP=TRUE
      IF %TC_TMP%==TRUE GOTO tc_find_java__2_1

      SET TC_JAVA=%TC_JAVA_HOME%\bin\java
      IF NOT "x%TC_VERBOSE_SCRIPTS%"=="x" echo TC_JAVA_HOME: %TC_JAVA_HOME%
      rem IF NOT "x%TC_VERBOSE_SCRIPTS%"=="x" "%TC_JAVA%" -version
      GOTO END

      :tc_find_java__2_1
         echo The JRE you specified in TC_JAVA_HOME, %TC_JAVA_HOME%,
         echo does not appear to be a directory with a valid 'java' command
         echo (at %TC_JAVA_HOME%\bin\java.exe). Stop.
         SET EXITFLAG=TRUE
         GOTO END

GOTO END

::
:: Usage:
::    tc_java_opts <default JVM options>
::
:: Prerequisites:
::    None.
::
:: Input:
::    TC_JAVA_OPTS -- Adds additional options to the Java command line
::    TC_ALL_JAVA_OPTS -- Replaces ALL Java command-line options; use carefully
::
:: Output:
::    TC_ALL_JAVA_OPTS -- The entire set of JVM options the application should run with
::
:tc_java_opts

   SET CMD=%*
   SET __DEFAULT_TC_JAVA_OPTS=%CMD:tc_java_opts=%

   IF NOT "x%TC_JAVA_OPTS%"=="x" GOTO tc_java_opts__1
   SET TC_ALL_JAVA_OPTS=%__DEFAULT_TC_JAVA_OPTS%
   GOTO END

   :tc_java_opts__1
      IF NOT "x%TC_VERBOSE_SCRIPTS%"=="x" echo TC_JAVA_OPTS: %TC_JAVA_OPTS%
      SET TC_ALL_JAVA_OPTS=%__DEFAULT_TC_JAVA_OPTS% %TC_JAVA_OPTS%
      GOTO END

GOTO END

::
:: Usage:
::    tc_config <default TC config>
::
:: Prerequisites:
::    None.
::
:: Input:
::    TC_CONFIG -- Overrides the Terracotta configuration file to be used by this application
::
:: Output:
::    D_TC_CONFIG -- The entire JVM option that defines the path to the Terracotta configuration
::
:tc_config

   SET __DEFAULT_TC_CONFIG=%2

   IF NOT "x%TC_CONFIG%"=="x" GOTO tc_config__1
   SET   D_TC_CONFIG=-Dtc.config=%__DEFAULT_TC_CONFIG%
   GOTO END

   :tc_config__1
      IF NOT "x%TC_VERBOSE_SCRIPTS%"=="x" echo TC_CONFIG: %TC_CONFIG%
      SET D_TC_CONFIG=-Dtc.config=%TC_CONFIG%
      GOTO END

GOTO END

:tc_set_dso_boot_jar

   :tc_set_dso_boot_jar__1
      IF NOT "x%DSO_BOOT_JAR%"=="x" GOTO tc_set_dso_boot_jar__1_1
      GOTO tc_set_dso_boot_jar__1_2

      :tc_set_dso_boot_jar__1_1
         IF NOT EXIST "%DSO_BOOT_JAR%" GOTO tc_set_dso_boot_jar__1_1_1
         GOTO END

         :tc_set_dso_boot_jar__1_1_1
            echo The custom DSO boot JAR you specified in the DSO_BOOT_JAR
            echo environment variable, %DSO_BOOT_JAR%, does not exist. Stop.
            SET EXITFLAG=TRUE
            GOTO END

      :tc_set_dso_boot_jar__1_2
         IF NOT DEFINED TMPFILE SET TMPFILE=%TEMP%\var~
         "%TC_JAVA%" -classpath "%TC_INSTALL_DIR%\lib\tc.jar" com.tc.object.tools.BootJarSignature >%TMPFILE%
         FOR /F %%i IN (%TMPFILE%) DO @SET DSO_BOOT_JAR_NAME=%%i
         DEL %TMPFILE%
         SET __FIND_DSO_BOOT_JAR_CMD_EXIT_CODE=%errorlevel%
         IF NOT ERRORLEVEL==0 GOTO tc_set_dso_boot_jar__1_2_1
         GOTO tc_set_dso_boot_jar__1_2_2

         :tc_set_dso_boot_jar__1_2_1
            echo We were unable to determine the correct
            echo name of the DSO boot JAR; we ran the tool do to that as follows:
            echo %TC_JAVA% -classpath "%TC_INSTALL_DIR%\lib\tc.jar" com.tc.object.tools.BootJarSignature
            echo ...but we got exit code %__FIND_DSO_BOOT_JAR_CMD_EXIT_CODE%. Stop.
            SET EXITFLAG=TRUE
            GOTO END

         :tc_set_dso_boot_jar__1_2_2
            SET DSO_BOOT_JAR=%TC_INSTALL_DIR%\lib\\dso-boot\%DSO_BOOT_JAR_NAME%

            IF NOT EXIST "%DSO_BOOT_JAR%" GOTO tc_set_dso_boot_jar__1_2_2_1
            GOTO END

            :tc_set_dso_boot_jar__1_2_2_1
               echo The DSO boot JAR you need for this platform,
               echo %DSO_BOOT_JAR%,
               echo does not exist. You may need to run the 'make-boot-jar' script,
               echo or consult the Terracotta documentation for more information.
               SET EXITFLAG=TRUE
               GOTO END

GOTO END


::
:: Usage:
::    tc_java ...command-line options...
::
:: Prerequisites:
::    'tc_java_opts' must be called first.
::
:: Input:
::    TC_VERBOSE_SCRIPTS -- If set to any nonempty value, will echo the command
::                          to be executed just before it runs
::
:: Output:
::    None; runs ${TC_JAVA} with the specified options.
::
:tc_java
   SET CMD=%*
   SET TMPVAR=%CMD:tc_java=%
   IF NOT "x%TC_VERBOSE_SCRIPTS%"=="x" echo Executing: "%TC_JAVA%" %TMPVAR%
   "%TC_JAVA%" %TMPVAR%

GOTO END

::
:: Usage:
::    run_dso_java ...command-line options...
::
:: Prerequisites:
::    'tc_install_dir' must be called first, with 'true' as the second argument.
::    'tc_java_opts' must be called first.
::
:: Input:
::    TC_VERBOSE_SCRIPTS -- If set to any nonempty value, will echo the command
::                          to be executed just before it runs
::
:: Output:
::    None; runs ${DSO_JAVA} with the specified options.
::
:: Note:
::    This should be used ONLY for Terracotta scripts that need to run the
::    equivalent of 'dso-java'. The problem is that if you use the 'dso-java'
::    script itself, all the initialization and setup will get done twice, which
::    you don't want. Instead, use this, and you'll be set.
::
:run_dso_java

   SET CMD=%*
   SET TMPARGS=%CMD:run_dso_java=%
   IF "x%TC_INSTALL_DIR%"=="x" GOTO run_dso_java__1
   CALL "%~f0" tc_set_dso_boot_jar
   IF NOT "x%DSO_JAVA_IS_CUSTOM%"=="x" GOTO run_dso_java__2
   CALL "%~f0" tc_java "-Xbootclasspath/p:%DSO_BOOT_JAR%" "-Dtc.install-root=%TC_INSTALL_DIR%" %TMPARGS%
   GOTO END

   :run_dso_java__2
      "%DSO_JAVA%" %TMPARGS%
      GOTO END

   :run_dso_java__1
      echo Error: You must call 'tc_install_dir' first, with 'true' as
      echo the second argument.
      SET EXITFLAG=TRUE
      GOTO END

GOTO END

:: ======================================================================
:: End of batch file
:: ======================================================================
:END

if NOT "%EXITFLAG%"=="TRUE" GOTO RETURN
if NOT "%EXIT_ON_ERROR%"=="TRUE" GOTO RETURN
EXIT 1

:RETURN

::
:: UNSUPPORTED environment variables -- these will probably work, but may result
:: in software that doesn't work correctly, and is definitely not supported by
:: Terracotta:
::
::    TC_INSTALL_DIR -- Overrides the root of your Terracotta installation
::
::    DSO_BOOT_JAR -- Overrides the DSO boot JAR to use
::
::    DSO_JAVA -- Overrides the 'dso-java' script to use
::
::    TC_JAR_CLASSES -- Uses the classes in this CLASSPATH variable instead of
::                      'tc.jar' (use with ext::e caution)
::
::    TC_CLASSPATH -- Overrides the ENTIRE class path for the application
::                    (use with ext::e caution)
::
::    TC_ALL_JAVA_OPTS -- Replaces ALL Java VM options with the ones specified
::                        in this variable (use with caution)
::
::    TC_CONFIG -- Overrides the Terracotta configuration file that is used by
::                 the application with the one specified here
::                 (use with ext::e caution)
::
