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
jetty1=$root/../jetty6.1/9081/webapps
jetty2=$root/../jetty6.1/9082/webapps

mkdir -p classes

ehcache_core=`\ls -1 ../../../ehcache/ehcache-core-*.jar | grep -v "sources" | grep -v "javadoc" | head -1`
if [ ! -f $ehcache_core ]; then
  echo "Couldn't find ehcache-core jar. Do you have a full kit?"
  exit 1
fi
classpath=classes:$tc_install_dir/lib/servlet-api-2.5-6.1.8.jar:$ehcache_core
for jar in web/WEB-INF/lib/*.jar; do
  classpath=$classpath:$jar
done

if $cygwin; then
  classpath=`cygpath -w -p $classpath`
fi

$JAVA_HOME/bin/javac -Xlint:unchecked -d classes -sourcepath src -cp $classpath src/demo/townsend/service/*.java src/demo/townsend/common/*.java src/demo/townsend/form/*.java src/demo/townsend/action/*.java
if [ $? -ne 0 ]; then 
  echo "Failed to compile demo. Do you have a full kit with Ehcache core?"
  exit 1
fi

mkdir -p dist
rm -rf dist/*
cp -r web/* dist
cp -r classes dist/WEB-INF
cp images/* dist
mkdir -p dist/WEB-INF/lib

#packaging echcache-core
cp $ehcache_core dist/WEB-INF/lib
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
  echo "$warname has been created successfully. Deploying..."
  cp $warname $jetty1
  cp $warname $jetty2
  echo "Done."
else
  echo "Error packaging $warname"
  exit 1
fi
