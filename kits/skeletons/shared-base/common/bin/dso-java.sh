#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

if [ $# == 0 ]; then
  echo "usage: dso-java [-options] class [args...]"
  exit 0
fi

ARGS=$*

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

TC_INSTALL_DIR=`dirname "$0"`/..
set -- -q "${TC_CONFIG}"
. "${TC_INSTALL_DIR}/bin/dso-env.sh"

# For Cygwin, convert paths to Windows
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --windows "$JAVA_HOME"`
fi

exec "${JAVA_HOME}/bin/java" ${TC_JAVA_OPTS} ${JAVA_OPTS} $ARGS
