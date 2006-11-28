#!/bin/sh

#@COPYRIGHT@

TOPDIR=`dirname "$0"`/../..
. "${TOPDIR}"/libexec/tc-functions.sh

tc_install_dir "${TOPDIR}"/.. true
tc_java -classpath "${TC_INSTALL_DIR}/common/lib/tc.jar" -Ddaemon=true com.tc.net.proxy.TCPProxy 8080 localhost:8081,localhost:8082,localhost:8083
