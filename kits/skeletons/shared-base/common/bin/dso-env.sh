#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
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
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath --unix "$TC_INSTALL_DIR"`
fi

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

if test "$1" = "-q" || test -n "${TC_INSTALL_DIR}"; then
  if test -z "${TC_INSTALL_DIR}"; then
    echo "Error: When the -q option is specified, I expect that"
    echo "the environment variable TC_INSTALL_DIR is set so that I"
    echo "can locate your Terracotta installation."
    exit 1
  fi
  if test "$1" = "-q"; then
    shift
    __DSO_ENV_QUIET="true"
  fi
else
  TC_INSTALL_DIR=`dirname "$0"`/..
fi

if test -n "$1"; then
  TC_CONFIG_PATH="$1"
fi

. "${TC_INSTALL_DIR}/bin/boot-jar-path.sh"

# For Cygwin, convert paths back to Windows
if $cygwin; then
  [ -n "$DSO_BOOT_JAR" ] && DSO_BOOT_JAR=`cygpath --windows "$DSO_BOOT_JAR"`
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath --windows "$TC_INSTALL_DIR"`
  [ -n "$TC_CONFIG_PATH" ] && TC_CONFIG_PATH=`cygpath --windows "$TC_CONFIG_PATH"`
fi

TC_JAVA_OPTS="-Xbootclasspath/p:${DSO_BOOT_JAR} \
 -Dtc.install-root=${TC_INSTALL_DIR} \
 -Dtc.config=${TC_CONFIG_PATH}"
 
test "${TC_SERVER}" && TC_JAVA_OPTS="${TC_JAVA_OPTS} -Dtc.server=${TC_SERVER}"
test -z "${__DSO_ENV_QUIET}" && echo "${TC_JAVA_OPTS}"
