#!/bin/sh

# WARNING: This file is created by the Configuration Wizard.
# Any changes to this script may be lost when adding extensions to this configuration.

# --- Start Functions ---

BP=100
SP=$BP

pushd()
{
	if [ -z "$1" ]
	then
		return
	fi

	SP=`expr $SP - 1`
	eval _stack$SP=`pwd`
	cd $1
	return
}

popd()
{
	if [ $SP -eq $BP ]
	then
		return
	fi
	eval cd \${_stack$SP}
	SP=`expr $SP + 1`
	return
}


# --- End Functions ---

# *************************************************************************
# This script is used to setup the needed environment to be able to start Weblogic Server in this domain.
# 
# This script initializes the following variables before calling commEnv to set other variables:
# 
# WL_HOME         - The BEA home directory of your WebLogic installation.
# JAVA_VM         - The desired Java VM to use. You can set this environment variable before calling
#                   this script to switch between Sun or BEA or just have the default be set. 
# JAVA_HOME       - Location of the version of Java used to start WebLogic
#                   Server. Depends directly on which JAVA_VM value is set by default or by the environment.
# USER_MEM_ARGS   - The variable to override the standard memory arguments
#                   passed to java.
# PRODUCTION_MODE - The variable that determines whether Weblogic Server is started in production mode.
# DOMAIN_PRODUCTION_MODE 
#                 - The variable that determines whether the workshop related settings like the debugger,
#                   testconsole or iterativedev should be enabled. ONLY settable using the 
#                   command-line parameter named production
#                   NOTE: Specifying the production command-line param will force 
#                          the server to start in production mode.
# 
# Other variables used in this script include:
# SERVER_NAME     - Name of the weblogic server.
# JAVA_OPTIONS    - Java command-line options for running the server. (These
#                   will be tagged on to the end of the JAVA_VM and
#                   MEM_ARGS)
# 
# For additional information, refer to the WebLogic Server Administration
# Console Online Help(http://e-docs.bea.com/wls/docs92/ConsoleHelp/startstop.html).
# *************************************************************************

WL_HOME="/home/teck/bea/weblogic92"
export WL_HOME

BEA_JAVA_HOME="/home/teck/bea/jrockit90_150_10"
export BEA_JAVA_HOME

SUN_JAVA_HOME="/home/teck/bea/jdk150_10"
export SUN_JAVA_HOME

if [ "${JAVA_VENDOR}" = "BEA" ] ; then
	JAVA_HOME="${BEA_JAVA_HOME}"
	export JAVA_HOME
else
	if [ "${JAVA_VENDOR}" = "Sun" ] ; then
		JAVA_HOME="${SUN_JAVA_HOME}"
		export JAVA_HOME
	else
		JAVA_VENDOR="Sun"
		export JAVA_VENDOR
		JAVA_HOME="/home/teck/bea/jdk150_10"
		export JAVA_HOME
	fi
fi

# We need to reset the value of JAVA_HOME to get it shortened AND 
# we can not shorten it above because immediate variable expansion will blank it

JAVA_HOME="${JAVA_HOME}"
export JAVA_HOME

SAMPLES_HOME="${WL_HOME}/samples"
export SAMPLES_HOME

DOMAIN_HOME="/home/teck/domain"
export DOMAIN_HOME

LONG_DOMAIN_HOME="/home/teck/domain"
export LONG_DOMAIN_HOME

if [ "${DEBUG_PORT}" = "" ] ; then
	DEBUG_PORT="8453"
	export DEBUG_PORT
fi

if [ "${SERVER_NAME}" = "" ] ; then
	SERVER_NAME="AdminServer"
	export SERVER_NAME
fi

POINTBASE_FLAG="false"
export POINTBASE_FLAG

enableHotswapFlag=""
export enableHotswapFlag

PRODUCTION_MODE=""
export PRODUCTION_MODE

doExitFlag="false"
export doExitFlag
verboseLoggingFlag="false"
export verboseLoggingFlag
while [ $# -gt 0 ]
do
	case $1 in
	nodebug)
		debugFlag="false"
		export debugFlag
		;;
	production)
		DOMAIN_PRODUCTION_MODE="true"
		export DOMAIN_PRODUCTION_MODE
		;;
	notestconsole)
		testConsoleFlag="false"
		export testConsoleFlag
		;;
	noiterativedev)
		iterativeDevFlag="false"
		export iterativeDevFlag
		;;
	noLogErrorsToConsole)
		logErrorsToConsoleFlag="false"
		export logErrorsToConsoleFlag
		;;
	nopointbase)
		POINTBASE_FLAG="false"
		export POINTBASE_FLAG
		;;
	doExit)
		doExitFlag="true"
		export doExitFlag
		;;
	noExit)
		doExitFlag="false"
		export doExitFlag
		;;
	verbose)
		verboseLoggingFlag="true"
		export verboseLoggingFlag
		;;
	enableHotswap)
		enableHotswapFlag="-javaagent:${WL_HOME}/server/lib/diagnostics-agent.jar"
		export enableHotswapFlag
		;;
	*)
		PROXY_SETTINGS="${PROXY_SETTINGS} $1"
		export PROXY_SETTINGS
		;;
	esac
	shift
