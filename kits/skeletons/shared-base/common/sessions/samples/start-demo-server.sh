#!/bin/sh
#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

cd "`dirname $0`"
exec ../../bin/start-tc-server.sh &
