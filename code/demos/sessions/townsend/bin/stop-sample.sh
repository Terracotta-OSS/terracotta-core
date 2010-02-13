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

$root/bin/stop-jetty.sh 9081
$root/bin/stop-jetty.sh 9082
 