done


MEM_DEV_ARGS=""
export MEM_DEV_ARGS

if [ "${DOMAIN_PRODUCTION_MODE}" = "true" ] ; then
	PRODUCTION_MODE="${DOMAIN_PRODUCTION_MODE}"
	export PRODUCTION_MODE
fi

if [ "${PRODUCTION_MODE}" = "true" ] ; then
	debugFlag="false"
	export debugFlag
	testConsoleFlag="false"
	export testConsoleFlag
	iterativeDevFlag="false"
	export iterativeDevFlag
fi

# If you want to override the default Patch Classpath, Library Path and Path for this domain,
# Please uncomment the following lines and add a valid value for the environment variables
# set PATCH_CLASSPATH=[myPatchClasspath] (windows)
# set PATCH_LIBPATH=[myPatchLibpath] (windows)
# set PATCH_PATH=[myPatchPath] (windows)
# PATCH_CLASSPATH=[myPatchClasspath] (unix)
# PATCH_LIBPATH=[myPatchLibpath] (unix)
# PATCH_PATH=[myPatchPath] (unix)

. ${WL_HOME}/common/bin/commEnv.sh

WLS_HOME="${WL_HOME}/server"
export WLS_HOME

WLI_HOME="${WL_HOME}/integration"
export WLI_HOME

MEM_ARGS="-Xms256m -Xmx512m"
export MEM_ARGS

if [ "${JAVA_VENDOR}" = "Sun" ] ; then
	if [ "${PRODUCTION_MODE}" = "" ] ; then
		MEM_DEV_ARGS="-XX:CompileThreshold=8000 -XX:PermSize=48m "
		export MEM_DEV_ARGS
	fi
fi

# Had to have a separate test here BECAUSE of immediate variable expansion on windows

if [ "${JAVA_VENDOR}" = "Sun" ] ; then
	MEM_ARGS="${MEM_ARGS} ${MEM_DEV_ARGS} -XX:MaxPermSize=128m"
	export MEM_ARGS
fi

if [ "${JAVA_VENDOR}" = "HP" ] ; then
	MEM_ARGS="${MEM_ARGS} -XX:MaxPermSize=128m"
	export MEM_ARGS
fi

# IF USER_MEM_ARGS the environment variable is set, use it to override ALL MEM_ARGS values

if [ "${USER_MEM_ARGS}" != "" ] ; then
	MEM_ARGS="${USER_MEM_ARGS}"
	export MEM_ARGS
fi

JAVA_PROPERTIES="-Dplatform.home=${WL_HOME} -Dwls.home=${WLS_HOME} -Dwli.home=${WLI_HOME}"
export JAVA_PROPERTIES

#  To use Java Authorization Contract for Containers (JACC) in this domain, 
#  please uncomment the following section. If there are multiple machines in 
#  your domain, be sure to edit the setDomainEnv in the associated domain on 
#  each machine.
# 
# -Djava.security.manager
# -Djava.security.policy=location of weblogic.policy
# -Djavax.security.jacc.policy.provider=weblogic.security.jacc.simpleprovider.SimpleJACCPolicy
# -Djavax.security.jacc.PolicyConfigurationFactory.provider=weblogic.security.jacc.simpleprovider.PolicyConfigurationFactoryImpl
# -Dweblogic.security.jacc.RoleMapperFactory.provider=weblogic.security.jacc.simpleprovider.RoleMapperFactoryImpl

JAVA_PROPERTIES="${JAVA_PROPERTIES} ${EXTRA_JAVA_PROPERTIES}"
export JAVA_PROPERTIES

ARDIR="${WL_HOME}/server/lib"
export ARDIR

pushd ${LONG_DOMAIN_HOME}

# Clustering support (edit for your cluster!)

if [ "${ADMIN_URL}" = "" ] ; then
	# The then part of this block is telling us we are either starting an admin server OR we are non-clustered
	CLUSTER_PROPERTIES="-Dweblogic.management.discover=true"
	export CLUSTER_PROPERTIES
else
	CLUSTER_PROPERTIES="-Dweblogic.management.discover=false -Dweblogic.management.server=${ADMIN_URL}"
	export CLUSTER_PROPERTIES
fi

if [ "${LOG4J_CONFIG_FILE}" != "" ] ; then
	JAVA_PROPERTIES="${JAVA_PROPERTIES} -Dlog4j.configuration=file:${LOG4J_CONFIG_FILE}"
	export JAVA_PROPERTIES
