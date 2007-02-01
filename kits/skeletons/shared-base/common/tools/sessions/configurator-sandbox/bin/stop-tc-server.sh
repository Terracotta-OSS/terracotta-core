##!/bin/sh
#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved.
#

cd `dirname "$0"`/../../..
SANDBOX="`pwd`/sessions/configurator-sandbox/$1"
../bin/stop-tc-server.sh -f "${SANDBOX}"/tc-config.xml
