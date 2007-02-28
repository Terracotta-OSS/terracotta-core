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
CATALINA_BASE="${SANDBOX}/tomcat5.5/${PORT}"
export CATALINA_BASE

if test "$2" != "nodso"; then
  TC_CONFIG_PATH="${SANDBOX}/tomcat5.5/tc-config.xml"
  . "${TC_INSTALL_DIR}"/bin/dso-env.sh -q

  JAVA_OPTS="${TC_JAVA_OPTS} ${JAVA_OPTS}"
  JAVA_OPTS="${JAVA_OPTS} -Dwebserver.log.name=${PORT}"
  JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote"
  export JAVA_OPTS
fi

CATALINA_HOME="${CATALINA_HOME:-${TC_INSTALL_DIR}/vendors/tomcat5.5}"
export CATALINA_HOME

if test ! -f "${CATALINA_HOME}/bin/catalina.sh"; then
  echo "CATALINA_HOME must be set to a Tomcat5.5 installation"
  exit 1
fi

exec "${CATALINA_HOME}/bin/catalina.sh" run
