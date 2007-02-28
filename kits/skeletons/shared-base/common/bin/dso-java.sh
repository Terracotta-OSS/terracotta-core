#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

TC_INSTALL_DIR=`dirname "$0"`/..
. "${TC_INSTALL_DIR}/bin/dso-env.sh" -q
exec "${JAVA_HOME}/bin/java" ${TC_JAVA_OPTS} ${JAVA_OPTS} "$@"
