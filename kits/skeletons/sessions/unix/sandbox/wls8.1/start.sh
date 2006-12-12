#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

# ----------------------------------------
# - start.sh 908{1,2} [nodso] [nowindow]
# ----------------------------------------

cd "`dirname $0`/.."
SANDBOX="`pwd`"
TC_INSTALL_DIR="${SANDBOX}/../.."

if test -z "${TC_JAVA_HOME}" -a -n "${BEA_HOME}" -a -d "${BEA_HOME}/jdk142_11"; then
    TC_JAVA_HOME="${BEA_HOME}/jdk142_11"
    export TC_JAVA_HOME
fi

if test -z "${TC_JAVA_HOME}"; then
    echo "TC_JAVA_HOME must be set to a 1.4 JDK."
    exit 1
fi

JAVA_HOME="${TC_JAVA_HOME}"
export JAVA_HOME

PORT="$1"

if test "$2" != "nodso"; then
    . ../libexec/tc-functions.sh
    tc_install_dir "${TC_INSTALL_DIR}"
    tc_set_dso_boot_jar

    JAVA_OPTIONS="${JAVA_OPTS} -Xbootclasspath/p:${DSO_BOOT_JAR}"
    JAVA_OPTIONS="${JAVA_OPTIONS} -Dtc.install-root=${TC_INSTALL_DIR}"
    JAVA_OPTIONS="${JAVA_OPTIONS} -Dtc.config=${SANDBOX}/wls8.1/tc-config.xml"
    JAVA_OPTIONS="${JAVA_OPTIONS} -Dwebserver.log.name=${PORT}"
    JAVA_OPTIONS="${JAVA_OPTIONS} -Dcom.sun.management.jmxremote"

    export JAVA_OPTIONS
fi

cd "${SANDBOX}/wls8.1/${PORT}"
rm -f SerializedSystemIni.dat
rm -rf myserver
rm -rf applications/.wlnotdelete
cp tmpls/* .

exec ../startWLS.sh
