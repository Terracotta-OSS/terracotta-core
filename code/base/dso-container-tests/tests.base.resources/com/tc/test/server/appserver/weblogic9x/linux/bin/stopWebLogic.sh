#!/bin/sh

# WARNING: This file is created by the Configuration Wizard.
# Any changes to this script may be lost when adding extensions to this configuration.

if [ "$1" != "" ] ; then
	userID="username='$1',"
	shift
else
	if [ "${userID}" != "" ] ; then
		userID="username='${userID}',"
	fi
fi

if [ "$1" != "" ] ; then
	password="password='$1',"
	shift
else
	if [ "${password}" != "" ] ; then
		password="password='${password}',"
	fi
fi

# set ADMIN_URL

if [ "$1" != "" ] ; then
	ADMIN_URL="$1"
	shift
else
	if [ "${ADMIN_URL}" = "" ] ; then
		ADMIN_URL="t3://localhost:7001"
	fi
fi

# Call setDomainEnv here because we want to have shifted out the environment vars above

DOMAIN_HOME="/home/teck/domain"

. ${DOMAIN_HOME}/bin/setDomainEnv.sh

echo "connect(${userID} ${password} url='${ADMIN_URL}',adminServerName='${SERVER_NAME}')" >"shutdown.py" 
echo "shutdown('${SERVER_NAME}','Server')" >>"shutdown.py" 
echo "exit()" >>"shutdown.py" 

echo "Stopping Weblogic Server..."

${JAVA_HOME}/bin/java ${JAVA_OPTIONS} weblogic.WLST shutdown.py  2>&1 

echo "Done"

exit

