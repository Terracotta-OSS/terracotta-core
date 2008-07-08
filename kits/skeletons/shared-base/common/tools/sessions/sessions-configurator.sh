#!/bin/sh

#
#  All content copyright (c) 2003-2008 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

TC_INSTALL_DIR=`dirname "$0"`/../..
CP=$TC_INSTALL_DIR/lib/tc.jar
SVT_JAR=`find $TC_INSTALL_DIR/lib -name "svt*.jar" | tail -1`
if [ -f "$SVT_JAR" ]; then  
  CP=$CP:$SVT_JAR  
fi

# For Cygwin, convert paths to Windows
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --windows "$JAVA_HOME"`
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath --windows "$TC_INSTALL_DIR"`
  [ -n "$CP" ] && CP=`cygpath -w -p $CP`
fi

"${JAVA_HOME}/bin/java" \
  -Dtc.install-root="${TC_INSTALL_DIR}" \
  ${JAVA_OPTS} \
  -cp "$CP" \
  org.terracotta.ui.session.SessionIntegrator "$@"
