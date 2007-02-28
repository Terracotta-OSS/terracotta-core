#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

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
  test "$1" = "-q" && shift
  __DSO_ENV_QUIET="true"
else
  TC_INSTALL_DIR=`dirname "$0"`/..
fi

if test -n "$1"; then
  TC_CONFIG_PATH="$1"
fi

. "${TC_INSTALL_DIR}/bin/boot-jar-path.sh"

TC_JAVA_OPTS="-Xbootclasspath/p:${DSO_BOOT_JAR} \
 -Dtc.install-root=${TC_INSTALL_DIR} \
 -Dtc.config=${TC_CONFIG_PATH}"

test -z "${__DSO_ENV_QUIET}" && echo "${TC_JAVA_OPTS}"
