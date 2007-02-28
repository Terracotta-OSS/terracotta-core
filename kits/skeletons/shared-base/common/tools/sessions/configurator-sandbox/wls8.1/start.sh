#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

# ----------------------------------------
# - start.sh 908{1,2} [nodso] [nowindow]
# ----------------------------------------

cd "`dirname $0`/.."
SANDBOX="`pwd`"
TC_INSTALL_DIR="${SANDBOX}/../../.."

if test -z "${JAVA_HOME}" -a -n "${BEA_HOME}" -a -d "${BEA_HOME}/jdk142_11"; then
  JAVA_HOME="${BEA_HOME}/jdk142_11"
  export JAVA_HOME
fi

if test -z "${JAVA_HOME}"; then
  echo "JAVA_HOME must be set to a 1.4 JDK."
  exit 1
fi

PORT="$1"

if test "$2" != "nodso"; then
  TC_CONFIG_PATH="${SANDBOX}/wls8.1/tc-config.xml"
  . "${TC_INSTALL_DIR}/bin/dso-env.sh" -q

  JAVA_OPTIONS="${TC_JAVA_OPTS} ${JAVA_OPTS}"
  JAVA_OPTIONS="${JAVA_OPTIONS} -Dwebserver.log.name=${PORT}"
  JAVA_OPTIONS="${JAVA_OPTIONS} -Dcom.sun.management.jmxremote"
  export JAVA_OPTIONS
fi

cd "${SANDBOX}/wls8.1/${PORT}"
rm -f SerializedSystemIni.dat
rm -rf myserver
rm -rf applications/.wlnotdelete
cp tmpls/* .

exec ../startWLS.sh
