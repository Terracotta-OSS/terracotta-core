#!/bin/sh
#@COPYRIGHT@

if test "$#" -ne 0; then
    echo "Usage:"
    echo "  $0"
    exit 1
fi

cd "`dirname $0`"
exec ../bin/start-tc-server.sh
