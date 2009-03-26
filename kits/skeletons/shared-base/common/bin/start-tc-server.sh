#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.


#

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

TC_INSTALL_DIR=`dirname "$0"`/..

# For Cygwin, convert paths to Windows before invoking java
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --windows "$JAVA_HOME"`
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath --windows "$TC_INSTALL_DIR"`
fi

${JAVA_HOME}/bin/java -server > /dev/null 2>&1
if test "$?" = "0" ; then
  SERVER_OPT=-server
else
  SERVER_OPT=
fi

start=true
while "$start"
do
"${JAVA_HOME}/bin/java" \
   $SERVER_OPT -Xms512m -Xmx512m -XX:NewRatio=3 -XX:MaxTenuringThreshold=15 -XX:+HeapDumpOnOutOfMemoryError \
   -Dcom.sun.management.jmxremote \
   -Dtc.install-root="${TC_INSTALL_DIR}" \
   ${JAVA_OPTS} \
   -cp "${TC_INSTALL_DIR}/lib/tc.jar" \
   com.tc.server.TCServerMain "$@"
 exitValue=$?
 start=false;

 if test "$exitValue" = "11"; then
   start=true;
   echo "start-tc-server: Restarting the server..."
 else
   exit $exitValue
 fi
done
