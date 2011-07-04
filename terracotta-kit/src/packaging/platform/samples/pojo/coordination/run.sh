#!/bin/sh

#
#  All content copyright Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

# samples/pojos/coordination
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

# OS specific support.  $var _must_ be set to either true or false.
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

exec "${JAVA_HOME}/bin/java" ${TC_JAVA_OPTS} ${JAVA_OPTS} \
  -cp "${CWD}/classes${PATH_SEPARATOR}${CWD}/lib/concurrent-1.3.4.jar" \
   demo.coordination.Main $ARGS
