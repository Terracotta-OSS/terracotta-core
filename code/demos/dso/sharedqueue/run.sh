#!/bin/sh

#@COPYRIGHT@

TOPDIR=`dirname "$0"`/../..
. "${TOPDIR}"/libexec/tc-functions.sh

TC_JAVA_OPTS="`./"${TOPDIR}"/bin/dso-env.sh tc-config.xml`"

tc_install_dir "${TOPDIR}"/.. true
tc_java ${TC_JAVA_OPTS} -cp "classes:lib/org.mortbay.jetty-4.2.20.jar:lib/javax.servlet.jar" demo.sharedqueue.Main "$@"
