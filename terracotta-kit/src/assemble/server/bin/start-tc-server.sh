#!/bin/sh

# 
# The contents of this file are subject to the Terracotta Public License Version
# 2.0 (the "License"); You may not use this file except in compliance with the
# License. You may obtain a copy of the License at 
# 
#      http://terracotta.org/legal/terracotta-public-license.
# 
# Software distributed under the License is distributed on an "AS IS" basis,
# WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
# the specific language governing rights and limitations under the License.
# 
# The Covered Software is Terracotta Platform.
# 
# The Initial Developer of the Covered Software is 
#     Terracotta, Inc., a Software AG company
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

# Install the signal handler so that SIGTERM (15) is relayed to the underlying JVM.
PID=0
function cleanup_TERM {
    kill -TERM $PID
}
trap cleanup_TERM SIGTERM
# Same thing for SIGINT (2).
# NOTE:  The shell sets SIGINT of the inferior process to SIG_IGN (ignored) when starting
# the process in the background (&) so we will actually commute the signal to SIGTERM
# (since that is our intention, anyway).
function cleanup_INT {
    kill -TERM $PID
}
trap cleanup_INT SIGINT


THIS_DIR=`dirname $0`
TC_INSTALL_DIR=`cd $THIS_DIR;pwd`/../..
PLUGIN_LIB_DIR="$TC_INSTALL_DIR/plugins/lib"
PLUGIN_API_DIR="$TC_INSTALL_DIR/plugins/api"

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi


for JAVA_COMMAND in \
"${JAVA_HOME}/bin/java -d64 -server -XX:MaxDirectMemorySize=9223372036854775807" \
"${JAVA_HOME}/bin/java -server -XX:MaxDirectMemorySize=9223372036854775807" \
"${JAVA_HOME}/bin/java -d64 -client  -XX:MaxDirectMemorySize=9223372036854775807" \
"${JAVA_HOME}/bin/java -client -XX:MaxDirectMemorySize=9223372036854775807" \
"${JAVA_HOME}/bin/java -XX:MaxDirectMemorySize=9223372036854775807"
do
  ${JAVA_COMMAND} -version > /dev/null 2>&1
  if test "$?" = "0" ; then break; fi
done

function setPluginClasspath {

    shopt -s nullglob

    for pluginDir in "${PLUGIN_LIB_DIR}" "${PLUGIN_API_DIR}"
    do
        if [ -e "${pluginDir}" ]
        then
            for jarFile in "${pluginDir}"/*.jar
            do
                PLUGIN_CLASSPATH=${PLUGIN_CLASSPATH}:${jarFile}
            done
#  Adding SLF4j libraries to the classpath of the server to 
#  support services that may use SLF4j for logging
            for jarFile in "${TC_INSTALL_DIR}"/server/lib/slf4j*.jar
            do
                PLUGIN_CLASSPATH=${PLUGIN_CLASSPATH}:${jarFile}
            done
        fi
    done

    shopt -u nullglob

}

setPluginClasspath;

#rmi.dgc.server.gcInterval is set an year to avoid system gc in case authentication is enabled
#users may change it accordingly
start=true
while "$start"
do
${JAVA_COMMAND} -Xms2g -Xmx2g -XX:+HeapDumpOnOutOfMemoryError \
   -Dcom.sun.management.jmxremote \
   -Dtc.install-root="${TC_INSTALL_DIR}" \
   -Dsun.rmi.dgc.server.gcInterval=31536000000\
   ${JAVA_OPTS} \
   -cp "${TC_INSTALL_DIR}/server/lib/tc.jar:${PLUGIN_CLASSPATH}" \
   com.tc.server.TCServerMain "$@" &
 PID=$!
 # We want to output the PID of the underlying process to STDOUT.
 echo "Server started as $PID"
 
 # NOTE:  Wait may return prematurely if the shell runs its signal handler but we know that it
 # returns 127 if the call fails so we will loop on calling it until that happens (we want to see
 # it fail due to the child process exiting and no longer being a child of this shell).  The last
 # wait call, prior to this failing call, will contain the correct return value.
 exitValue=128
 keepWaiting=true
 while [ "$keepWaiting" == true ]; do
  # Redirect error so we don't see the message about not being a child shell.
  wait $PID >& /dev/null
  returnValue=$?
  if [ $returnValue -ne 127 ]; then
   exitValue=$returnValue
  else
   keepWaiting=false
  fi
 done
 start=false;

 if test "$exitValue" = "11"; then
   start=true;
   echo "start-tc-server: Restarting the server..."
 else
   exit $exitValue
 fi
done

