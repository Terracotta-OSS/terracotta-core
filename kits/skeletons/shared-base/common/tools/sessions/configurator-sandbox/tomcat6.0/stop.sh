#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.


#

cd "`dirname $0`/.."
SANDBOX="`pwd`"
TC_INSTALL_DIR="${SANDBOX}/../../.."

CATALINA_BASE="${SANDBOX}/tomcat6.0/$1"
CATALINA_HOME="${CATALINA_HOME:-${TC_INSTALL_DIR}/vendors/tomcat6.0}"
export CATALINA_BASE CATALINA_HOME

if test ! -f "${CATALINA_HOME}/bin/catalina.sh"; then
  echo "CATALINA_HOME must be set to a Tomcat6.0 installation"
  exit 1
fi

exec "${CATALINA_HOME}/bin/catalina.sh" stop