#!/bin/sh

#
#@COPYRIGHT@
#
# This file sets up various shell variables used by the other Terracotta
# scripts.
#
# Environment variables you can use to control Terracotta scripts' behavior:
#
#    TC_JAVA_HOME -- Uses an alternate JVM to run the Terracotta software;
#                    note that only the VM included in this kit is officially
#                    supported by Terracotta for running the server and
#                    database listener
#
#    TC_JAVA_OPTS -- Adds these Java VM options to the 'java' command line, after
#                    any options otherwise passed by the script
#
#    TC_ADDITIONAL_CLASSPATH -- Will add classes specified in this path to the
#                               end of the CLASSPATH used for the program
#
#    TC_VERBOSE_SCRIPTS -- If set to any non-empty value, will cause all commands
#                          to be echoed to the screen before being run
#


# ======================================================================
# Implementation follows
# ======================================================================

#
# Usage:
#    tc_install_dir <tc-install-dir> [true]
#
# The first parameter must be the path to the Terracotta installation directory;
# the second is optional, but must be 'true' if you are using DSO.
#
# Prerequisites:
#    None.
#
# Input:
#    TC_INSTALL_DIR -- Overrides the location of the TC installation directory
#
# Sets:
#
#    TC_INSTALL_DIR -- location of the Terracotta installation directory; this
#                 should only be used by other parts of this script
#    TC_USING_DSO -- The value of the second argument
#
# Also see tc_find_java(), below.
# Also see tc_dso_setup(), below, if you're using DSO.
#
tc_install_dir() {
   __DEFAULT_TC_INSTALL_DIR="$1"
   __USING_DSO="$2"

   if test -z "${TC_INSTALL_DIR}"; then
      TC_INSTALL_DIR="$__DEFAULT_TC_INSTALL_DIR"

      if test \! -d "${TC_INSTALL_DIR}"; then
         echo "$0: The root of the Terracotta installation, ${TC_INSTALL_DIR},"
         echo "       doesn't seem to exist. Stop."

         exit 2
      fi

      if test \! -f "${TC_INSTALL_DIR}/common/lib/tc.jar"; then
         echo "$0: The Terracotta installation directory, ${TC_INSTALL_DIR},"
         echo "       doesn't seem to contain common/lib/tc.jar. Stop."

         exit 3
      fi

      curDir=`pwd`
      cd "${TC_INSTALL_DIR}"
      TC_INSTALL_DIR=`pwd`
      cd $curDir
   else
      if test \! -d "${TC_INSTALL_DIR}"; then
         echo "$0: The Terracotta installation directory you specified via the "
         echo "       environment variable TC_INSTALL_DIR, ${TC_INSTALL_DIR},"
         echo "       doesn't actually exist. Stop."

         exit 4
      elif test \! -f "${TC_INSTALL_DIR}/common/lib/tc.jar"; then
         echo "$0: The Terracotta installation directory you specified via the "
         echo "       environment variable TC_INSTALL_DIR, ${TC_INSTALL_DIR},"
         echo "       doesn't seem to contain common/lib/tc.jar. We are proceeding, "
         echo "       but this could cause problems."
      else
         curDir=`pwd`
         cd "${TC_INSTALL_DIR}" 
         TC_INSTALL_DIR=`pwd`
         cd $curDir 

         if test -n "$TC_VERBOSE_SCRIPTS"; then
           echo "$0: TC_INSTALL_DIR: ${TC_INSTALL_DIR}"
         fi
      fi
   fi

   tc_find_java

   if test "$__USING_DSO" = "true"; then
      tc_dso_setup
   fi

   TC_USING_DSO="$__USING_DSO"
}

#
# Usage:
#    tc_dso_setup
#
# Prerequisites:
#    Should only be called from tc_install_dir() directly.
#
# Input:
#    DSO_JAVA -- Overrides the location of the 'dso-java' script
#    DSO_BOOT_JAR -- Overrides the location of the DSO boot JAR
#
# Output:
#    DSO_JAVA -- The location of the 'dso-java' script
#    DSO_JAVA_IS_CUSTOM -- 'true' if and only if the user overrode DSO_JAVA
#    DSO_BOOT_JAR -- The location of the DSO boot JAR
#
tc_dso_setup() {
   if test -n "$DSO_JAVA"; then
      if test \! -x "${DSO_JAVA}"; then
         echo "$0: Your custom dso-java command, as specified in the DSO_JAVA "
         echo "       environment variable, ${DSO_JAVA}, does not exist "
         echo "       or is not executable. Stop."

         exit 5
      else
         echo "$0: DSO_JAVA: ${DSO_JAVA}"

         DSO_JAVA_IS_CUSTOM="true"
      fi
   fi
}

