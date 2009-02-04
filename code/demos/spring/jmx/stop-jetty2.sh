CWD=`dirname "$0"`
TC_HOME=${CWD}/../../..
JETTY_HOME="${TC_HOME}/vendors/jetty-6.1.15"
JAVA_OPTS="${JAVA_OPTS} -Djetty.home=${JETTY_HOME}"
JAVA_OPTS="${JAVA_OPTS} -DSTOP.PORT=8282"
JAVA_OPTS="${JAVA_OPTS} -DSTOP.KEY=secret"
export JAVA_OPTS
exec "${JAVA_HOME}/bin/java" ${JAVA_OPTS} -jar ${JETTY_HOME}/start.jar --stop
