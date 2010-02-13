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
cd $root
tc_install_dir=../../../
jetty1=$root/jetty6.1/9081/webapps
jetty2=$root/jetty6.1/9082/webapps
mkdir -p dist
rm -rf dist/*
cp -r web/* dist
cp -r classes dist/WEB-INF
mkdir -p dist/WEB-INF/lib

#packaging terracotta-session
cp $tc_install_dir/sessions/terracotta-session*.jar dist/WEB-INF/lib
if [ $? -ne 0 ]; then
  echo "Couldn't package terracotta-session. Do you have a complete kit?"
  exit 1
fi

#create WAR
warname=DepartmentTaskList.war
cd dist
$JAVA_HOME/bin/jar cf $warname *
if [ $? -eq 0 ]; then
  echo "$warname has been created successfully. Deploying..."
  cp $warname $jetty1
  cp $warname $jetty2
  echo "Done."
  exit 0
else
  echo "Error packaging $warname"
  exit 1
fi
