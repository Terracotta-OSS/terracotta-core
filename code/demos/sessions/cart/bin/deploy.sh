#!/bin/bash

cygwin=false
if [ `uname | grep GYGWIN` ]; then
  cygwin=true
fi

if [ "$JAVA_HOME" = "" ]; then
  echo "JAVA_HOME is not defined"
  exit 1
fi

root=`dirname $0`/..
root=`cd $root && pwd`
dist=dist
classes=classes
web=web
warfile=Cart.war
jetty1=$root/../jetty6.1/9081/webapps
jetty2=$root/../jetty6.1/9082/webapps

cd $root
echo "Cleaning dist folder..."
rm -rf $dist
mkdir $dist

echo "Packaging WAR file..."
cp -r $web/* $dist
#ensure lib folder
mkdir -p $dist/WEB-INF/lib
cp $root/../../terracotta-session*.jar $dist/WEB-INF/lib
cp -r $classes $dist/WEB-INF
cd $dist
$JAVA_HOME/bin/jar cf $warfile *

if [ ! -f Cart.war ]; then
  echo "Packaging failed"
  exit 1
else
  echo "Packaging succeeded"
fi

echo "Deploying $warfile to Jetty"
cp $warfile $jetty1
cp $warfile $jetty2
echo "Done"
