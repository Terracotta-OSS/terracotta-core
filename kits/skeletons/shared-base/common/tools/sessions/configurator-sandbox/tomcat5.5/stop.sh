#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.


#

cd "`dirname $0`/.."
SANDBOX="`pwd`"
TC_INSTALL_DIR="${SANDBOX}/../../.."

CATALINA_BASE="${SANDBOX}/tomcat5.5/$1"
CATALINA_HOME="${CATALINA_HOME:-${TC_INSTALL_DIR}/vendors/tomcat5.5}"
export CATALINA_BASE CATALINA_HOME

if test ! -f "${CATALINA_HOME}/bin/catalina.sh"; then
  echo "CATALINA_HOME must be set to a Tomcat5.5 installation"
  exit 1
fi

exec "${CATALINA_HOME}/bin/catalina.sh" stop