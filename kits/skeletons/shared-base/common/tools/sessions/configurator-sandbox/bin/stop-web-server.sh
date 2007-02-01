#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

# --------------------------------------------------------------------
# - stop-web-server.sh {tomcat5.0|tomcat5.5|wls8.1} 908{1,2}
# --------------------------------------------------------------------

CWD=`dirname "$0"`

cd "${CWD}"/../"$1"
exec ./stop.sh "$2"
