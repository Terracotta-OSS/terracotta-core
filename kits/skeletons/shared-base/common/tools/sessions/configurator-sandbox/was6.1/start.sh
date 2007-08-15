#! /bin/sh

###########################################################################################
##
## Main program
##
##    Arguments: [-debug] <port> [nodso]
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
. "${TC_INSTALL_DIR}"/bin/dso-env.sh
export WAS_SANDBOX
export TC_INSTALL_DIR
export TC_CONFIG_PATH
export DSO_BOOT_JAR

. "${WAS_SANDBOX}/websphere-common.sh"

if ! _validateWasHome; then
    _error WAS_HOME must point to a valid WebSphere Application Server 6.1 installation
    exit 1
fi

if ! _createProfile "${port}"; then
    _error Unable to create a profile 'for' port "${port}" to run the Terracotta Configurator
    exit 1
fi

_runWsAdmin -connType NONE -profileName "tc-${port}" -javaoption -Dwebapp.dir="${WAS_SANDBOX}/${port}/webapps" -f "${WAS_SANDBOX}/deployApps.py"
if test "$?" != "0"; then
    _error unable to deploy web applications to WebSphere Application Server on port "${port}"
    exit 1
else
    _info starting WebSphere Application Server on port "${port}"...
    _startWebSphere "${port}" "${2}"
    if test "$?" != "0"; then
        _error unable to start WebSphere Application Server on port "${port}"
        _stopWebSphere "${port}"
        exit 1
    else
        _runWsAdmin -profileName "tc-${port}" -javaoption -DprofileName="tc-${port}" -f "${WAS_SANDBOX}/wait-for-shutdown.py"
        exit $?
    fi
fi
