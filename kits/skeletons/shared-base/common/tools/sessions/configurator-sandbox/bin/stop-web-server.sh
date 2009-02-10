#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.


#

# --------------------------------------------------------------------
# - stop-web-server.sh {tomcat5.0|tomcat5.5|wls8.1} 908{1,2}
# --------------------------------------------------------------------

CWD=`dirname "$0"`

cd "${CWD}"/../"$1"
exec ./stop.sh "$2"
