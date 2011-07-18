#!/bin/sh

#  All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

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
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath -d "$TC_INSTALL_DIR"`
fi

JAVACMD=${JAVA_HOME}/bin/java
TC_JAR=${TC_INSTALL_DIR}/lib/tc.jar

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$TC_JAR" ] && TC_JAR=`cygpath -d "$TC_JAR"`
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
 
  BOOT_JAR_DIR="${TC_INSTALL_DIR}/lib/dso-boot"
  if [ ! -d "${BOOT_JAR_DIR}" ]; then
    mkdir -p "${BOOT_JAR_DIR}"
  fi 
  DSO_BOOT_JAR="${BOOT_JAR_DIR}/${DSO_BOOT_JAR_NAME}"
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  if [ -n "$DSO_BOOT_JAR" ]; then
    # cygpath -d only works if the file exists so we need 
    # to make sure the designated bootjar file existed first
    if [ -f "$DSO_BOOT_JAR" ]; then
      DSO_BOOT_JAR=`cygpath -d "$DSO_BOOT_JAR"`
    else
      touch "$DSO_BOOT_JAR"
      DSO_BOOT_JAR=`cygpath -d "$DSO_BOOT_JAR"`
      rm "$DSO_BOOT_JAR"
    fi
  fi
  [ -n "$TC_CONFIG_PATH" ] && TC_CONFIG_PATH=`cygpath -d "$TC_CONFIG_PATH"`
fi

echo "Starting BootJarTool..."
if test -n "${TC_CONFIG_PATH}"; then
  "${TC_INSTALL_DIR}/platform/bin/make-boot-jar.sh" -o "${DSO_BOOT_JAR}" -f "${TC_CONFIG_PATH}"
else
  "${TC_INSTALL_DIR}/platform/bin/make-boot-jar.sh" -o "${DSO_BOOT_JAR}"
fi
if test $? != 0; then
  exit 14
fi
