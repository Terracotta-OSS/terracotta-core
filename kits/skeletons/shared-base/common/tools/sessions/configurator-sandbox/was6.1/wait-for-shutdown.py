
import time.sleep
import java.lang.System

################################################################################
## Helper functions
################################################################################

def info(s):
    print '[INFO]  ' + s

def error(s):
    print '[ERROR] ' + s

################################################################################
## Main program
################################################################################

try:
    serverSearchString = 'type=Server,j2eeType=J2EEServer,node=' + AdminControl.getNode() + ',cell=' + AdminControl.getCell() + ',*'
    serverInstances = AdminControl.queryMBeans(serverSearchString)
    if serverInstances.size() != 1:
        error('Found ' + str(serverInstances.size()) + ' servers, expected 1: ' + str(serverInstances))
        sys.exit(1)
    serverInstance = serverInstances[0]
    serverName     = serverInstance.getObjectName()
    serverProcess  = serverName.getKeyProperty('process')
    info('Connected to WebSphere Application Server process[' + serverProcess + '], waiting for shutdown.')
    connected          = 'True'
    stoppingStateFound = None
    while 1:
            serverState = AdminControl.getAttribute(serverName.toString(), "state")
            if serverState == 'STOPPED':
                info('Server is stopped')
                sys.exit(0)
            elif serverState == 'STOPPING':
                stoppingStateFound = 'True'
            sleep(3)
except:
    if stoppingStateFound:
        info('Server is stopped')
    elif connected:
        error('Lost connection to server before we were able to see a STOPPING state, assuming server is now stopped')
    else:
        error('Unable to connect to server ' + server + ', assuming it is stopped')
    sys.exit(0)
