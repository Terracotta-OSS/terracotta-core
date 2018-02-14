#!/bin/sh
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


# this will only happen if using sag installer
if [ -r "${TC_VOTER_DIR}/bin/setenv.sh" ] ; then
  . "${TC_VOTER_DIR}/bin/setenv.sh"
fi

if ! [ -d "${JAVA_HOME}" ]; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

TC_KIT_ROOT=$(dirname "$TC_VOTER_DIR")
TC_LOGGING_ROOT=$TC_KIT_ROOT/client/logging
TC_CLIENT_ROOT=$TC_KIT_ROOT/client/lib

CLASS_PATH="${TC_VOTER_DIR}/lib/*:${TC_CLIENT_ROOT}/*:${TC_LOGGING_ROOT}/*:${TC_LOGGING_ROOT}/impl/*:${TC_LOGGING_ROOT}/impl/"

"$JAVA_HOME/bin/java" ${JAVA_OPTS} -cp "$CLASS_PATH" $TC_VOTER_MAIN "$@"