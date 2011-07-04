#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.


#

if test "$#" = "0"; then 
  echo "usage: dso-java [-options] class [args...]"
  exit 0
fi

ARGS=$*
for i in ${ARGS}; do
  case "${i}" in 
  -D*) export JAVA_OPTS="${JAVA_OPTS} ${i}" ;;
    *) class="${class} ${i}" ;;
  esac 
done

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

TC_INSTALL_DIR=`dirname "$0"`/../..
set -- -q
. "${TC_INSTALL_DIR}/platform/bin/dso-env.sh"

# For Cygwin, convert paths to Windows
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --windows "$JAVA_HOME"`
fi

echo ""
echo "Starting Terracotta client..."
exec "${JAVA_HOME}/bin/java" ${TC_JAVA_OPTS} ${JAVA_OPTS} ${class}
