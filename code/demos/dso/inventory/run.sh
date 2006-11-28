#!/bin/sh

#@COPYRIGHT@

TOPDIR=`dirname "$0"`/../..
. "${TOPDIR}"/libexec/tc-functions.sh

TC_JAVA_OPTS="`./"${TOPDIR}"/bin/dso-env.sh tc-config.xml`"

tc_install_dir "${TOPDIR}"/.. true
tc_java ${TC_JAVA_OPTS} -cp "classes" demo.inventory.Main "$@"
