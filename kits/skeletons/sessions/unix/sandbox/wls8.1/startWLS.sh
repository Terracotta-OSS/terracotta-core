#!/bin/sh

#@COPYRIGHT@

if test -z "${BEA_HOME}"; then
    echo "BEA_HOME must be set to an 8.1 installation"
    exit 1
fi

if test -z "${WL_HOME}"; then
  WL_HOME="${BEA_HOME}/weblogic81"
  export WL_HOME
fi

PRODUCTION_MODE=
JAVA_VENDOR=Sun
export PRODUCTION_MODE JAVA_VENDOR

if test -z "${JAVA_HOME}"; then
    JAVA_HOME="${BEA_HOME}/jdk142_11"
    export JAVA_HOME
fi

. "${WL_HOME}/common/bin/commEnv.sh"

SERVER_NAME=myserver
export SERVER_NAME

WLS_USER=tc
WLS_PW=tc

CLASSPATH="${WEBLOGIC_CLASSPATH}:${POINTBASE_CLASSPATH}:${JAVA_HOME}/jre/lib/rt.jar:${WL_HOME}/server/lib/webservices.jar:${CLASSPATH}"

exec "${JAVA_HOME}/bin/java" ${JAVA_VM} ${MEM_ARGS} ${JAVA_OPTIONS} -classpath "${CLASSPATH}" "-Dweblogic.Name=${SERVER_NAME}" \
    "-Dweblogic.management.username=${WLS_USER}" "-Dweblogic.management.password=${WLS_PW}" "-Dweblogic.ProductionModeEnabled=${PRODUCTION_MODE}" \
    "-Djava.security.policy=${WL_HOME}/server/lib/weblogic.policy" weblogic.Server
