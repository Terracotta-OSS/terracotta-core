#! /bin/sh

#
#  All content copyright (c) 2003-2008 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

###########################################################################################
##
## Main program
##
##    Arguments: [-debug] <port>
##
###########################################################################################

if test "${1}" = "-debug"; then
    shift
    set -x
fi

port="${1:?You must specify a port as the first argument}"

starting_dir="`pwd`"
cd "`dirname $0`"
WAS_SANDBOX="`pwd`"
cd ../../../..
TC_INSTALL_DIR="`pwd`"
cd "${starting_dir}"

TC_CONFIG_PATH="${WAS_SANDBOX}/tc-config.xml"
set -- -q "${TC_CONFIG_PATH}"
. "${TC_INSTALL_DIR}/bin/dso-env.sh"

export TC_INSTALL_DIR
export TC_CONFIG_PATH
export DSO_BOOT_JAR

. "${WAS_SANDBOX}/websphere-common.sh"

if ! _validateWasHome; then
    _error WAS_HOME must point to a valid WebSphere Application Server 6.1 installation
    exit 1
fi

_info stopping WebSphere Application Server on port "${port}"...
_stopWebSphere "${port}"
exit $?
