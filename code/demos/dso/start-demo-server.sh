#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

if test "$#" -ne "0"; then
	echo "Usage:"
	echo "  $0"
	
	exit 1
fi

cd `dirname $0`
exec ../bin/start-tc-server.sh
