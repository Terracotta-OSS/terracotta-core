#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

TOPDIR=`dirname "$0"`/../../..
. "${TOPDIR}"/bin/tc-functions.sh

tc_install_dir "${TOPDIR}" true

JAVA_HOME="${TC_JAVA_HOME:-${TOPDIR}/jre}"
CATALINA_HOME="${TOPDIR}/vendors/tomcat5.5"
CATALINA_BASE="tomcat1"
export JAVA_HOME CATALINA_HOME CATALINA_BASE

exec "${CATALINA_HOME}/bin/catalina.sh" stop
