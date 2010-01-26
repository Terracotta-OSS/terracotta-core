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
work_dir=${samples_dir}/work
rm -rf $work_dir
lib_dir=${work_dir}/WEB-INF/lib
mkdir -p "${lib_dir}"
cp $samples_dir/../terracotta-session*.jar "${lib_dir}"
cd "${samples_dir}"

for demo in cart tasklist; do
  cp $demo/dist/*.war $work_dir
done

for war in $work_dir/*.war; do
  warfile=$war
  workdir=$work_dir
  if $cygwin; then
    warfile=`cygpath -w $war`
    workdir=`cygpath -w $work_dir`
  fi
  echo "Packaging and deploying `basename $war`"
  $JAVA_HOME/bin/jar uf "${warfile}" -C "${workdir}" WEB-INF/lib
  cp $war $jetty1
  cp $war $jetty2
done

echo "Done"
