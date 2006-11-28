#!/bin/sh

cd `dirname "$0"`/..
../bin/make-boot-jar.sh -o ../../common/lib/dso-boot -f "$1"/tc-config.xml
../bin/start-tc-server.sh -f "$1"/tc-config.xml
