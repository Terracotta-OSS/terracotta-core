#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

TC_INSTALL_DIR=`dirname "$0"`/../..
CP="$TC_INSTALL_DIR/lib/tc.jar"

# For Cygwin, convert paths to Windows
if $cygwin; then
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath -d "$TC_INSTALL_DIR"`
  [ -n "$CP" ] && CP=`cygpath -d "$CP"`
fi

"${JAVA_HOME}/bin/java" \
  -Dtc.install-root="${TC_INSTALL_DIR}" \
  ${JAVA_OPTS} \
  -cp "$CP" \
  org.terracotta.ui.session.SessionIntegrator "$@"