#
# Usage:
#    tc_classpath <default-application-classpath> (true|false)
#
# The first argument is the CLASSPATH that the application needs for its own
# classes; the second argument is whether to include the Terracotta classes
# in the CLASSPATH or not.
#
# The CLASSPATH in the first argument should *always* be in colon notation
# (i.e., using ':' as the separator between components); this script will take
# care of replacing the colons with semicolons if required (for cygwin). Note
# that this does not apply to the 'input' variables, below, as they will be
# expected to be in whatever notation is correct for the platform in question.
#
# Prerequisites:
#    'tc_install_dir' must be called first.
#
# Input:
#    TC_CLASSPATH -- Overrides the ENTIRE class path for this application
#    TC_ADDITIONAL_CLASSPATH -- Added to the end of the class path, if present;
#                               ignored if TC_CLASSPATH is set
#    TC_JAR_CLASSES -- Uses the classes in this path instead of tc.jar
#                      (only if second argument is 'true')
#
# Output:
#    TC_CLASSPATH -- The CLASSPATH this application should use, in its entirety
#    TC_CLASSPATH_WITH_TC_JAR -- The CLASSPATH this application should use,
#                                in its entirety, but always with the 'tc.jar'
#                                (or equivalent)
#    TC_CLASSPATH_WITHOUT_TC_JAR -- The CLASSPATH this application should use,
#                                   in its entirety, but always without the
#                                   'tc.jar' (or equivalent)
#
tc_classpath() {
   __DEFAULT_TC_APPLICATION_CLASSPATH="$1"
   __INCLUDE_TC_JAR="$2"

   if test -z "${TC_INSTALL_DIR}"; then
      echo "$0: Error: You must call tc_install_dir before calling this function. Stop."
      exit 8
   fi

   if test -n "`uname -s | grep -i cygwin || uname -s | grep -i windows`"; then
      __TC_PATHSEP=";"
      __DEFAULT_TC_APPLICATION_CLASSPATH="`echo ${__DEFAULT_TC_APPLICATION_CLASSPATH} | sed 's/:/;/g'`"
   else
      __TC_PATHSEP=":"
   fi

   if test -z "${TC_CLASSPATH}"; then
      if test -n "${TC_JAR_CLASSES}"; then
         echo "$0: Using a custom location for the classes in 'tc.jar', as specified in the "
         echo "       TC_JAR_CLASSES environment variable: "
         echo ""
         echo "       ${TC_JAR_CLASSES}"
         echo ""
      else
         if test \! -f "${TC_INSTALL_DIR}/common/lib/tc.jar"; then
            echo "$0: There is no 'tc.jar' in TC_INSTALL_DIR/common/lib/tc.jar "
            echo "       (${TC_INSTALL_DIR}/common/lib/tc.jar). Your Terracotta "
            echo "       installation directory may be specified incorrectly or corrupt."
            echo "       (You may also specify a location for tc.jar manually, in the "
            echo "       TC_JAR_CLASSES environment variable.) Stop."

            exit 9
         fi

         TC_JAR_CLASSES="${TC_INSTALL_DIR}/common/lib/tc.jar"
      fi

      if test -z "${__DEFAULT_TC_APPLICATION_CLASSPATH}"; then
         TC_CLASSPATH_WITH_TC_JAR="${TC_JAR_CLASSES}"
         TC_CLASSPATH_WITHOUT_TC_JAR=""
      else
         TC_CLASSPATH_WITH_TC_JAR="${__DEFAULT_TC_APPLICATION_CLASSPATH}${__TC_PATHSEP}${TC_JAR_CLASSES}"
         TC_CLASSPATH_WITHOUT_TC_JAR="${__DEFAULT_TC_APPLICATION_CLASSPATH}"
      fi

      if test -n "${TC_ADDITIONAL_CLASSPATH}"; then
         echo "$0: Appending the additional CLASSPATH you specified in the "
         echo "       TC_ADDITIONAL_CLASSPATH environment variable to the end of the CLASSPATH "
         echo "       used to run this application. Additional CLASSPATH is:"
         echo ""
         echo "       ${TC_ADDITIONAL_CLASSPATH}"

         TC_CLASSPATH_WITH_TC_JAR="${TC_CLASSPATH_WITH_TC_JAR}${__TC_PATHSEP}${TC_ADDITIONAL_CLASSPATH}"
         TC_CLASSPATH_WITHOUT_TC_JAR="${TC_CLASSPATH_WITHOUT_TC_JAR}${__TC_PATHSEP}${TC_ADDITIONAL_CLASSPATH}"
      fi

      if test "$__INCLUDE_TC_JAR" = "true"; then
         TC_CLASSPATH="${TC_CLASSPATH_WITH_TC_JAR}"
      else
         TC_CLASSPATH="${TC_CLASSPATH_WITHOUT_TC_JAR}"
      fi
   else
      if test -n "$TC_VERBOSE_SCRIPTS"; then
        echo "$0: TC_CLASSPATH: ${TC_CLASSPATH}"
      fi
   fi
}

