#!/bin/sh

#
#  All content copyright Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

#
# samples/pojos/sharedqueue
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

cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

PATH_SEPARATOR=":"
if $cygwin; then
  PATH_SEPARATOR=";"
fi

CWD=`dirname "$0"`
TC_INSTALL_DIR=${CWD}/../../../..
ARGS=$*
TC_CONFIG_PATH="${CWD}/tc-config.xml"
set -- -q
. "${TC_INSTALL_DIR}/platform/bin/dso-env.sh"

CLASSPATH="${CWD}/classes"
CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${CWD}/lib/jetty-6.1.1.jar"
CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${CWD}/lib/jetty-util-6.1.1.jar"
CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${CWD}/lib/servlet-api-2.5-6.1.1.jar"

exec "${JAVA_HOME}/bin/java" ${TC_JAVA_OPTS} ${JAVA_OPTS} \
  -Dcom.sun.management.jmxremote \
  -cp "${CLASSPATH}" demo.sharedqueue.Main $ARGS
