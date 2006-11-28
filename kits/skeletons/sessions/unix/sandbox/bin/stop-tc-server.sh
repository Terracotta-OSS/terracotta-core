#!/bin/sh

cd `dirname "$0"`/..
../bin/stop-tc-server.sh -f "$1"/tc-config.xml
