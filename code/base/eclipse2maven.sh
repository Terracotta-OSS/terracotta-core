#!/bin/sh

if test -z "${JAVA_HOME}"; then
	JAVA_HOME="${TC_JAVA_HOME_15}"
	export JAVA_HOME
fi

JRUBY_HOME="`dirname $0`/../../buildsystems/jruby"
export JRUBY_HOME

if test ! -d "${JRUBY_HOME}"; then
	echo "--------------------------------------------------------------------------------"
	echo "LOADING JRUBY USING IVY"
	echo ""
	"${ANT_HOME}/bin/ant" -buildfile "`dirname $0`/buildconfig/build.xml"
fi


	echo ""
	echo "--------------------------------------------------------------------------------"
	echo "RUNNING ECLIPSE2MAVEN"
	echo ""
	exec /bin/sh "${JRUBY_HOME}/bin/jruby" eclipse2maven.rb "$@"
