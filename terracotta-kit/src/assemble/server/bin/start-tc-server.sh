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

TC_SERVER_DIR=$(dirname "$(cd "$(dirname "$0")";pwd)")
PLUGIN_LIB_DIR="${TC_SERVER_DIR}/plugins/lib"
PLUGIN_API_DIR="${TC_SERVER_DIR}/plugins/api"

# this will only happen if using sag installer
if [ -r "${TC_SERVER_DIR}/bin/setenv.sh" ] ; then
  . "${TC_SERVER_DIR}/bin/setenv.sh"
fi

if ! [ -d "${JAVA_HOME}" ]; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

# Determine supported JVM args
for JAVA_COMMAND_ARGS in \
    "-d64 -server -XX:MaxDirectMemorySize=1048576g" \
    "-server -XX:MaxDirectMemorySize=1048576g" \
    "-d64 -client  -XX:MaxDirectMemorySize=1048576g" \
    "-client -XX:MaxDirectMemorySize=1048576g" \
    "-XX:MaxDirectMemorySize=1048576g"
do
    # accept the first one that works
    "${JAVA_HOME}/bin/java" $JAVA_COMMAND_ARGS -version > /dev/null 2>&1 && break
done

function setPluginClasspath {

    shopt -s nullglob

    for pluginDir in "${PLUGIN_LIB_DIR}" "${PLUGIN_API_DIR}"
    do
        if [ -d "${pluginDir}" ]
        then
            for jarFile in "${pluginDir}"/*.jar
            do
                PLUGIN_CLASSPATH="${PLUGIN_CLASSPATH}:${jarFile}"
            done

        fi
    done
    #  Adding SLF4j libraries to the classpath of the server to
    #  support services that may use SLF4j for logging
    for jarFile in "${TC_SERVER_DIR}"/lib/slf4j*.jar
    do
        PLUGIN_CLASSPATH="${PLUGIN_CLASSPATH}:${jarFile}"
    done

    shopt -u nullglob

}

setPluginClasspath;

PLUGIN_CLASSPATH="${PLUGIN_CLASSPATH}:${TC_SERVER_DIR}/lib"

#rmi.dgc.server.gcInterval is set an year to avoid system gc in case authentication is enabled
#users may change it accordingly
while [ 1 ] ; do
# the solaris 64-bit JVM has a bug that makes it fail to allocate more than 2GB of offheap when
# the max heap is <= 2G, hence we set the heap size to a bit more than 2GB
    "${JAVA_HOME}/bin/java" $JAVA_COMMAND_ARGS -Xms256m -Xmx2049m -XX:+HeapDumpOnOutOfMemoryError \
        -Dtc.install-root="${TC_SERVER_DIR}" \
        ${JAVA_OPTS} \
        -cp "${TC_SERVER_DIR}/lib/tc.jar:${PLUGIN_CLASSPATH}" \
        com.tc.server.TCServerMain "$@"
    exitValue=$?

    if [ $exitValue -eq 11 ] ; then
        echo "$0: Restarting the server..."
        sleep 1
    else
        exit $exitValue
    fi
done
