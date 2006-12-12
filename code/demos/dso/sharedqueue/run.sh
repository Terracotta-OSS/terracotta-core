#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

TOPDIR=`dirname "$0"`/../..
. "${TOPDIR}"/libexec/tc-functions.sh
. "${TOPDIR}"/bin/dso-env.sh -q tc-config.xml

tc_install_dir "${TOPDIR}"/.. true
tc_java ${TC_JAVA_OPTS} -cp "classes:lib/org.mortbay.jetty-4.2.20.jar:lib/javax.servlet.jar" demo.sharedqueue.Main "$@"
