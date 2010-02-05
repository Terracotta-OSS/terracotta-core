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

$root/bin/package.sh

$root/../bin/start-jetty.sh 9081
sleep 3
$root/../bin/start-jetty.sh 9082
 