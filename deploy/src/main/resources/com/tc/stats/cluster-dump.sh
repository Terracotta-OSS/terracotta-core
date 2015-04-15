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

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

TC_INSTALL_DIR=`dirname "$0"`/..

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath -d "$TC_INSTALL_DIR"`
fi

exec "${JAVA_HOME}/bin/java" \
  -Dtc.install-root="${TC_INSTALL_DIR}" \
  ${JAVA_OPTS} \
  -cp "${TC_INSTALL_DIR}/lib/tc.jar" \
  com.tc.server.util.ClusterDumper "$@"