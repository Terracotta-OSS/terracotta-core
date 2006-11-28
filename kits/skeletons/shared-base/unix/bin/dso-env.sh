#!/bin/sh

#@COPYRIGHT@

TOPDIR=`dirname "$0"`/..
. "${TC_INSTALL_DIR:-${TOPDIR}}"/libexec/tc-functions.sh
__CONFIG=$1

tc_install_dir "${TOPDIR}"/.. true
tc_config "${__CONFIG}"
tc_set_dso_boot_jar
 
test -z "${__CONFIG}" && unset D_TC_CONFIG
TC_JAVA_OPTS="-Xbootclasspath/p:${DSO_BOOT_JAR} -Dtc.install-root=${TC_INSTALL_DIR} ${D_TC_CONFIG}"

echo ${TC_JAVA_OPTS}
