#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

CWD=`dirname "$0"`
TC_INSTALL_DIR=${CWD}/../../..

CATALINA_HOME="${TC_INSTALL_DIR}/vendors/tomcat5.5"
CATALINA_BASE="tomcat1"
export JAVA_HOME CATALINA_HOME CATALINA_BASE

exec "${CATALINA_HOME}/bin/catalina.sh" stop
