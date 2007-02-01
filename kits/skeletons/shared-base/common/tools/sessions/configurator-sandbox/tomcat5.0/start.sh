#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

cd "`dirname $0`/.."
SANDBOX="`pwd`"
TC_INSTALL_DIR="${SANDBOX}/../../.."

PORT="$1"
CATALINA_BASE="${SANDBOX}/tomcat5.0/${PORT}"
export CATALINA_BASE

if test "$2" != "nodso"; then
  . ../../../bin/tc-functions.sh
  tc_install_dir "${TC_INSTALL_DIR}"
  tc_set_dso_boot_jar

  JAVA_OPTS="${JAVA_OPTS} -Xbootclasspath/p:${DSO_BOOT_JAR}"
  JAVA_OPTS="${JAVA_OPTS} -Dtc.install-root=${TC_INSTALL_DIR}"
  JAVA_OPTS="${JAVA_OPTS} -Dtc.config=${SANDBOX}/tomcat5.0/tc-config.xml"
  JAVA_OPTS="${JAVA_OPTS} -Dwebserver.log.name=${PORT}"
  JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote"
  export JAVA_OPTS
fi

JAVA_HOME="${TC_JAVA_HOME:-${TC_INSTALL_DIR}/jre}"
export JAVA_HOME

if test ! -d "${CATALINA_HOME}" -o ! -f "${CATALINA_HOME}/bin/catalina.sh"; then
  echo "CATALINA_HOME must be set to a Tomcat5.0 installation"
  exit 1
fi

exec "${CATALINA_HOME}/bin/catalina.sh" run
