#!/bin/sh

#
#@COPYRIGHT@
#
# Helper script that sets DSO_BOOT_JAR location, creating if necessary.
# Used by the dso-env script.
#
# JAVA_HOME -- [required] JVM to use when checking for/creating the bootjar
# TC_INSTALL_DIR -- [required] root of Terracotta installation
# TC_CONFIG_PATH -- [optional] config file to use when creating bootjar
# DSO_BOOT_JAR -- [optional] path to DSO bootjar; will be created iff it
#                 doesn't exist; if not specified, is set to default, VM-
#                 specific location under ${TC_INSTALL_DIR}/lib/dso-boot.
#

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath --unix "$TC_INSTALL_DIR"`
fi

JAVACMD=${JAVA_HOME}/bin/java
TC_JAR=${TC_INSTALL_DIR}/lib/tc.jar

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$TC_JAR" ] && TC_JAR=`cygpath --windows "$TC_JAR"`
fi

if test -z "$DSO_BOOT_JAR"; then
  DSO_BOOT_JAR_NAME="`"${JAVACMD}" -cp "${TC_JAR}" com.tc.object.tools.BootJarSignature|tr -d '\r'`"
  __BOOT_JAR_SIG_EXIT_CODE="$?"
  if test "$__BOOT_JAR_SIG_EXIT_CODE" != 0; then
    echo "$0: We were unable to determine the correct"
    echo "    name of the DSO boot JAR using the following command:"
    echo ""
    echo "    ${JAVACMD} -cp \"${TC_JAR}\" com.tc.object.tools.BootJarSignature"
    echo ""
    echo "    ...but we got exit code ${__BOOT_JAR_SIG_EXIT_CODE}. Stop."

    exit 13
  fi

  DSO_BOOT_JAR="${TC_INSTALL_DIR}/lib/dso-boot/${DSO_BOOT_JAR_NAME}"
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$DSO_BOOT_JAR" ] && DSO_BOOT_JAR=`cygpath --windows "$DSO_BOOT_JAR"`
fi

if test \! -f "${DSO_BOOT_JAR}"; then
  if test -n "${TC_CONFIG_PATH}"; then
   "${JAVACMD}" -Dtc.install-root="${TC_INSTALL_DIR}" -cp "${TC_JAR}" com.tc.object.tools.BootJarTool -o "${DSO_BOOT_JAR}" -f "${TC_CONFIG_PATH}"
  else
   "${JAVACMD}" -Dtc.install-root="${TC_INSTALL_DIR}" -cp "${TC_JAR}" com.tc.object.tools.BootJarTool -o "${DSO_BOOT_JAR}"
  fi
  if test $? != 0; then
    exit 14
  fi
fi
