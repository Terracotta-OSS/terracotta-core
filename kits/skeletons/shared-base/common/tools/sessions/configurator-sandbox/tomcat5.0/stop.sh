#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.


#

cd `dirname "$0"`/..
SANDBOX="`pwd`"
TC_INSTALL_DIR="${SANDBOX}/../../.."

CATALINA_BASE="${SANDBOX}/tomcat5.0/$1"
export CATALINA_BASE

if test ! -f "${CATALINA_HOME}/bin/catalina.sh"; then
  echo "CATALINA_HOME must be set to a Tomcat5.0 installation"
  exit 1
fi

exec "${CATALINA_HOME}/bin/catalina.sh" stop
