#!/bin/sh

#@COPYRIGHT@

TOPDIR=`dirname $0`/../..
. ${TC_INSTALL_DIR:-${TOPDIR}}/libexec/tc-functions.sh

tc_install_dir ${TOPDIR}/.. true
tc_classpath "`dirname $0`/classes:`dirname $0`/lib/servlet-api.jar:`dirname $0`/lib/commonj-twm.jar:`dirname $0`/lib/commons-logging-1.0.4.jar:`dirname $0`/lib/spring-2.0.jar" false
tc_java_opts ""
tc_config "`dirname $0`/tc-config.xml"

run_dso_java -classpath "${TC_CLASSPATH}" "${D_TC_CONFIG}" ${TC_ALL_JAVA_OPTS} demo.workmanager.Main worker "$@"
