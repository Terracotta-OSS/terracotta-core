#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

if test "$1" = "-q"; then
  if test -z "${TOPDIR}"; then
    echo "Error: When the -q option is specified, I expect that"
    echo "the environment variable TOPDIR is set so that I"
    echo "can locate the libexec directory of your Terracotta"
    echo "software installation."
    exit 1
  fi
  shift
  . "${TOPDIR}"/libexec/tc-functions.sh
  __DSO_ENV_QUIET="true"
else
  TOPDIR=`dirname "$0"`/..
  . "${TC_INSTALL_DIR:-${TOPDIR}}"/libexec/tc-functions.sh
fi
__DSO_ENV_CONFIG=$1

tc_install_dir "${TOPDIR}"/.. true
tc_config "${__DSO_ENV_CONFIG}"
tc_set_dso_boot_jar

test -z "${__DSO_ENV_CONFIG}" && unset D_TC_CONFIG
TC_JAVA_OPTS="-Xbootclasspath/p:${DSO_BOOT_JAR} -Dtc.install-root=${TC_INSTALL_DIR} ${D_TC_CONFIG}"

test -z "${__DSO_ENV_QUIET}" && echo "${TC_JAVA_OPTS}"
