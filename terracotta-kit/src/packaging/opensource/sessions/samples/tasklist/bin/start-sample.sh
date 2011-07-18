#!/bin/bash

cygwin=false
if [ `uname | grep CYGWIN` ]; then
  cygwin=true
fi

if [ "$JAVA_HOME" = "" ]; then
  echo "JAVA_HOME is not defined"
  exit 1
fi

unset CDPATH
root=`dirname $0`/..
root=`cd $root && pwd`

$root/bin/package.sh

if [ $? -ne 0 ]; then 
  exit 1
fi

$root/bin/start-jetty.sh 9081
echo "Go to: http://localhost:9081/DepartmentTaskList"
echo

sleep 3

$root/bin/start-jetty.sh 9082
echo "Go to: http://localhost:9082/DepartmentTaskList"
echo
 
