#!/bin/sh
#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

if test "$#" != "0"; then
   echo "Usage:"
   echo "  $0"
   exit 1
fi

cd "`dirname $0`"
exec ../../../../bin/stop-tc-server.sh
