#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

# samples/pojos/coordination
#
# Environment variables required by dso-env script:
#  TC_INSTALL_DIR: root of Terracotta installation
#  TC_CONFIG_PATH: location of DSO config file
#
# Environment variable set by dso-env script:
#  TC_JAVA_OPTS: Java options needed to activate DSO
#

CWD=`dirname "$0"`
TC_INSTALL_DIR=${CWD}/../../..

TC_CONFIG_PATH="${CWD}/tc-config.xml"
. "${TC_INSTALL_DIR}/bin/dso-env.sh" -q

exec "${JAVA_HOME}/bin/java" ${TC_JAVA_OPTS} ${JAVA_OPTS} \
  -cp "${CWD}/classes:${CWD}/lib/concurrent-1.3.4.jar" \
   demo.coordination.Main "$@"
