#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

TC_INSTALL_DIR=`dirname "$0"`/../../..

exec "${JAVA_HOME}/bin/java" \
  -Dtc.install-root="${TC_INSTALL_DIR}" -Ddaemon=true \
  ${JAVA_OPTS} \
  -cp "${TC_INSTALL_DIR}/lib/tc.jar" \
   com.tc.net.proxy.TCPProxy 8080 localhost:8081,localhost:8082,localhost:8083
