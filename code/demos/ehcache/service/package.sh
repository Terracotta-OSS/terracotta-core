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

$JAVA_HOME/bin/javac -d classes -sourcepath src -cp $classpath src/org/terracotta/*.java
if [ $? -ne 0 ]; then 
  echo "Failed to compile demo. Do you have a full kit with Ehcache core?"
  exit 1
fi

mkdir -p dist
rm -rf dist/*
cp -r web/* dist
cp -r classes dist/WEB-INF
mkdir -p dist/WEB-INF/lib

#packaging terracotta-ehcache
cp $tc_install_dir/ehcache/ehcache-terracotta*.jar dist/WEB-INF/lib
if [ $? -ne 0 ]; then
  echo "Couldn't package ehcache-terracotta. Do you have a complete kit?"
  exit 1
fi

#packaging ehcache-core
cp $tc_install_dir/ehcache/ehcache-core*.jar dist/WEB-INF/lib
if [ $? -ne 0 ]; then
  echo "Couldn't package ehcache-core. Do you have a complete kit?"
  exit 1
fi

#packaging slf4j-api
cp $tc_install_dir/ehcache/slf4j-api*.jar dist/WEB-INF/lib
if [ $? -ne 0 ]; then
  echo "Couldn't package slf4j-api. Do you have a complete kit?"
  exit 1
fi

#packaging slf4j-jdk14
cp $tc_install_dir/ehcache/slf4j-jdk14*.jar dist/WEB-INF/lib
if [ $? -ne 0 ]; then
  echo "Couldn't package slf4j-jdk14. Do you have a complete kit?"
  exit 1
fi

#create WAR
warname=Service.war
cd dist
$JAVA_HOME/bin/jar cf $warname *
if [ $? -eq 0 ]; then
  echo "$warname has been created successfully."
  exit 0
else
  echo "Error packaging $warname"
  exit 1
fi
