#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

TC_INSTALL_DIR=`dirname "$0"`/..

# For Cygwin, convert paths to Windows before invoking java
if $cygwin; then
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath -d "$TC_INSTALL_DIR"`
fi

CP=$TC_INSTALL_DIR/lib/tc.jar

JAVA_OPTS="-Xmx256m -Dsun.java2d.pmoffscreen=false -Dtc.install-root=${TC_INSTALL_DIR} $JAVA_OPTS"
exec "${JAVA_HOME}/bin/java" \
  ${JAVA_OPTS} \
  -cp "$CP" \
  com.tc.admin.EnterpriseAdminClient "$@"
