#!/bin/bash

# 
# The contents of this file are subject to the Terracotta Public License Version
# 2.0 (the "License"); You may not use this file except in compliance with the
# License. You may obtain a copy of the License at 
# 
#      http://terracotta.org/legal/terracotta-public-license.
# 
# Software distributed under the License is distributed on an "AS IS" basis,
# WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
# the specific language governing rights and limitations under the License.
# 
# The Covered Software is Terracotta Platform.
# 
# The Initial Developer of the Covered Software is 
#     Terracotta, Inc., a Software AG company
#

#This script can not run on its own, it must be invoked by usermanagement.sh, keychain.sh or rest-client.sh
if [ "$cli_name" = "" ]; then
  echo "This script cannot be called directly, you should use usermanagement.sh, keychain.sh or rest-client.sh"
  exit 1
fi

#cygwin=false
if [ `uname | grep CYGWIN` ]; then
 # cygwin=true
  echo "cygwin shell is not supported, please use the corresponding .bat script "
  echo "(for example keychain.bat in place of keychain.sh) from a Windows cmd.exe terminal."
  exit 1
fi

THIS_DIR=`dirname $0`
TC_INSTALL_DIR=`cd $THIS_DIR;pwd`/../../..
COMMAND_MANAGER=${commandmanager}

if [ -r "$TC_INSTALL_DIR"/server/bin/setenv.sh ] ; then
  . "$TC_INSTALL_DIR"/server/bin/setenv.sh
fi

if [ "$JAVA_HOME" = "" ]; then
  echo "JAVA_HOME is not defined"
  exit 1
fi

unset CDPATH
root=`dirname $0`/..
cli_runner=$root/lib/${management_cli_project}-${management-cli.version}.jar
#support for terracotta kit
if [ ! -f $cli_runner ]
then
    cli_runner=$root/lib/${management_cli_project}/${management_cli_project}-${management-cli.version}.jar
fi
edg_opt=
tmpdir_opt=

#if $cygwin; then
#  cli_runner=`cygpath -w $cli_runner`
#fi

#if [ `uname | grep Darwin` ]; then
#  tmpdir_opt=-Djava.io.tmpdir=/tmp
#fi

java_opts=""

if [[ "${cli_name}" != "__hidden__" ]]; then
  echo Terracotta Command Line Tools - $cli_name
fi

"$JAVA_HOME"/bin/java -Dmanager=$COMMAND_MANAGER -Xmx256m -XX:MaxPermSize=128m \
 $java_opts \
 -cp $cli_runner $main_class "$@"
exit_code=$?
#sleep 1
echo
exit $exit_code