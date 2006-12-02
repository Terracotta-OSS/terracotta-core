#!/bin/sh

#@COPYRIGHT@

TOPDIR=`dirname "$0"`/../..
. "${TOPDIR}"/libexec/tc-functions.sh

TC_JAVA_OPTS="`./"${TOPDIR}"/bin/dso-env.sh tc-config.xml`"
if [ $? -ne 0 ]; then
  echo $TC_JAVA_OPTS
  exit 1
fi

tc_install_dir "${TOPDIR}"/.. true
JAVA_OPTS="${TC_JAVA_OPTS} -Dcounter.log.prefix=CounterService-Tomcat-Node-2:"
export JAVA_OPTS

JAVA_HOME="${TC_JAVA_HOME:-${TOPDIR}/jre}"
CATALINA_HOME="${TOPDIR}/../vendors/tomcat5.5"
CATALINA_BASE="tomcat2"
export JAVA_HOME CATALINA_HOME CATALINA_BASE

mkdir -p "${CATALINA_BASE}/logs" "${CATALINA_BASE}/temp"

# To run Tomcat in a debugger you can use the following options
# JPDA_TRANSPORT=dt_socket
# JPDA_ADDRESS=8096
# export JPDA_TRANSPORT JPDA_ADDRESS
# exec "${CATALINA_HOME}/bin/catalina.sh" jpda run

exec "${CATALINA_HOME}/bin/catalina.sh" run
