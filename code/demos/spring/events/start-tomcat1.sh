#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

CWD=`dirname "$0"`
TC_INSTALL_DIR=${CWD}/../../..

TC_CONFIG_PATH="${CWD}/tc-config.xml"
. "${TC_INSTALL_DIR}/bin/dso-env.sh" -q
JAVA_OPTS="${TC_JAVA_OPTS} -Dcom.sun.management.jmxremote"
export JAVA_OPTS

CATALINA_HOME="${TC_INSTALL_DIR}/vendors/tomcat5.5"
CATALINA_BASE="tomcat1"
export CATALINA_HOME CATALINA_BASE

mkdir -p "${CATALINA_BASE}/logs" "${CATALINA_BASE}/temp"
exec "${CATALINA_HOME}/bin/catalina.sh" run
