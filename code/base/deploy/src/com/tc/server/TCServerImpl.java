/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.capabilities.AbstractCapabilitiesFactory;
import com.tc.config.Directories;
import com.tc.config.schema.L2Info;
import com.tc.config.schema.NewCommonL2Config;
import com.tc.config.schema.messaging.http.ConfigServlet;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.l2.state.StateManager;
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
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.ConnectionPolicyImpl;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.properties.TCPropertiesImpl;
import com.tc.servlets.L1PropertiesFromL2Servlet;
import com.tc.statistics.StatisticsGathererSubSystem;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.beans.impl.StatisticsLocalGathererMBeanImpl;
import com.tc.stats.DSO;
import com.tc.stats.DSOMBean;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

public class TCServerImpl extends SEDA implements TCServer {

  private static final String                  HTTP_DEFAULTSERVLET_ENABLED                  = "http.defaultservlet.enabled";
  private static final String                  HTTP_DEFAULTSERVLET_ATTRIBUTE_DIRALLOWED     = "http.defaultservlet.attribute.dirallowed";
  private static final String                  HTTP_DEFAULTSERVLET_ATTRIBUTE_ALIASES        = "http.defaultservlet.attribute.aliases";

  private static final String                  VERSION_SERVLET_PATH                         = "/version";
  private static final String                  CONFIG_SERVLET_PATH                          = "/config";
  private static final String                  STATISTICS_GATHERER_SERVLET_PATH             = "/statistics-gatherer/*";
  private static final String                  L1_RECONNECT_PROPERTIES_FROML2_SERVELET_PATH = "/l1reconnectproperties";

  private static final String                  HTTP_AUTHENTICATION_ROLE_STATISTICS          = "statistics";

  private static final TCLogger                logger                                       = TCLogging
                                                                                                .getLogger(TCServer.class);
  private static final TCLogger                consoleLogger                                = CustomerLogging
                                                                                                .getConsoleLogger();

  private volatile long                        startTime                                    = -1;
  private volatile long                        activateTime                                 = -1;

  private DistributedObjectServer              dsoServer;
  private Server                               httpServer;
  private TerracottaConnector                  terracottaConnector;

  private final Object                         stateLock                                    = new Object();
  private final L2State                        state                                        = new L2State();

  private final L2TVSConfigurationSetupManager configurationSetupManager;
  private final ConnectionPolicy               connectionPolicy;

  private final StatisticsGathererSubSystem    statisticsGathererSubSystem;

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
    super(group, false);
    this.connectionPolicy = connectionPolicy;
    Assert.assertNotNull(manager);
    this.configurationSetupManager = manager;

