/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L1MBeanNames;
import com.tc.management.beans.MBeanNames;
import com.tc.management.beans.sessions.SessionMonitor;
import com.tc.management.beans.sessions.SessionMonitorMBean;
import com.tc.management.beans.tx.ClientTxMonitor;
import com.tc.management.beans.tx.ClientTxMonitorMBean;
import com.tc.management.exposed.SessionsProduct;
import com.tc.management.exposed.TerracottaCluster;
import com.tc.management.remote.protocol.ProtocolProvider;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.management.remote.protocol.terracotta.TunnelingMessageConnectionServer;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.runtime.Vm;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public final class L1Management extends TerracottaManagement {

  private static final TCLogger       logger      = TCLogging.getLogger(L1Management.class);
  private static boolean              forceCreate = false;

  private final SetOnceFlag           started;
  private final TunnelingEventHandler tunnelingHandler;
  private final Object                mBeanServerLock;
  private MBeanServer                 mBeanServer;
  private final ClientTxMonitor       clientTxBean;
  private final SessionMonitor        internalSessionBean;
  private final SessionsProduct       publicSessionBean;
  private final TerracottaCluster     clusterBean;

  public L1Management(final TunnelingEventHandler tunnelingHandler) {
    super();
    started = new SetOnceFlag();
    this.tunnelingHandler = tunnelingHandler;
    try {
      clientTxBean = new ClientTxMonitor();
      internalSessionBean = new SessionMonitor();
      publicSessionBean = new SessionsProduct(internalSessionBean, clientTxBean);
      clusterBean = new TerracottaCluster();
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException(
                                   "Unable to construct one of the L1 MBeans: this is a programming error in one of those beans",
                                   ncmbe);
    }
    mBeanServerLock = new Object();
  }

  public synchronized void start() {
    started.set();
    Thread registrationThread = new Thread(new Runnable() {

      private final int MAX_ATTEMPTS = 60 * 5;

      public void run() {
        boolean registered = false;
        int attemptCounter = 0;
        while (!registered && attemptCounter++ < MAX_ATTEMPTS) {
          try {
            attemptToRegister();
            registered = true;
          } catch (Exception e) {
            // Ignore and try again after 1 second, give the VM a chance to get started
            try {
              Thread.sleep(1000);
            } catch (InterruptedException ie) {
              new Exception("JMX registration thread interrupted, management beans will not be available", ie)
                  .printStackTrace();
            }
          }
        }
        if (registered) {
          tunnelingHandler.jmxIsReady();
        } else {
          logger.error("Aborted attempts to register management" + " beans after " + (MAX_ATTEMPTS / 60)
                       + " min of trying.");
        }
      }
    }, "L1Management JMX registration");
    registrationThread.setDaemon(true);
    registrationThread.start();
  }

  public Object findMBean(final ObjectName objectName, final Class mBeanInterface) throws IOException {
    if (objectName.equals(MBeanNames.CLIENT_TX_INTERNAL)) return clientTxBean;
    else if (objectName.equals(MBeanNames.SESSION_INTERNAL)) return internalSessionBean;
    else if (objectName.equals(L1MBeanNames.SESSION_PRODUCT_PUBLIC)) return publicSessionBean;
    else {
      synchronized (mBeanServerLock) {
        if (mBeanServer != null) { return findMBean(objectName, mBeanInterface, mBeanServer); }
      }
    }
    return null;
  }

  public ClientTxMonitorMBean findClientTxMonitorMBean() {
    return clientTxBean;
  }

  public SessionMonitorMBean findSessionMonitorMBean() {
    return internalSessionBean;
  }

  public synchronized static void forceCreateMBeanServer() {
    forceCreate = true;
  }

  public TerracottaCluster getTerracottaCluster() {
    return clusterBean;
  }

  private void attemptToRegister() throws InstanceAlreadyExistsException, MBeanRegistrationException,
      NotCompliantMBeanException {
    synchronized (mBeanServerLock) {
      if (mBeanServer == null) {
        if (shouldCreateMBeanServer()) {
          mBeanServer = MBeanServerFactory.createMBeanServer();
        } else {
          List mBeanServers = MBeanServerFactory.findMBeanServer(null);
          if (!mBeanServers.isEmpty()) {
            mBeanServer = (MBeanServer) mBeanServers.get(0);
          } else {
            throw new MBeanRegistrationException(new Exception("Waiting for default MBeanServer"));
          }
        }
        addJMXConnectors();
      }
    }
    mBeanServer.registerMBean(clientTxBean, MBeanNames.CLIENT_TX_INTERNAL);
    mBeanServer.registerMBean(internalSessionBean, MBeanNames.SESSION_INTERNAL);
    mBeanServer.registerMBean(publicSessionBean, L1MBeanNames.SESSION_PRODUCT_PUBLIC);
    mBeanServer.registerMBean(clusterBean, L1MBeanNames.CLUSTER_BEAN_PUBLIC);
    
  }

  private void addJMXConnectors() {
    JMXServiceURL url = null;
    try {
      // LKC-2990 and LKC-3171: Remove the JMX generic optional logging
      java.util.logging.Logger jmxLogger = java.util.logging.Logger.getLogger("javax.management.remote.generic");
      jmxLogger.setLevel(java.util.logging.Level.OFF);
    } catch (Throwable t) {
      logger.warn("Unable to disable default logging in Sun's JMX package; when Terracotta clients go"
                  + " up/down you may see stack traces printed to the log");
    }
    try {
      final Map environment = new HashMap();
      ProtocolProvider.addTerracottaJmxProvider(environment);
      environment.put(TunnelingMessageConnectionServer.TUNNELING_HANDLER, tunnelingHandler);
      url = new JMXServiceURL("terracotta", "localhost", 0);
      // Normally you should NOT do this in the client, but we have a modified version of jmxremote_optional.jar that
      // uses a daemon thread to wait for connections so we don't hang the client
      final JMXConnectorServer connServer = JMXConnectorServerFactory.newJMXConnectorServer(url, environment,
                                                                                            mBeanServer);
      connServer.start();
      logger.info("Terracotta JMX connector available at[" + url + "]");
    } catch (Exception e) {
      if (url != null) {
        logger.warn("Unable to start embedded JMX connector for url[" + url + "]", e);
      } else {
        logger.warn("Unable to construct embedded JMX connector URL with params (terracotta, localhost, 0)");
      }
    }
  }

  private synchronized static boolean shouldCreateMBeanServer() {
    // We get called very early in the bootstrap process, and can outrun the creation of the default mbean server that
    // the 1.5 JDK and jconsole uses. If it looks like the default server should start up then return false, otherwise
    // (1.4 JDK or the com.sun.management.jmxremote property is not set) return true.
    return forceCreate || !(Vm.isJDK15() && System.getProperty("com.sun.management.jmxremote") != null);
  }

}
