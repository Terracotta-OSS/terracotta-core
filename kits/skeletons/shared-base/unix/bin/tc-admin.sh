#!/bin/sh

#@COPYRIGHT@

TOPDIR=`dirname "$0"`/..
. "${TOPDIR}"/libexec/tc-functions.sh

tc_install_dir "${TOPDIR}"/..
tc_classpath "" true
tc_java_opts ""

tc_java -classpath "${TC_CLASSPATH}" -Dtc.install-root="${TC_INSTALL_DIR}" ${TC_ALL_JAVA_OPTS} com.tc.admin.AdminClient "$@"