    statisticsGathererSubSystem = new StatisticsGathererSubSystem();
    statisticsGathererSubSystem.setup(manager.commonl2Config());
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
      if (!state.isStartState()) {
        stopServer();
        logger.info("Server stopped.");
      } else {
        logger.warn("Server in incorrect state (" + state.getState() + ") to be stopped.");
      }
    }

  }

  public void start() {
    synchronized (stateLock) {
      if (state.isStartState()) {
        try {
          startServer();
        } catch (Throwable t) {
          if (t instanceof RuntimeException) { throw (RuntimeException) t; }
          throw new RuntimeException(t);
        }
      } else {
        logger.warn("Server in incorrect state (" + state.getState() + ") to be started.");
      }
    }
  }

  public boolean canShutdown() {
    return (!state.isStartState() || (dsoServer != null && dsoServer.isBlocking())) && !state.isStopState();
  }

  public synchronized void shutdown() {
    if (canShutdown()) {
      state.setState(StateManager.STOP_STATE);
      consoleLogger.info("Server exiting...");
      System.exit(0);
    } else {
      logger.warn("Server in incorrect state (" + state.getState() + ") to be shutdown.");
    }
  }

  public long getStartTime() {
    return startTime;
  }

  public void updateActivateTime() {
    if (activateTime == -1) {
      activateTime = System.currentTimeMillis();
    }
  }

  public long getActivateTime() {
    return activateTime;
  }

  public String getConfig() {
    try {
      InputStream is = configurationSetupManager.rawConfigFile();
      return IOUtils.toString(is);
    } catch (IOException ioe) {
      return ioe.getLocalizedMessage();
    }
  }

  public int getDSOListenPort() {
    if (dsoServer != null) { return dsoServer.getListenPort(); }
    throw new IllegalStateException("DSO Server not running");
  }

  public DistributedObjectServer getDSOServer() {
    return dsoServer;
  }

  public boolean isStarted() {
    return !state.isStartState();
  }

  public boolean isActive() {
    return state.isActiveCoordinator();
  }

  public boolean isStopped() {
    // XXX:: introduce a new state when stop is officially supported.
    return state.isStartState();
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

      NewCommonL2Config commonL2Config = TCServerImpl.this.configurationSetupManager.commonl2Config();
      terracottaConnector = new TerracottaConnector();
      startHTTPServer(commonL2Config, terracottaConnector);

      Stage stage = getStageManager().createStage("dso-http-bridge", new HttpConnectionHandler(terracottaConnector), 1,
                                                  100);
      stage.start(new NullContext(getStageManager()));

      // the following code starts the jmx server as well
      startDSOServer(stage.getSink());

      updateActivateTime();

      if (activationListener != null) {
        activationListener.serverActivated();
      }

      if (updateCheckEnabled()) {
        UpdateCheckAction.start(TCServerImpl.this, updateCheckPeriodDays());
      }
    }
  }

  private boolean updateCheckEnabled() {
    String s = System.getenv("TC_UPDATE_CHECK_ENABLED");
    boolean checkEnabled = (s == null) || Boolean.parseBoolean(s);
    return checkEnabled && configurationSetupManager.updateCheckConfig().isEnabled().getBoolean();
  }

  private int updateCheckPeriodDays() {
    return configurationSetupManager.updateCheckConfig().periodDays().getInt();
  }

  protected void startServer() throws Exception {
    new StartupHelper(getThreadGroup(), new StartAction()).startUp();
  }

  private void startDSOServer(Sink httpSink) throws Exception {
    Assert.assertTrue(state.isStartState());
    dsoServer = new DistributedObjectServer(configurationSetupManager, getThreadGroup(), connectionPolicy, httpSink,
                                            new TCServerInfo(this, state), state, this);
    dsoServer.start();
    registerDSOServer();
  }

  private void startHTTPServer(NewCommonL2Config commonL2Config, TerracottaConnector tcConnector) throws Exception {
    httpServer = new Server();
    httpServer.addConnector(tcConnector);

    Context context = new Context(null, "/", Context.NO_SESSIONS|Context.SECURITY);

    if (commonL2Config.httpAuthentication()) {
      Constraint constraint = new Constraint();
      constraint.setName(Constraint.__BASIC_AUTH);
      constraint.setRoles(new String[] { HTTP_AUTHENTICATION_ROLE_STATISTICS });
      constraint.setAuthenticate(true);

      ConstraintMapping cm = new ConstraintMapping();
      cm.setConstraint(constraint);
      cm.setPathSpec(STATISTICS_GATHERER_SERVLET_PATH);

      SecurityHandler sh = new SecurityHandler();
      sh.setUserRealm(new HashUserRealm("Terracotta Statistics Gatherer", commonL2Config
          .httpAuthenticationUserRealmFile()));
      sh.setConstraintMappings(new ConstraintMapping[] { cm });

      context.addHandler(sh);
      logger.info("HTTP Authentication enabled for path '" + STATISTICS_GATHERER_SERVLET_PATH
                  + "', using user realm file '" + commonL2Config.httpAuthenticationUserRealmFile() + "'");
    }

    context.setAttribute(ConfigServlet.CONFIG_ATTRIBUTE, this.configurationSetupManager);
    context.setAttribute(StatisticsGathererServlet.GATHERER_ATTRIBUTE, this.statisticsGathererSubSystem);

    ServletHandler servletHandler = new ServletHandler();

    /**
     * We usually don't serve up any files, just hook in a few servlets. The ResourceBase can't be null though.
     */
    File tcInstallDir;
    try {
      tcInstallDir = Directories.getInstallationRoot();
    } catch (FileNotFoundException e) {
      // if an error occurs during the retrieval of the installation root, just set it to null
      // so that the fallback mechanism can be used
      tcInstallDir = null;
    }
    File userDir = new File(System.getProperty("user.dir"));
    boolean tcInstallDirValid = false;
    File resourceBaseDir = userDir;
    if (tcInstallDir != null &&
        tcInstallDir.exists() &&
        tcInstallDir.isDirectory() &&
        tcInstallDir.canRead()) {
      tcInstallDirValid = true;
      resourceBaseDir = tcInstallDir;
    }
    context.setResourceBase(resourceBaseDir.getAbsolutePath());

    createAndAddServlet(servletHandler, VersionServlet.class.getName(), VERSION_SERVLET_PATH);
    createAndAddServlet(servletHandler, ConfigServlet.class.getName(), CONFIG_SERVLET_PATH);
    createAndAddServlet(servletHandler, StatisticsGathererServlet.class.getName(), STATISTICS_GATHERER_SERVLET_PATH);
    createAndAddServlet(servletHandler, L1PropertiesFromL2Servlet.class.getName(),
                        L1_RECONNECT_PROPERTIES_FROML2_SERVELET_PATH);

    if (TCPropertiesImpl.getProperties().getBoolean(HTTP_DEFAULTSERVLET_ENABLED, false)) {
      if (!tcInstallDirValid) {
        String msg = "Default HTTP servlet with file serving NOT enabled because the '"
                     + Directories.TC_INSTALL_ROOT_PROPERTY_NAME + "' system property is invalid.";
        consoleLogger.warn(msg);
        logger.warn(msg);
      } else {
        boolean aliases = TCPropertiesImpl.getProperties().getBoolean(HTTP_DEFAULTSERVLET_ATTRIBUTE_ALIASES, false);
        boolean dirallowed = TCPropertiesImpl.getProperties().getBoolean(HTTP_DEFAULTSERVLET_ATTRIBUTE_DIRALLOWED,
                                                                         false);
        context.setAttribute("aliases", aliases);
        context.setAttribute("dirAllowed", dirallowed);
        createAndAddServlet(servletHandler, DefaultServlet.class.getName(), "/");
        String msg = "Default HTTP servlet with file serving enabled for '" + resourceBaseDir.getCanonicalPath()
                     + "' (aliases = '" + aliases + "', dirallowed = '" + dirallowed + "')";
        consoleLogger.info(msg);
        logger.info(msg);
      }
    }

    context.setServletHandler(servletHandler);
    httpServer.addHandler(context);

    try {
      httpServer.start();
    } catch (Exception e) {
      consoleLogger.warn("Couldn't start HTTP server", e);
      throw e;
    }
  }

  private static void createAndAddServlet(ServletHandler servletHandler, String servletClassName, String path) {
    ServletHolder holder = servletHandler.addServletWithMapping(servletClassName, path);
    holder.setInitParameter("scratchdir", "jsp"); // avoid jetty from creating a "jsp" directory
    servletHandler.addServlet(holder);
  }

  public void dump() {
    if (dsoServer != null) {
      dsoServer.dump();
    }
  }

  private void registerDSOServer() throws InstanceAlreadyExistsException, MBeanRegistrationException,
      NotCompliantMBeanException, NullPointerException {

    ServerManagementContext mgmtContext = dsoServer.getManagementContext();
    MBeanServer mBeanServer = dsoServer.getMBeanServer();
    DSOMBean dso = new DSO(mgmtContext, mBeanServer);
    mBeanServer.registerMBean(dso, L2MBeanNames.DSO);
    mBeanServer.registerMBean(mgmtContext.getDSOAppEventsMBean(), L2MBeanNames.DSO_APP_EVENTS);
    StatisticsLocalGathererMBeanImpl local_gatherer = new StatisticsLocalGathererMBeanImpl(statisticsGathererSubSystem,
                                                                                           configurationSetupManager
                                                                                               .commonl2Config());
    mBeanServer.registerMBean(local_gatherer, StatisticsMBeanNames.STATISTICS_GATHERER);
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

  public void startBeanShell(int port) {
    if (dsoServer != null) {
      dsoServer.startBeanShell(port);
    }
  }
}