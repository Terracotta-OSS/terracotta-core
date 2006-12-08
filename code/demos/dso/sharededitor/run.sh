#!/bin/sh

#@COPYRIGHT@

TOPDIR=`dirname "$0"`/../..
. "${TOPDIR}"/libexec/tc-functions.sh

TC_JAVA_OPTS="`./"${TOPDIR}"/bin/dso-env.sh -v tc-config.xml`"
if [ $? -ne 0 ]; then
  echo $TC_JAVA_OPTS
  exit 1
fi

tc_install_dir "${TOPDIR}"/.. true
tc_java ${TC_JAVA_OPTS} -cp "classes" "-Djava.awt.Window.locationByPlatform=true" demo.sharededitor.Main "$@"
