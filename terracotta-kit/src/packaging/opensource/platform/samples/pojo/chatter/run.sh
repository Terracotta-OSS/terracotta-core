#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

#
# samples/pojos/chatter
#
# Environment variables required by dso-env helper script:
#  JAVA_HOME: root of Java Development Kit installation
#  TC_INSTALL_DIR: root of Terracotta installation
#
# Arguments to dso-env helper script:
#  -q: do not print value of TC_JAVA_OPTS
#  TC_CONFIG_PATH: location of Terracotta config file; overridden by value
#                  of optional TC_CONFIG
#
# Environment variable set by dso-env helper script:
#  TC_JAVA_OPTS: Java options needed to activate DSO
#

CWD=`dirname "$0"`
TC_INSTALL_DIR=${CWD}/../../../..
ARGS=$*
TC_CONFIG_PATH="${CWD}/tc-config.xml"
set -- -q
. "${TC_INSTALL_DIR}/platform/bin/dso-env.sh"

exec "${JAVA_HOME}/bin/java" ${TC_JAVA_OPTS} \
  -Djava.awt.Window.locationByPlatform=true \
  ${JAVA_OPTS} \
  -cp "${CWD}/classes" demo.chatter.Main $ARGS
