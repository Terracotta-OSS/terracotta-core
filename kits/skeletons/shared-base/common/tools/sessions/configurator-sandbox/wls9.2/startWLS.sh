#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

if test -z "${BEA_HOME}"; then
  echo "BEA_HOME must be set to an 8.1 installation"
  exit 1
fi

if test -z "${WL_HOME}"; then
  WL_HOME="${BEA_HOME}/weblogic92"
  export WL_HOME
fi

if test \! -d "${WL_HOME}"; then
  echo "WL_HOME '${WL_HOME}' does not exist. Did you set BEA_HOME correctly?"
  exit 1
fi

PRODUCTION_MODE=
JAVA_VENDOR=Sun
export PRODUCTION_MODE JAVA_VENDOR

if test -z "${JAVA_HOME}"; then
  JAVA_HOME="${BEA_HOME}/jdk150_10"
  export JAVA_HOME
fi

. "${WL_HOME}/common/bin/commEnv.sh"

SERVER_NAME=myserver
export SERVER_NAME

WLS_USER=weblogic
WLS_PW=weblogic

CLASSPATH="${WEBLOGIC_CLASSPATH}:${POINTBASE_CLASSPATH}:${JAVA_HOME}/jre/lib/rt.jar:${WL_HOME}/server/lib/webservices.jar:${CLASSPATH}"

exec "${JAVA_HOME}/bin/java" ${JAVA_VM} ${MEM_ARGS} ${JAVA_OPTIONS} -classpath "${CLASSPATH}" "-Dweblogic.Name=${SERVER_NAME}" \
    "-Dweblogic.management.username=${WLS_USER}" "-Dweblogic.management.password=${WLS_PW}" "-Dweblogic.ProductionModeEnabled=${PRODUCTION_MODE}" \
    "-Djava.security.policy=${WL_HOME}/server/lib/weblogic.policy" weblogic.Server
