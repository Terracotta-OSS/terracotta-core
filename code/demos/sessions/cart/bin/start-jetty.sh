#!/bin/bash

cygwin=false
if [ `uname | grep CYGWIN` ]; then
  cygwin=true
fi

if [ "$JAVA_HOME" = "" ]; then
  echo "JAVA_HOME is not defined"
  exit 1
fi

jetty_instance=$1

if [ "$jetty_instance" = "" ]; then
  echo "Need to specify which instance of Jetty: 9081 or 9082"
  exit 1
fi

root=`dirname $0`/..
root=`cd $root && pwd`
warfile=Cart.war
jetty_work_dir=$root/../jetty6.1/$jetty_instance
jetty_home=$root/../../../vendors/jetty-6.1.15
start_jar=$jetty_home/start.jar
stop_port=$((jetty_instance + 2))

cd $root

#Checking if $warfile has been deployed...
if [ -f $jetty_work_dir/webapps/$warfile ]; then
  echo "$warfile has been deployed"
else
  echo "$warfile has not been deployed. Deploying it now..."
  bin/deploy.sh
  if [ $? -ne 0 ]; then
    exit 1
  fi
fi

cd $jetty_work_dir
if $cygwin; then
  jetty_work_dir=`cygpath -w $jetty_work_dir`
  jetty_home=`cygpath -w $jetty_home`
  start_jar=`cygpath -w $start_jar`
fi

echo "starting Jetty $jetty_instance..."
$JAVA_HOME/bin/java -Djetty.home=$jetty_home -DSTOP.PORT=$stop_port -DSTOP.KEY=secret\
 -jar $start_jar conf.xml &
sleep 1
echo
