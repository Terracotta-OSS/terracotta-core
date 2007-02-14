#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

TOPDIR=`dirname "$0"`/../../..
. "${TOPDIR}"/bin/tc-functions.sh

TC_CONFIG_PATH="tc-config.xml"
. "${TOPDIR}"/bin/dso-env.sh

JAVA_OPTS="${TC_JAVA_OPTS} -Dcounter.log.prefix=CounterService-Tomcat-Node-1:"
export JAVA_OPTS

JAVA_HOME="${TC_JAVA_HOME:-${TOPDIR}/jre}"
CATALINA_HOME="${TOPDIR}/vendors/tomcat5.5"
CATALINA_BASE="tomcat1"
export JAVA_HOME CATALINA_HOME CATALINA_BASE

mkdir -p "${CATALINA_BASE}/logs" "${CATALINA_BASE}/temp"

# To run Tomcat in a debugger you can use the following options
# JPDA_TRANSPORT=dt_socket
# JPDA_ADDRESS=8095
# export JPDA_TRANSPORT JPDA_ADDRESS
# exec "${CATALINA_HOME}/bin/catalina.sh" jpda run

exec "${CATALINA_HOME}/bin/catalina.sh" run