fi

JAVA_PROPERTIES="${JAVA_PROPERTIES} ${CLUSTER_PROPERTIES}"
export JAVA_PROPERTIES

# Clear the pre_classpath here in case an application template wants to set it before the larger pre_classpath is invoked below

PRE_CLASSPATH=""
export PRE_CLASSPATH

JAVA_DEBUG=""
export JAVA_DEBUG

if [ "${debugFlag}" = "true" ] ; then
	JAVA_DEBUG="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=${DEBUG_PORT},server=y,suspend=n -Djava.compiler=NONE"
	export JAVA_DEBUG
	JAVA_OPTIONS="${JAVA_OPTIONS} ${enableHotswapFlag} -ea -da:com.bea... -da:javelin... -da:weblogic... -ea:com.bea.wli... -ea:com.bea.broker... -ea:com.bea.sbconsole..."
	export JAVA_OPTIONS
else
	JAVA_OPTIONS="${JAVA_OPTIONS} ${enableHotswapFlag} -da"
	export JAVA_OPTIONS
fi

if [ ! -d ${JAVA_HOME}/lib ] ; then
	echo "The JRE was not found in directory ${JAVA_HOME}. (JAVA_HOME)"
	echo "Please edit your environment and set the JAVA_HOME"
	echo "variable to point to the root directory of your Java installation."
	popd
	read _val
	exit
fi

if [ "${POINTBASE_FLAG}" = "true" ] ; then
	DATABASE_CLASSPATH="${POINTBASE_CLASSPATH}"
	export DATABASE_CLASSPATH
else
	DATABASE_CLASSPATH="${POINTBASE_CLIENT_CLASSPATH}"
	export DATABASE_CLASSPATH
fi

POST_CLASSPATH=""
export POST_CLASSPATH

POST_CLASSPATH="${POST_CLASSPATH}${CLASSPATHSEP}${DATABASE_CLASSPATH}${CLASSPATHSEP}${ARDIR}/xqrl.jar"
export POST_CLASSPATH

# PROFILING SUPPORT

JAVA_PROFILE=""
export JAVA_PROFILE

SERVER_CLASS="weblogic.Server"
export SERVER_CLASS

JAVA_PROPERTIES="${JAVA_PROPERTIES} ${WLP_JAVA_PROPERTIES}"
export JAVA_PROPERTIES

JAVA_OPTIONS="${JAVA_OPTIONS} ${JAVA_PROPERTIES} -Dwlw.iterativeDev=${iterativeDevFlag} -Dwlw.testConsole=${testConsoleFlag} -Dwlw.logErrorsToConsole=${logErrorsToConsoleFlag}"
export JAVA_OPTIONS

# -- Setup properties so that we can save stdout and stderr to files

if [ "${WLS_STDOUT_LOG}" != "" ] ; then
	echo "Logging WLS stdout to ${WLS_STDOUT_LOG}"
	JAVA_OPTIONS="${JAVA_OPTIONS} -Dweblogic.Stdout=${WLS_STDOUT_LOG}"
	export JAVA_OPTIONS
fi

if [ "${WLS_STDERR_LOG}" != "" ] ; then
	echo "Logging WLS stderr to ${WLS_STDERR_LOG}"
	JAVA_OPTIONS="${JAVA_OPTIONS} -Dweblogic.Stderr=${WLS_STDERR_LOG}"
	export JAVA_OPTIONS
fi

# ADD EXTENSIONS TO CLASSPATHS

if [ "${EXT_PRE_CLASSPATH}" != "" ] ; then
	PRE_CLASSPATH="${EXT_PRE_CLASSPATH}${CLASSPATHSEP}${PRE_CLASSPATH}"
	export PRE_CLASSPATH
fi

if [ "${EXT_POST_CLASSPATH}" != "" ] ; then
	POST_CLASSPATH="${POST_CLASSPATH}${CLASSPATHSEP}${EXT_POST_CLASSPATH}"
	export POST_CLASSPATH
fi

if [ "${WEBLOGIC_EXTENSION_DIRS}" != "" ] ; then
	JAVA_OPTIONS="${JAVA_OPTIONS} -Dweblogic.ext.dirs=${WEBLOGIC_EXTENSION_DIRS}"
	export JAVA_OPTIONS
fi

JAVA_OPTIONS="${JAVA_OPTIONS}"
export JAVA_OPTIONS

# SET THE CLASSPATH

CLASSPATH="${PRE_CLASSPATH}${CLASSPATHSEP}${WEBLOGIC_CLASSPATH}${CLASSPATHSEP}${POST_CLASSPATH}${CLASSPATHSEP}${WLP_POST_CLASSPATH}"
export CLASSPATH

JAVA_VM="${JAVA_VM} ${JAVA_DEBUG} ${JAVA_PROFILE}"
export JAVA_VM

