#!/bin/bash

cygwin=false
if [ `uname | grep CYGWIN` ]; then
  cygwin=true
fi

if [ "$JAVA_HOME" = "" ]; then
  echo "JAVA_HOME is not defined"
  exit 1
fi

root=`dirname $0`/..
root=`cd $root && pwd`
h2_jar=$root/src/main/webapp/WEB-INF/lib/h2-1.1.116.jar

if $cygwin; then
  h2_jar=`cygpath -w -p $h2_jar`
fi

$JAVA_HOME/bin/java -cp $h2_jar org.h2.tools.Server -tcp -tcpAllowOthers&
