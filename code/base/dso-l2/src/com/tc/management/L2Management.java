/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ObjectManagementMonitor;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.util.PortChooser;

import java.io.IOException;
import java.net.BindException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;

public class L2Management extends TerracottaManagement {

  private MBeanServer                          mBeanServer;
  private JMXConnectorServer                   jmxConnectorServer;
  private final L2TVSConfigurationSetupManager configurationSetupManager;
  private final TCServerInfoMBean              tcServerInfo;
  private final ObjectManagementMonitor        objectManagementBean;

  public L2Management(TCServerInfoMBean tcServerInfo, L2TVSConfigurationSetupManager configurationSetupManager)
      throws MBeanRegistrationException, NotCompliantMBeanException, InstanceAlreadyExistsException {
    this.tcServerInfo = tcServerInfo;
    this.configurationSetupManager = configurationSetupManager;

    try {
      objectManagementBean = new ObjectManagementMonitor();
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException(
                                   "Unable to construct one of the L2 MBeans: this is a programming error in one of those beans",
                                   ncmbe);
    }

    // LKC-2990 and LKC-3171: Remove the JMX generic optional logging
    java.util.logging.Logger jmxLogger = java.util.logging.Logger.getLogger("javax.management.remote.generic");
    jmxLogger.setLevel(java.util.logging.Level.OFF);

    final List jmxServers = MBeanServerFactory.findMBeanServer(null);
    if (jmxServers != null && !jmxServers.isEmpty()) {
      mBeanServer = (MBeanServer) jmxServers.get(0);
    } else {
      mBeanServer = MBeanServerFactory.createMBeanServer();
    }
    registerMBeans();
  }

  // service:jmx:rmi:///jndi/rmi://localhost:9519/jmxrmi
  public synchronized void start() throws Exception {
    int jmxPort = configurationSetupManager.commonl2Config().jmxPort().getInt();
    if (jmxPort == 0) {
      jmxPort = new PortChooser().chooseRandomPort();
    }
    try {
      JMXServiceURL url;
      Map env = new HashMap();
      if (configurationSetupManager.commonl2Config().authentication()) {
        env.put("jmx.remote.x.password.file", configurationSetupManager.commonl2Config().authenticationPasswordFile());
        env.put("jmx.remote.x.access.file", configurationSetupManager.commonl2Config().authenticationAccessFile());
//        File javaMgmnt = new File(System.getProperty("java.home") + File.separator + "lib" + File.separator
//                                  + "management");
//        env.put("jmx.remote.x.password.file", javaMgmnt + File.separator + "jmxremote.password");
//        env.put("jmx.remote.x.access.file", javaMgmnt + File.separator + "jmxremote.access");
      }
        Registry registry = LocateRegistry.createRegistry(jmxPort);
        url = new JMXServiceURL("service:jmx:rmi://");
        RMIJRMPServerImpl server = new RMIJRMPServerImpl(jmxPort, null, null, env);
        jmxConnectorServer = new RMIConnectorServer(url, env, server, mBeanServer);
        jmxConnectorServer.start();
        registry.bind("jmxrmi", server);
        CustomerLogging.getConsoleLogger().info(
                                                "JMX Server started. Available at URL["
                                                    + "service:jmx:rmi:///jndi/rmi://localhost:" + jmxPort + "/jmxrmi"
                                                    + "]");
    } catch (BindException be) {
      throw new Exception("Unable to bind JMX server on port " + jmxPort
                          + "; perhaps this port is already in use, or you don't have sufficient privileges?", be);
    }
  }

  public synchronized void stop() throws IOException, InstanceNotFoundException, MBeanRegistrationException {
    unregisterMBeans();
    if (jmxConnectorServer != null) {
      jmxConnectorServer.stop();
    }
  }

  public Object findMBean(ObjectName objectName, Class mBeanInterface) throws IOException {
    return findMBean(objectName, mBeanInterface, mBeanServer);
  }

  public MBeanServer getMBeanServer() {
    return mBeanServer;
  }

  public ObjectManagementMonitorMBean findObjectManagementMonitorMBean() {
    return objectManagementBean;
  }

  private void registerMBeans() throws MBeanRegistrationException, NotCompliantMBeanException,
      InstanceAlreadyExistsException {
    mBeanServer.registerMBean(tcServerInfo, L2MBeanNames.TC_SERVER_INFO);
    mBeanServer.registerMBean(TCLogging.getAppender().getMBean(), L2MBeanNames.LOGGER);
    mBeanServer.registerMBean(objectManagementBean, L2MBeanNames.OBJECT_MANAGEMENT);
  }

  private void unregisterMBeans() throws InstanceNotFoundException, MBeanRegistrationException {
    mBeanServer.unregisterMBean(L2MBeanNames.TC_SERVER_INFO);
    mBeanServer.unregisterMBean(L2MBeanNames.LOGGER);
    mBeanServer.unregisterMBean(L2MBeanNames.OBJECT_MANAGEMENT);
  }
}
