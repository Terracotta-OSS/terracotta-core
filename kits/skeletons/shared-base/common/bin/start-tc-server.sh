#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

case "$1" in
  "--help"|"-h"|"-?")
    echo "Syntax: $0 [-f /path/to/tc-config.xml] [-n server_name]"
    echo
    echo "-f : start the server with your own Terracotta configuration instead of the default one"
    echo "-n : specify which server you want to start when you have more than one servers configured"
    exit
    ;;
esac

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

for JAVA_COMMAND in \
"${JAVA_HOME}/bin/java -d64 -server -XX:MaxDirectMemorySize=64g" \
"${JAVA_HOME}/bin/java -server -XX:MaxDirectMemorySize=1g" \
"${JAVA_HOME}/bin/java -d64 -client  -XX:MaxDirectMemorySize=64g" \
"${JAVA_HOME}/bin/java -client -XX:MaxDirectMemorySize=1g" \
"${JAVA_HOME}/bin/java"
do
  ${JAVA_COMMAND} -version > /dev/null 2>&1
  if test "$?" = "0" ; then break; fi
done

#rmi.dgc.server.gcInterval is set an year to avoid system gc in case authentication is enabled
#users may change it accordingly
start=true
while "$start"
do
${JAVA_COMMAND} -Xms512m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError \
   -Dcom.sun.management.jmxremote \
   -Dtc.install-root="${TC_INSTALL_DIR}" \
   -Dsun.rmi.dgc.server.gcInterval=31536000 \
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
