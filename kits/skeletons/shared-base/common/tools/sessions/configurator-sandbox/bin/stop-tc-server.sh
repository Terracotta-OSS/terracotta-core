#!/bin/sh

#  All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

cd `dirname "$0"`/../../..
SANDBOX="`pwd`/sessions/configurator-sandbox/$1"
../bin/stop-tc-server.sh -f "${SANDBOX}"/tc-config.xml
