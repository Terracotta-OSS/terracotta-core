###########################################################################################
##
## Supporting functions
##
###########################################################################################

function _stdout() {
    echo "$@"
}

function _stderr() {
    echo 1>&2 "$@"
}

function _info() {
    _stdout ' [info] : ' "$@"
}

function _warn() {
    _stderr ' [warn] : ' "$@"
}

function _error() {
    _stderr '[error] : ' "$@"
}

function _executable() {
    test -r "${1}" -a -x "${1}"
    return $?
}

function _validateWasHome() {
    _info validating the sandbox environment...
    if test -z "${WAS_SANDBOX}"; then
        _warn WAS_SANDBOX not defined
        return 1
    fi
    _info validating the WAS_HOME environment...
    if test -z "${WAS_HOME}"; then
        _warn WAS_HOME not defined
        return 1
    fi
    _was_bin="startServer.sh stopServer.sh wsadmin.sh manageprofiles.sh"
    for bin in ${_was_bin}; do
        if ! _executable "${WAS_HOME}/bin/${bin}"; then
            _warn unable to locate "\${WAS_HOME}/bin/${bin}"
        fi
    done
}

function _createProfile() {
    "${WAS_HOME}/bin/manageprofiles.sh" -listProfiles | grep -q "tc-${1}"
    if test "$?" != "0"; then
        _info creating profile "tc-${1}" 'for' port "${1}"...
        _info
        _info "	==> THIS CAN TAKE A LONG TIME SO PLEASE BE PATIENT <=="
        _info
        if ! "${WAS_HOME}/bin/manageprofiles.sh" -create -templatePath "${WAS_HOME}/profileTemplates/default" -portsFile "${WAS_SANDBOX}/profiles/${1}.port-defs" -profileName "tc-${1}" -enableAdminSecurity false -isDeveloperServer; then
            _warn unable to create profile "tc-${1}" 'for' port "${1}"
            return 1
        fi
    else
        _info WebSphere profile "tc-${1}" already exists, skipping profile creation 'for' port "${1}"
    fi
}

function _runWsAdmin() {
    "${WAS_HOME}/bin/wsadmin.sh" -lang jython "$@"
    return $?
}

function _addTerracottaToPolicy() {
    echo "${DSO_BOOT_JAR}" | grep -q "${WAS_HOME}/profiles/tc-${1}/properties/server.policy"
    if test "$?" != "0"; then
        _info adding Terracotta codebase to server.policy in profile "tc-${1}" 'for' port "${1}"...
        cat << __EOF__ >> "${WAS_HOME}/profiles/tc-${1}/properties/server.policy"

grant codeBase "file:${TC_INSTALL_DIR}/lib/-" {
      permission java.security.AllPermission;
};

grant codeBase "file:${TC_INSTALL_DIR}/lib/dso-boot/-" {
      permission java.security.AllPermission;
};
__EOF__
        return $?
    fi
}

function _startWebSphere() {
    if test "${2}" != "nodso"; then
        # Instrument WebSphere for use with Terracotta
        _addTerracottaToPolicy "${1}"
        __retVal="$?"
        if test "${__retVal}" != "0"; then
            _error unable to modify server.policy settings to grant Terracotta code privileges
            return "${__retVal}"
        fi
        _runWsAdmin -connType NONE -profileName "tc-${1}" -f "${WAS_SANDBOX}/toggle-dso.py" "true"
        __retVal="$?"
        if test "${__retVal}" != "0"; then
            return "${__retVal}"
        fi
    else
        # Make sure DSO is not enabled in WebSphere
        _runWsAdmin -connType NONE -profileName "tc-${1}" -f "${WAS_SANDBOX}/toggle-dso.py" "false"
        __retVal="$?"
        if test "${__retVal}" != "0"; then
            return "${__retVal}"
        fi
    fi
    "${WAS_HOME}/bin/startServer.sh" server1 -profileName "tc-${1}"
    return $?
}

function _stopWebSphere() {
    "${WAS_HOME}/bin/stopServer.sh" server1 -profileName "tc-${1}"
    return $?
}
