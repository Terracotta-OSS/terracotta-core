#!/bin/sh

#  All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

# --------------------------------------------------------------------
# - start.sh {tomcat5.0|tomcat5.5|wls8.1|geronimo1.1} 908{1,2} [nodso] [nowindow]
# --------------------------------------------------------------------

CWD=`dirname "$0"`

cd "${CWD}"/../"$1"
exec ./start.sh "$2" "$3" "$4"
