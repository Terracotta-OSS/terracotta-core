#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

# -------------------
# stop.cmd 908{1,2}
# -------------------

cd "`dirname $0`/.."
SANDBOX="`pwd`"
TC_INSTALL_DIR="${SANDBOX}/../../.."

WL_HOME="${BEA_HOME}/weblogic92"
PRODUCTION_MODE=
JAVA_VENDOR="Sun"
export PRODUCTION_MODE JAVA_VENDOR

ADMIN_URL="t3://localhost:$1"
SERVER_NAME="myserver"

. "${WL_HOME}/common/bin/commEnv.sh"

CLASSPATH="${WEBLOGIC_CLASSPATH}:${POINTBASE_CLASSPATH}:${JAVA_HOME}/jre/lib/rt.jar:${WL_HOME}/server/lib/webservices.jar:${CLASSPATH}"

exec "${JAVA_HOME}/bin/java" -classpath "${CLASSPATH}" weblogic.Admin FORCESHUTDOWN -url "${ADMIN_URL}" -username weblogic -password weblogic "${SERVER_NAME}"
