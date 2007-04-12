/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import org.apache.commons.lang.StringUtils;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedByte;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.capabilities.AbstractCapabilitiesFactory;
import com.tc.config.schema.L2Info;
import com.tc.config.schema.NewCommonL2Config;
import com.tc.config.schema.NewSystemConfig;
import com.tc.config.schema.messaging.http.ConfigServlet;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.lang.StartupHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.L2State;
import com.tc.management.beans.TCServerInfo;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.ConnectionPolicyImpl;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.stats.DSO;
import com.tc.stats.DSOMBean;
import com.tc.util.Assert;

import java.util.Date;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

public class TCServerImpl extends SEDA implements TCServer {

  private static final TCLogger                logger        = TCLogging.getLogger(TCServer.class);
  private static final TCLogger                consoleLogger = CustomerLogging.getConsoleLogger();
  private static final byte                    STATE_STOPPED = 0;
  private static final byte                    STATE_STARTED = 1;
  private static final byte                    STATE_ACTIVE  = 2;

  private long                                 startTime;
  private long                                 activateTime;

  private CommunicationsManagerImpl            commsManager;
  private DistributedObjectServer              dsoServer;
  private Server                               httpServer;
  private TerracottaConnector                  terracottaConnector;

  private final Object                         stateLock     = new Object();
  private final SynchronizedByte               state         = new SynchronizedByte(STATE_STOPPED);

  private final L2TVSConfigurationSetupManager configurationSetupManager;
  private final ConnectionPolicy               connectionPolicy;

