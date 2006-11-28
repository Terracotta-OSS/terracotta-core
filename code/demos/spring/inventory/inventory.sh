#!/bin/sh

#@COPYRIGHT@

cd `dirname $0`
TOPDIR=`pwd`/../..
. ${TOPDIR}/libexec/tc-functions.sh

cp=lib/commons-logging-1.0.4.jar
cp=${cp};lib/spring-1.2.8.jar
cp=${cp};target/inventory.jar

tc_install_dir ${TOPDIR}/.. true
tc_classpath "${cp}" false
tc_java_opts ""
tc_config "tc-config.xml"

run_dso_java -classpath "${TC_CLASSPATH}" "${D_TC_CONFIG}" ${TC_ALL_JAVA_OPTS} demo.jtable.TableDemo "$@"
