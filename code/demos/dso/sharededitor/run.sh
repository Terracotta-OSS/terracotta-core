#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

TOPDIR=`dirname "$0"`/../..
. "${TOPDIR}"/libexec/tc-functions.sh

TC_CONFIG_PATH="tc-config.xml"
. "${TOPDIR}"/bin/dso-env.sh

tc_install_dir "${TOPDIR}"/.. true
tc_java ${TC_JAVA_OPTS} -cp "classes" "-Djava.awt.Window.locationByPlatform=true" demo.sharededitor.Main "$@"
