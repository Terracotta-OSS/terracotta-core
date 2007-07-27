#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

#
# samples/spring/webflow
#
# Environment variables required by dso-env helper script:
#  JAVA_HOME: root of Java Development Kit installation
#  TC_INSTALL_DIR: root of Terracotta installation
#
# Arguments to dso-env helper script:
#  -q: do not print value of TC_JAVA_OPTS
#  TC_CONFIG_PATH: location of Terracotta config file; overridden by value
#                  of optional TC_CONFIG
#
# Environment variable set by dso-env helper script:
#  TC_JAVA_OPTS: Java options needed to activate DSO
#

CWD=`dirname "$0"`
TC_INSTALL_DIR=${CWD}/../../..

TC_CONFIG_PATH="${CWD}/tc-config.xml"
set -- -q "${TC_CONFIG}"
. "${TC_INSTALL_DIR}/bin/dso-env.sh"

OPTS="${TC_JAVA_OPTS} -Dcom.sun.management.jmxremote"
OPTS="${OPTS} -Dcom.sun.management.jmxremote.port=8091"
OPTS="${OPTS} -Dcom.sun.management.jmxremote.authenticate=false"
OPTS="${OPTS} -Dcom.sun.management.jmxremote.ssl=false"
OPTS="${OPTS} -Dtc.node-name=Node1"
JAVA_OPTS="${OPTS} ${JAVA_OPTS}"
export JAVA_OPTS

CATALINA_HOME="${TC_INSTALL_DIR}/vendors/tomcat5.5"
CATALINA_BASE="tomcat1"
export CATALINA_HOME CATALINA_BASE

mkdir -p "${CATALINA_BASE}/logs" "${CATALINA_BASE}/temp"
exec "${CATALINA_HOME}/bin/catalina.sh" run
