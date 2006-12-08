#!/bin/sh

#@COPYRIGHT@

TOPDIR=`dirname "$0"`/..
test "$1" = "-v" && shift && __DSO_ENV_VERBOSE="true"

. "${TC_INSTALL_DIR:-${TOPDIR}}"/libexec/tc-functions.sh
__DSO_ENV_CONFIG=$1

tc_install_dir "${TOPDIR}"/.. true
tc_config "${__DSO_ENV_CONFIG}"
tc_set_dso_boot_jar
 
test -z "${__DSO_ENV_CONFIG}" && unset D_TC_CONFIG
TC_JAVA_OPTS="-Xbootclasspath/p:${DSO_BOOT_JAR} -Dtc.install-root=${TC_INSTALL_DIR} ${D_TC_CONFIG}"

test -n ${__DSO_ENV_VERBOSE} && echo ${TC_JAVA_OPTS}