  /**
   * This should only be used for tests.
   */
  public TCServerImpl(L2TVSConfigurationSetupManager configurationSetupManager) {
    this(configurationSetupManager, new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(TCServer.class))));
  }

  public TCServerImpl(L2TVSConfigurationSetupManager configurationSetupManager, TCThreadGroup threadGroup) {
    this(configurationSetupManager, threadGroup, new ConnectionPolicyImpl(Integer.MAX_VALUE));
  }

  public TCServerImpl(L2TVSConfigurationSetupManager manager, TCThreadGroup group, ConnectionPolicy connectionPolicy) {
    super(group);
    this.connectionPolicy = connectionPolicy;
    Assert.assertNotNull(manager);
    this.configurationSetupManager = manager;
  }

  public L2Info[] infoForAllL2s() {
    String[] allKnownL2s = this.configurationSetupManager.allCurrentlyKnownServers();
    L2Info[] out = new L2Info[allKnownL2s.length];

    for (int i = 0; i < out.length; ++i) {
      try {
        NewCommonL2Config config = this.configurationSetupManager.commonL2ConfigFor(allKnownL2s[i]);

        String name = allKnownL2s[i];
        if (name == null) name = L2Info.IMPLICIT_L2_NAME;

        String host = config.host().getString();
        if (StringUtils.isBlank(host)) host = name;

        out[i] = new L2Info(name, host, config.jmxPort().getInt());
      } catch (ConfigurationSetupException cse) {
        throw Assert.failure("This should be impossible here", cse);
      }
    }

    return out;
  }

  public String getDescriptionOfCapabilities() {
    return AbstractCapabilitiesFactory.getCapabilitiesManager().describe();
  }

  /**
   * I realize this is wrong, since the server can still be starting but we'll have to deal with the whole stopping
   * issue later, and there's the TCStop feature which should be removed.
   */
  public void stop() {
    synchronized (stateLock) {
      if (state.commit(STATE_ACTIVE, STATE_STOPPED)) {
        stopServer();
        logger.info("Server stopped.");
      } else {
        logger.warn("Server in incorrect state (" + state.get() + ") to be stopped.");
      }
    }

  }

  public void start() {
    synchronized (stateLock) {
      if (state.commit(STATE_STOPPED, STATE_STARTED)) {
        try {
          startServer();
          if (!state.commit(STATE_STARTED, STATE_ACTIVE)) {
            // formatting
            throw new AssertionError("Server in incorrect state (" + state.get() + ") to be active.");
          }
        } catch (Throwable t) {
          if (t instanceof RuntimeException) { throw (RuntimeException) t; }
          throw new RuntimeException(t);
        }
      } else {
        logger.warn("Server in incorrect state (" + state.get() + ") to be started.");
      }
    }
  }

  public void shutdown() {
    synchronized (stateLock) {
      if (!isStopped()) {
        stop();
      }
      consoleLogger.info("Server exiting...");
      System.exit(0);
    }
  }

  public long getStartTime() {
    return startTime;
  }

  public long getActivateTime() {
    synchronized (stateLock) {
      return activateTime;
    }
  }

  public int getDSOListenPort() {
    if (dsoServer != null) { return dsoServer.getListenPort(); }
    throw new IllegalStateException("DSO Server not running");
  }

  public void dump() {
    if (dsoServer != null) {
      dsoServer.dump();
    }
  }

  public DistributedObjectServer getDSOServer() {
    return dsoServer;
  }

  public boolean isStarted() {
    return state.compareTo(STATE_STARTED) == 0;
  }

  public boolean isActive() {
    return state.compareTo(STATE_ACTIVE) == 0;
  }

  public boolean isStopped() {
    return state.compareTo(STATE_STOPPED) == 0;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Server: ").append(super.toString()).append("\n");
    if (isActive()) {
      buf.append("Active since ").append(new Date(getStartTime())).append("\n");
    } else if (isStarted()) {
      buf.append("Started at ").append(new Date(getStartTime())).append("\n");
    } else {
      buf.append("Server is stopped").append("\n");
    }

    return buf.toString();
  }

  private void stopServer() {
    // XXX: I have no idea if order of operations is correct here?

    if (logger.isDebugEnabled()) {
      consoleLogger.debug("Stopping TC server...");
    }

    if (commsManager != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Shutting down communications manager...");
      }

      try {
        commsManager.shutdown();
      } catch (Exception e) {
        logger.error("Error shutting down comms manager", e);
      } finally {
        commsManager = null;
      }
    }

    if (terracottaConnector != null) {
      try {
        terracottaConnector.shutdown();
      } catch (Exception e) {
        logger.error("Error shutting down terracotta connector", e);
      } finally {
        terracottaConnector = null;
      }
    }

    try {
      getStageManager().stopAll();
    } catch (Exception e) {
      logger.error("Error shutting down stage manager", e);
    }

    if (httpServer != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Shutting down HTTP server...");
      }

      try {
        httpServer.stop();
      } catch (Exception e) {
        logger.error("Error shutting down HTTP server", e);
      } finally {
        httpServer = null;
      }
    }

    // this stops the jmx server then dso server
    if (dsoServer != null) {
      try {
        dsoServer.quickStop();
      } catch (Exception e) {
        logger.error("Error shutting down DSO server", e);
      } finally {
        dsoServer = null;
      }
    }

  }

  private class StartAction implements StartupAction {
    public void execute() throws Throwable {
      if (logger.isDebugEnabled()) {
        logger.debug("Starting Terracotta server...");
      }

      startTime = System.currentTimeMillis();

      NewSystemConfig systemConfig = TCServerImpl.this.configurationSetupManager.systemConfig();
      terracottaConnector = new TerracottaConnector();
      startHTTPServer(systemConfig, terracottaConnector);

      Stage stage = getStageManager().createStage("dso-http-bridge", new HttpConnectionHandler(terracottaConnector), 1,
                                                  100);
      getStageManager().startAll(new NullContext(getStageManager()));

      // the following code starts the jmx server as well
      startDSOServer(stage.getSink());

      activateTime = System.currentTimeMillis();

      if (activationListener != null) {
        activationListener.serverActivated();
      }
    }
  }

  protected void startServer() throws Exception {
    new StartupHelper(getThreadGroup(), new StartAction()).startUp();
  }

  private void startDSOServer(Sink httpSink) throws Exception {
    L2State l2State = new L2State();
    dsoServer = new DistributedObjectServer(configurationSetupManager, getThreadGroup(), connectionPolicy, httpSink,
                                            new TCServerInfo(this, l2State), l2State);
    dsoServer.start();
    registerDSOServer();
  }

  private void startHTTPServer(NewSystemConfig systemConfig, TerracottaConnector tcConnector) throws Exception {
    httpServer = new Server();
    httpServer.addConnector(tcConnector);

    WebAppContext context = new WebAppContext("", "/");
    ServletHandler servletHandler = new ServletHandler();

    /**
     * We don't serve up any files, just hook in a few servlets. It's required the ResourceBase be non-null.
     */
    context.setResourceBase(System.getProperty("user.dir"));

    ServletHolder holder;
    holder = servletHandler.addServletWithMapping(VersionServlet.class.getName(), "/version");
    servletHandler.addServlet(holder);

    context.setAttribute(ConfigServlet.CONFIG_ATTRIBUTE, this.configurationSetupManager);
    holder = servletHandler.addServletWithMapping(ConfigServlet.class.getName(), "/config");
    servletHandler.addServlet(holder);

    context.setServletHandler(servletHandler);
    httpServer.addHandler(context);

    try {
      httpServer.start();
    } catch (Exception e) {
      consoleLogger.warn("Couldn't start HTTP server", e);
      throw e;
    }
  }

  private void registerDSOServer() throws InstanceAlreadyExistsException, MBeanRegistrationException,
      NotCompliantMBeanException, NullPointerException {

    ServerManagementContext mgmtContext = dsoServer.getManagementContext();
    MBeanServer mBeanServer = dsoServer.getMBeanServer();
    DSOMBean dso = new DSO(mgmtContext, mBeanServer);
    mBeanServer.registerMBean(dso, L2MBeanNames.DSO);
    mBeanServer.registerMBean(mgmtContext.getDSOAppEventsMBean(), L2MBeanNames.DSO_APP_EVENTS);
  }

  // TODO: check that this is not needed then remove
  private TCServerActivationListener activationListener;

  public void setActivationListener(TCServerActivationListener listener) {
    activationListener = listener;
  }

  private static class NullContext implements ConfigurationContext {

    private final StageManager manager;

    public NullContext(StageManager manager) {
      this.manager = manager;
    }

    public TCLogger getLogger(Class clazz) {
      return TCLogging.getLogger(clazz);
    }

    public Stage getStage(String name) {
      return manager.getStage(name);
    }

  }

}
