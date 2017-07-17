#!/bin/bash

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



THIS_DIR=`dirname $0`
TC_SERVER_DIR=`cd $THIS_DIR;pwd`/..
PLUGIN_LIB_DIR="$TC_SERVER_DIR/plugins/lib"
PLUGIN_API_DIR="$TC_SERVER_DIR/plugins/api"

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
            for jarFile in "${TC_SERVER_DIR}"/lib/slf4j*.jar
            do
                PLUGIN_CLASSPATH=${PLUGIN_CLASSPATH}:${jarFile}
            done
        fi
    done

    shopt -u nullglob

}

setPluginClasspath;

PLUGIN_CLASSPATH=${PLUGIN_CLASSPATH}:${TC_SERVER_DIR}/lib

#rmi.dgc.server.gcInterval is set an year to avoid system gc in case authentication is enabled
#users may change it accordingly
start=true
while "$start"
do
# the solaris 64-bit JVM has a bug that makes it fail to allocate more than 2GB of offheap when
# the max heap is <= 2G, hence we set the heap size to a bit more than 2GB
${JAVA_COMMAND} -Xms256m -Xmx2049m -XX:+HeapDumpOnOutOfMemoryError \
   -Dtc.install-root="${TC_SERVER_DIR}" \
   ${JAVA_OPTS} \
   -cp "${TC_SERVER_DIR}/lib/tc.jar:${PLUGIN_CLASSPATH}" \
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

