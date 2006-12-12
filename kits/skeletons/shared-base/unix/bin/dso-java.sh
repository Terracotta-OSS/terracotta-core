#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

TOPDIR=`dirname "$0"`/..
. "${TOPDIR}"/libexec/tc-functions.sh
tc_install_dir "${TOPDIR}"/.. true
tc_classpath "" false
tc_java_opts ""
tc_set_dso_boot_jar

if test $# -eq 0; then
	tc_java | sed -e "s| java | `basename $0` |"
	exit 1
fi

tc_java "-Xbootclasspath/p:${DSO_BOOT_JAR}" "-Dtc.install-root=${TC_INSTALL_DIR}" "$@"
