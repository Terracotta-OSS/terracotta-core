#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

cd `dirname "$0"`/..
SANDBOX="`pwd`"
TC_INSTALL_DIR="${SANDBOX}"/../..

CATALINA_BASE="${SANDBOX}"/tomcat5.0/"$1"
JAVA_HOME="${TC_JAVA_HOME:-${TC_INSTALL_DIR}/jre}"
export CATALINA_BASE JAVA_HOME

if test ! -d "${CATALINA_HOME}" -o ! -f "${CATALINA_HOME}/bin/catalina.sh"; then
  echo "CATALINA_HOME must be set to a Tomcat5.0 installation"
  exit 1
fi

exec "${CATALINA_HOME}/bin/catalina.sh" stop
