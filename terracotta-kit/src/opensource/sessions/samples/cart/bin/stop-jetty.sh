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

unset CDPATH
root=`dirname $0`/..
root=`cd $root && pwd`

jetty_work_dir=$root/jetty6.1/$jetty_instance
jetty_home=$root/../../../third-party/jetty-6.1.15
start_jar=$jetty_home/start.jar
stop_port=$((jetty_instance + 2))

cd $jetty_work_dir
if $cygwin; then
  jetty_work_dir=`cygpath -w $jetty_work_dir`
  jetty_home=`cygpath -w $jetty_home`
  start_jar=`cygpath -w $start_jar`
fi

echo "Stopping Jetty $jetty_instance..."
$JAVA_HOME/bin/java -Djetty.home=$jetty_home -DSTOP.PORT=$stop_port -DSTOP.KEY=secret\
 -jar $start_jar --stop