#
# Usage:
#    tc_find_java
#
# Prerequisites:
#    Should only be called from the 'tc_install_dir' script.
#
# Input:
#    TC_JAVA_HOME -- Overrides the Java VM that's used to run the application
#
# Output:
#    TC_JAVA -- The path to the 'java' executable that we should use to run the application
#
tc_find_java() {
   if test -z "$TC_JAVA_HOME"; then
      TC_JAVA="${TC_INSTALL_DIR}/jre/bin/java"
      if test \! -x "$TC_JAVA"; then
         echo "$0: You haven't specified a JRE installation to use "
         echo "       (via the TC_JAVA_HOME environment variable), and "
         echo "       there is no JRE included in this Terracotta kit. Stop."

         exit 10
      fi

      TC_JAVA_HOME="${TC_INSTALL_DIR}/jre"
      if test -n "$TC_VERBOSE_SCRIPTS"; then
        echo "$0: TC_JAVA_HOME: ${TC_JAVA_HOME}"
      fi
   else
      if (test \! -d "$TC_JAVA_HOME") || (test \! -x "$TC_JAVA_HOME/bin/java"); then
         echo "$0: The JRE you specified in TC_JAVA_HOME, ${TC_JAVA_HOME},"
         echo "       does not appear to be a directory with a valid 'java' command "
         echo "       in it (at ${TC_JAVA_HOME}/bin). Stop."

         exit 11
      fi

      TC_JAVA="${TC_JAVA_HOME}/bin/java"

      if test -n "$TC_VERBOSE_SCRIPTS"; then
        echo "$0: TC_JAVA_HOME: ${TC_JAVA_HOME}"
      fi
#      echo ""
#      echo "Java version: "
#      echo ""
#      "${TC_JAVA}" -version
#      echo ""
   fi
}

#
# Usage:
#    tc_java_opts <default JVM options>
# (options must be a single shell variable -- a single "word")
#
# Prerequisites:
#    None.
#
# Input:
#    TC_JAVA_OPTS -- Adds additional options to the Java command line
#    TC_ALL_JAVA_OPTS -- Replaces ALL Java command-line options; use carefully
#
# Output:
#    TC_ALL_JAVA_OPTS -- The entire set of JVM options the application should run with
#
tc_java_opts() {
   __DEFAULT_TC_JAVA_OPTS="$1"

   if test -n "$TC_ALL_JAVA_OPTS"; then
      if test -n "$TC_VERBOSE_SCRIPTS"; then
        echo "$0: TC_ALL_JAVA_OPTS: ${TC_ALL_JAVA_OPTS}"
      fi
   else
      if test -n "$TC_JAVA_OPTS"; then
        if test -n "$TC_VERBOSE_SCRIPTS"; then
          echo "$0: TC_JAVA_OPTS: ${TC_JAVA_OPTS}"
        fi
         TC_ALL_JAVA_OPTS="${__DEFAULT_TC_JAVA_OPTS} ${TC_JAVA_OPTS}"
      else
         TC_ALL_JAVA_OPTS="${__DEFAULT_TC_JAVA_OPTS}"
      fi
   fi
}

#
# Usage:
#    tc_config <default TC config>
#
# Prerequisites:
#    None.
#
# Input:
#    TC_CONFIG -- Overrides the Terracotta configuration file to be used by this application
#
# Output:
#    D_TC_CONFIG -- The entire JVM option that defines the path to the Terracotta configuration
#
tc_config() {
   __DEFAULT_TC_CONFIG="$1"

   if test -n "$TC_CONFIG"; then
      if test -n "$TC_VERBOSE_SCRIPTS"; then
        echo "$0: TC_CONFIG: ${TC_CONFIG}"
      fi
      D_TC_CONFIG="-Dtc.config=${TC_CONFIG}"
   else
      D_TC_CONFIG="-Dtc.config=${__DEFAULT_TC_CONFIG}"
   fi
}

