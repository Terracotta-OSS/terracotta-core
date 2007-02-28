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

JAVACMD=${JAVA_HOME}/bin/java
TC_JAR=${TC_INSTALL_DIR}/lib/tc.jar

if test -z "$DSO_BOOT_JAR"; then
  DSO_BOOT_JAR_NAME="`"${JAVACMD}" -cp "${TC_JAR}" com.tc.object.tools.BootJarSignature`"
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
if test \! -f "${DSO_BOOT_JAR}"; then
  if test -n "${TC_CONFIG_PATH}"; then
   "${JAVACMD}" -cp "${TC_JAR}" com.tc.object.tools.BootJarTool -o "${DSO_BOOT_JAR}" -f "${TC_CONFIG_PATH}"
  else
   "${JAVACMD}" -cp "${TC_JAR}" com.tc.object.tools.BootJarTool -o "${DSO_BOOT_JAR}"
  fi
fi
