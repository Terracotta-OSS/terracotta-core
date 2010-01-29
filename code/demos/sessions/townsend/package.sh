#!/bin/bash

cygwin=false
if [ `uname | grep CYGWIN` ]; then
  cygwin=true
fi

if [ "$JAVA_HOME" = "" ]; then
  echo "JAVA_HOME is not defined"
  exit 1
fi

root=`dirname $0`
cd $root
tc_install_dir=../../../
mkdir -p dist
rm -rf dist/*
cp -r web/* dist
cp -r classes dist/WEB-INF
cp images/* dist
mkdir -p dist/WEB-INF/lib

#packaging echcache-core
cp $tc_install_dir/ehcache/ehcache-core*.jar dist/WEB-INF/lib
if [ $? -ne 0 ]; then
  echo "Couldn't package ehcache-core. Do you have a complete kit?"
  exit 1
fi

#packaging ehcache-terracotta
cp $tc_install_dir/ehcache/ehcache-terracotta*.jar dist/WEB-INF/lib
if [ $? -ne 0 ]; then
  echo "Couldn't package ehcache-terracotta. Do you have a complete kit?"
  exit 1
fi

#packaging terracotta-session
cp $tc_install_dir/sessions/terracotta-session*.jar dist/WEB-INF/lib
if [ $? -ne 0 ]; then
  echo "Couldn't package terracotta-session. Do you have a complete kit?"
  exit 1
fi

#create WAR
warname=Townsend.war
cd dist
$JAVA_HOME/bin/jar cf $warname *
if [ $? -eq 0 ]; then
  echo "$warname has been created successfully."
  exit 0
else
  echo "Error packaging $warname"
  exit 1
fi
