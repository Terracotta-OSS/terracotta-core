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

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath -d "$TC_INSTALL_DIR"`
fi

exec "${JAVA_HOME}/bin/java" \
  -Dtc.install-root="${TC_INSTALL_DIR}" \
  ${JAVA_OPTS} \
  -cp "${TC_INSTALL_DIR}/lib/tc.jar" \
  com.tc.gcrunner.GCRunner "$@"
