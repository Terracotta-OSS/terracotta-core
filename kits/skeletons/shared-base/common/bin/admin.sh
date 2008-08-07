#!/bin/sh

#
#  All content copyright (c) 2003-2008 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

TC_INSTALL_DIR=`dirname "$0"`/..
CP=$TC_INSTALL_DIR/lib/tc.jar
SVT_JAR=`find $TC_INSTALL_DIR/lib -name "svt*.jar" | tail -1`
if [ -f "$SVT_JAR" ]; then  
  CP="$SVT_JAR:$CP"
fi

# For Cygwin, convert paths to Windows
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --windows "$JAVA_HOME"`
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath --windows "$TC_INSTALL_DIR"`
  [ -n "$CP" ] && CP=`cygpath -w -p $CP`
fi

JAVA_OPTS="-Xmx128m -Dtc.install-root=${TC_INSTALL_DIR} $JAVA_OPTS"
exec "${JAVA_HOME}/bin/java" \
  ${JAVA_OPTS} \
  -cp "$CP" \
  com.tc.admin.AdminClient "$@"
