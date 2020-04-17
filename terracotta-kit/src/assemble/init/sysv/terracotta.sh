#!/bin/sh -e
#
#
#
#  The contents of this file are subject to the Terracotta Public License Version
#  2.0 (the "License"); You may not use this file except in compliance with the
#  License. You may obtain a copy of the License at
#
#  http://terracotta.org/legal/terracotta-public-license.
#
#  Software distributed under the License is distributed on an "AS IS" basis,
#  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
#  the specific language governing rights and limitations under the License.
#
#  The Covered Software is Terracotta Core.
#
#  The Initial Developer of the Covered Software is
#  Terracotta, Inc., a Software AG company
#

# Terracotta Server startup script
#chkconfig: 2345 80 05
#description: Terracotta Server
### BEGIN INIT INFO
# Provides:             terracotta-server
# Required-Start:       $remote_fs $syslog
# Required-Stop:        $remote_fs $syslog
# Default-Start:        2 3 4 5
# Default-Stop:
# Short-Description:    Terracotta Server
### END INIT INFO

# Allow multiple copies of this script with own config, ie
# /etc/init/terracotta1, /etc/init.d/terracotta2
APP=$(basename $0)

. /etc/default/$APP || {
  echo "Unable to process configuration in /etc/default/$APP"
  exit 1
}

findpid() {
  PID=$(jps -m -l | grep -- "$JPS_MATCH" | awk '{print $1}')
  [ -n "$PID" ]
}

case "$1" in
# Start command
start)
  if findpid; then
    echo "Already running"
  else
    echo "Starting $APP"
    /bin/su -m $USER -c "$TERRACOTTA_HOME/bin/start-tc-server.sh $TC_ARGS &> /dev/null"
  fi
  ;;
# Stop command
stop)
  if findpid; then
    echo "Stopping $APP with pid [$PID]"
    kill -- $PID
    echo "$APP killed"
  else
    echo "No running process found, nothing to do"
  fi
  ;;
# Restart command
restart)
  $0 stop
  sleep 5
  $0 start
  ;;
# Status command
status)
  if findpid; then
    echo "$APP is running with PID [$PID]"
  else
    echo "$APP is not running"
    exit 1
  fi
  ;;
*)
  echo "Usage: /etc/init.d/$APP {start|restart|stop|status}"
  exit 1
  ;;
esac

exit 0