tc_set_dso_boot_jar() {
   if test -n "$DSO_BOOT_JAR"; then
      if test \! -f "${DSO_BOOT_JAR}"; then
         echo "$0: The custom DSO boot JAR you specified in the DSO_BOOT_JAR "
         echo "       environment variable, ${DSO_BOOT_JAR}, does not exist. Stop."

         exit 12
      fi
   else
      DSO_BOOT_JAR_NAME="`${TC_JAVA} -classpath "${TC_INSTALL_DIR}/common/lib/tc.jar" ${TC_ALL_JAVA_OPTS} com.tc.object.tools.BootJarSignature`"
      __FIND_DSO_BOOT_JAR_CMD_EXIT_CODE="$?"
      if test "$__FIND_DSO_BOOT_JAR_CMD_EXIT_CODE" != 0; then
         echo "$0: We were unable to determine the correct"
         echo "       name of the DSO boot JAR; we ran the tool do to that as follows:"
         echo ""
         echo "       ${TC_JAVA} -classpath \"${TC_INSTALL_DIR}\common\lib\tc.jar\" ${TC_ALL_JAVA_OPTS} com.tc.object.tools.BootJarSignature"
         echo ""
         echo "       ...but we got exit code ${__FIND_DSO_BOOT_JAR_CMD_EXIT_CODE}. Stop."

         exit 13
      fi

      DSO_BOOT_JAR="${TC_INSTALL_DIR}/common/lib/dso-boot/${DSO_BOOT_JAR_NAME}"
      if test \! -f "${DSO_BOOT_JAR}"; then
         echo "$0: The DSO boot JAR you need for this platform,"
         echo "       ${DSO_BOOT_JAR},"
         echo "       does not exist. You may need to run the 'make-boot-jar' script,"
         echo "       or consult the Terracotta documentation for more information."

         exit 14
      fi
   fi
}

#
# Usage:
#    tc_java [-subshell] ...command-line options...
#
# Prerequisites:
#    'tc_java_opts' must be called first.
#
# Input:
#    -subshell -- If this is the FIRST option, then 'java' will be executed by
#                 this shell, as normal. Otherwise, it will be executed using
#                 'exec', replacing this shell process with the Java VM.
#    TC_VERBOSE_SCRIPTS -- If set to any nonempty value, will echo the command
#                          to be executed just before it runs
#
# Output:
#    None; runs ${TC_JAVA} with the specified options.
#
tc_java() {
   __EXEC=exec

   if test "$1" = "-subshell"; then
      __EXEC=""
      shift
   fi

   if test -n "$TC_VERBOSE_SCRIPTS"; then
      echo "Executing: ${TC_JAVA} $@"
   fi

   "${__EXEC}" "${TC_JAVA}" "$@"
}

#
# Usage:
#    run_dso_java ...command-line options...
#
# Prerequisites:
#    'tc_install_dir' must be called first, with 'true' as the second argument.
#    'tc_java_opts' must be called first.
#
# Input:
#    TC_VERBOSE_SCRIPTS -- If set to any nonempty value, will echo the command
#                          to be executed just before it runs
#
# Output:
#    None; runs ${DSO_JAVA} with the specified options.
#
# Note:
#    This should be used ONLY for Terracotta scripts that need to run the
#    equivalent of 'dso-java'. The problem is that if you use the 'dso-java'
#    script itself, all the initialization and setup will get done twice, which
#    you don't want. Instead, use this, and you'll be set.
#
run_dso_java() {
   if test -z "$TC_INSTALL_DIR"; then
      echo "$0: Error: You must call 'tc_install_dir' first, with 'true' as "
      echo "       the second argument."

      exit 13
   fi

   tc_set_dso_boot_jar

   if test "${DSO_JAVA_IS_CUSTOM}" = "true"; then
      "${DSO_JAVA}" "$@"
   else
      tc_java "-Xbootclasspath/p:${DSO_BOOT_JAR}" "-Dtc.install-root=${TC_INSTALL_DIR}" "$@"
   fi
}

#
# UNSUPPORTED environment variables -- these will probably work, but may result
# in software that doesn't work correctly, and is definitely not supported by
# Terracotta:
#
#    TC_INSTALL_DIR -- Overrides the root of your Terracotta installation
#
#    DSO_BOOT_JAR -- Overrides the DSO boot JAR to use
#
#    DSO_JAVA -- Overrides the 'dso-java' script to use
#
#    TC_JAR_CLASSES -- Uses the classes in this CLASSPATH variable instead of
#                      'tc.jar' (use with extreme caution)
#
#    TC_CLASSPATH -- Overrides the ENTIRE class path for the application
#                    (use with extreme caution)
#
#    TC_ALL_JAVA_OPTS -- Replaces ALL Java VM options with the ones specified
#                        in this variable (use with caution)
#
#    TC_CONFIG -- Overrides the Terracotta configuration file that is used by
#                 the application with the one specified here
#                 (use with extreme caution)
#
