#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

TOPDIR=`dirname "$0"`/../../..
. "${TOPDIR}"/bin/tc-functions.sh

tc_install_dir "${TOPDIR}" true
tc_java -classpath "${TC_INSTALL_DIR}/lib/tc.jar" -Ddaemon=true com.tc.net.proxy.TCPProxy 8080 localhost:8081,localhost:8082,localhost:8083
