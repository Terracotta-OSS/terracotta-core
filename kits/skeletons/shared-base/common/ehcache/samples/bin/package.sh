#!/bin/bash

cygwin=false
if [ `uname | grep CYGWIN` ]; then
  cygwin=true
fi

if [ "$JAVA_HOME" = "" ]; then
  echo "JAVA_HOME is not defined"
  exit 1
fi

samples_dir=`dirname $0`/..
samples_dir=`cd $samples_dir && pwd`
jetty1=$samples_dir/jetty6.1/9081/webapps
jetty2=$samples_dir/jetty6.1/9082/webapps

cd "${samples_dir}"

for demo in colorcache; do
  $demo/package.sh
  cp $demo/dist/*.war $jetty1
  cp $demo/dist/*.war $jetty2
done
