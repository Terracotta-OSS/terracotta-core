#!/bin/sh

#@COPYRIGHT@

TOPDIR=`dirname "$0"`/../..
. "${TOPDIR}"/libexec/tc-functions.sh

tc_install_dir "${TOPDIR}"/.. true

JAVA_HOME="${TC_JAVA_HOME:-${TOPDIR}/jre}"
CATALINA_HOME="${TOPDIR}/../vendors/tomcat5.5"
CATALINA_BASE="tomcat3"
export JAVA_HOME CATALINA_HOME CATALINA_BASE

exec "${CATALINA_HOME}/bin/catalina.sh" stop
