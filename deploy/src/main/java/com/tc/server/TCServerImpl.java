/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.webapp.WebAppContext;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.config.Directories;
import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.L2Info;
import com.tc.config.schema.SecurityConfig;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.messaging.http.ConfigServlet;
import com.tc.config.schema.messaging.http.GroupIDMapServlet;
import com.tc.config.schema.messaging.http.GroupInfoServlet;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.exception.TCRuntimeException;
import com.tc.l2.state.StateManager;
import com.tc.lang.StartupHelper;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.license.LicenseManager;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.L2State;
import com.tc.management.beans.TCServerInfo;
import com.tc.net.GroupID;
import com.tc.net.OrderedGroupIDs;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.ConnectionPolicyImpl;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.util.TcHashSessionIdManager;
import com.tc.servlets.L1ReconnectPropertiesServlet;
import com.tc.stats.DSO;
import com.tc.stats.api.DSOMBean;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.Conversion.MetricsFormatException;
import com.tc.util.ProductInfo;
import com.terracottatech.config.DataStorage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

public class TCServerImpl extends SEDA implements TCServer {

  public static final String                VERSION_SERVLET_PATH                         = "/version";
  public static final String                CONFIG_SERVLET_PATH                          = "/config";
  public static final String                GROUP_INFO_SERVLET_PATH                      = "/groupinfo";
  public static final String                GROUPID_MAP_SERVLET_PATH                     = "/groupidmap";
  public static final String                STATISTICS_GATHERER_SERVLET_PREFIX           = "/statistics-gatherer";
  public static final String                STATISTICS_GATHERER_SERVLET_PATH             = STATISTICS_GATHERER_SERVLET_PREFIX
                                                                                           + "/*";
  public static final String                L1_RECONNECT_PROPERTIES_FROML2_SERVELET_PATH = "/l1reconnectproperties";

  public static final String                HTTP_AUTHENTICATION_ROLE_STATISTICS          = "statistics";

  public static final String                HTTP_SECURITY_ROLE                           = "terracotta";

  public static final File                  TC_MANAGEMENT_API_LOCKFILE                   = new File(
                                                                                                    System
                                                                                                        .getProperty("java.io.tmpdir"),
                                                                                                    ".tc-management-api.lock");

  private static final TCLogger             logger                                       = TCLogging
                                                                                             .getLogger(TCServer.class);
  private static final TCLogger             consoleLogger                                = CustomerLogging
                                                                                             .getConsoleLogger();

  private volatile long                     startTime                                    = -1;
  private volatile long                     activateTime                                 = -1;

  protected DistributedObjectServer         dsoServer;
  private Server                            httpServer;
  private ContextHandlerCollection          contextHandlerCollection;
  private TerracottaConnector               terracottaConnector;

  private final Object                      stateLock                                    = new Object();
  private final L2State                     state                                        = new L2State();

  private final L2ConfigurationSetupManager configurationSetupManager;
  protected final ConnectionPolicy          connectionPolicy;
  private boolean                           shutdown                                     = false;
  protected final TCSecurityManager         securityManager;

  /**
   * This should only be used for tests.
   */
  public TCServerImpl(final L2ConfigurationSetupManager configurationSetupManager) {
    this(configurationSetupManager, new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(TCServer.class))));
  }

  public TCServerImpl(final L2ConfigurationSetupManager configurationSetupManager, final TCThreadGroup threadGroup) {
    this(configurationSetupManager, threadGroup, new ConnectionPolicyImpl(Integer.MAX_VALUE));
  }

  public TCServerImpl(final L2ConfigurationSetupManager manager, final TCThreadGroup group,
                      final ConnectionPolicy connectionPolicy) {
    super(group);

    this.connectionPolicy = connectionPolicy;
    Assert.assertNotNull(manager);
    validateEnterpriseFeatures(manager);
    this.configurationSetupManager = manager;

    if (configurationSetupManager.isSecure()) {
      this.securityManager = createSecurityManager(configurationSetupManager.getSecurity());
      verifySecurityManagerFindsCredentialsForAllL2Servers();
    } else {
      this.securityManager = null;
    }
  }

  private void verifySecurityManagerFindsCredentialsForAllL2Servers() {
    if (logger.isDebugEnabled()) {
      logger.debug("Checking that keychain information is complete for cluster");
    }
    List<ActiveServerGroupConfig> activeServerGroups = configurationSetupManager.activeServerGroupsConfig()
        .getActiveServerGroups();
    for (ActiveServerGroupConfig activeServerGroup : activeServerGroups) {
      for (String server : activeServerGroup.getMembers()) {
        if (!configurationSetupManager.getL2Identifier().equals(server)) {
          try {
            L2DSOConfig dsoConfig = configurationSetupManager.dsoL2ConfigFor(server);
            try {
              // Following will throw NPE if password not found - which is what we want
              securityManager.getPasswordForTC(securityManager.getIntraL2Username(), dsoConfig.host(), dsoConfig
                  .tsaGroupPort().getIntValue());
            } catch (NullPointerException npex) {
              throw new IllegalStateException(
                                              "Invalid cluster security configuration. Unable to find connection credentials to server "
                                                  + dsoConfig.serverName(), npex);
            }
          } catch (ConfigurationSetupException e) {
            throw new IllegalStateException("Unexpected error browsing cluster configuration", e);
          }
        }
      }
    }
  }

  protected TCSecurityManager createSecurityManager(final SecurityConfig securityConfig) {
    throw new UnsupportedOperationException("Only Terracotta EE supports the security feature, "
                                            + "you're currently running an OS version");
  }

  private void validateEnterpriseFeatures(final L2ConfigurationSetupManager manager) {
    if (!LicenseManager.enterpriseEdition()) return;

    DataStorage dataStorage = manager.dsoL2Config().getDataStorage();
    if (!TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_OFFHEAP_DISABLED, false)) {
      LicenseManager.verifyServerArrayOffheapCapability(dataStorage.getSize());
    }
    if (manager.commonl2Config().authentication()) {
      LicenseManager.verifyAuthenticationCapability();
    }
  }

  private static OrderedGroupIDs createOrderedGroupIds(List<ActiveServerGroupConfig> groups) {
    GroupID[] gids = new GroupID[groups.size()];
    for (int i = 0; i < groups.size(); i++) {
      gids[i] = groups.get(i).getGroupId();
    }
    return new OrderedGroupIDs(gids);
  }

  @Override
  public ServerGroupInfo[] serverGroups() {
    L2Info[] l2Infos = infoForAllL2s();
    List<ActiveServerGroupConfig> groups = this.configurationSetupManager.activeServerGroupsConfig()
        .getActiveServerGroups();
    OrderedGroupIDs orderedGroupsIds = createOrderedGroupIds(groups);
    GroupID coordinatorId = orderedGroupsIds.getActiveCoordinatorGroup();
    ServerGroupInfo[] result = new ServerGroupInfo[groups.size()];
    for (int i = 0; i < groups.size(); i++) {
      ActiveServerGroupConfig groupInfo = groups.get(i);
      GroupID groupId = groupInfo.getGroupId();
      List<L2Info> memberList = new ArrayList<L2Info>();
      for (L2Info l2Info : l2Infos) {
        if (groupInfo.isMember(l2Info.name())) {
          memberList.add(l2Info);
        }
      }
      result[i] = new ServerGroupInfo(memberList.toArray(new L2Info[0]), groupInfo.getGroupName(), groupId.toInt(),
                                      coordinatorId.equals(groupId));
    }
    return result;
  }

  @Override
  public L2Info[] infoForAllL2s() {
    String[] allKnownL2s = this.configurationSetupManager.allCurrentlyKnownServers();
    L2Info[] out = new L2Info[allKnownL2s.length];

    for (int i = 0; i < out.length; ++i) {
      try {
        CommonL2Config config = this.configurationSetupManager.commonL2ConfigFor(allKnownL2s[i]);

        String name = allKnownL2s[i];
        if (name == null) {
          name = L2Info.IMPLICIT_L2_NAME;
        }

        String host = config.jmxPort().getBind();
        if (TCSocketAddress.WILDCARD_IP.equals(host)) {
          host = config.host();
        }
        if (StringUtils.isBlank(host)) {
          host = name;
        }

        out[i] = new L2Info(name, host, config.jmxPort().getIntValue(), config.tsaPort().getIntValue(), config
            .tsaGroupPort().getBind(), config.tsaGroupPort().getIntValue(), getSecurityHostname());
      } catch (ConfigurationSetupException cse) {
        throw Assert.failure("This should be impossible here", cse);
      }
    }

    return out;
  }

  @Override
  public String getL2Identifier() {
    return configurationSetupManager.getL2Identifier();
  }

  @Override
  public String getDescriptionOfCapabilities() {
    if (ProductInfo.getInstance().isEnterprise()) {
      return LicenseManager.licensedCapabilities();
    } else {
      return "Open source capabilities";
    }
  }

  /**
   * I realize this is wrong, since the server can still be starting but we'll have to deal with the whole stopping
   * issue later, and there's the TCStop feature which should be removed.
   */
  @Override
  public void stop() {
    synchronized (this.stateLock) {
      if (!this.state.isStartState()) {
        stopServer();
        logger.info("Server stopped.");
      } else {
        logger.warn("Server in incorrect state (" + this.state.getState() + ") to be stopped.");
      }
    }

  }

  @Override
  public void start() {
    synchronized (this.stateLock) {
      if (this.state.isStartState()) {
        try {
          startServer();
        } catch (Throwable t) {
          if (t instanceof RuntimeException) { throw (RuntimeException) t; }
          throw new RuntimeException(t);
        }
      } else {
        logger.warn("Server in incorrect state (" + this.state.getState() + ") to be started.");
      }
    }
  }

  @Override
  public boolean canShutdown() {
    return state.isPassiveStandby() || state.isActiveCoordinator() || state.isPassiveUninitialized();
  }

  @Override
  public synchronized void shutdown() {
    if (canShutdown()) {
      this.state.setState(StateManager.STOP_STATE);
      consoleLogger.info("Server exiting...");
      notifyShutdown();
      Runtime.getRuntime().exit(0);
    } else {
      logger.warn("Server in incorrect state (" + this.state.getState() + ") to be shutdown.");
    }
  }

  @Override
  public long getStartTime() {
    return this.startTime;
  }

  @Override
  public void updateActivateTime() {
    if (this.activateTime == -1) {
      this.activateTime = System.currentTimeMillis();
    }
  }

  @Override
  public long getActivateTime() {
    return this.activateTime;
  }

  @Override
  public boolean isGarbageCollectionEnabled() {
    return this.configurationSetupManager.dsoL2Config().garbageCollection().getEnabled();
  }

  @Override
  public int getGarbageCollectionInterval() {
    return this.configurationSetupManager.dsoL2Config().garbageCollection().getInterval();
  }

  @Override
  public String getConfig() {
    try {
      InputStream is = this.configurationSetupManager.rawConfigFile();
      return IOUtils.toString(is);
    } catch (IOException ioe) {
      return ioe.getLocalizedMessage();
    }
  }

  @Override
  public boolean getRestartable() {
    return configurationSetupManager.dsoL2Config().getRestartable().getEnabled();
  }

  @Override
  public int getTSAListenPort() {
    if (this.dsoServer != null) { return this.dsoServer.getListenPort(); }
    throw new IllegalStateException("TSA Server not running");
  }

  @Override
  public int getTSAGroupPort() {
    if (this.dsoServer != null) { return this.dsoServer.getGroupPort(); }
    throw new IllegalStateException("TSA Server not running");
  }

  public DistributedObjectServer getDSOServer() {
    return this.dsoServer;
  }

  @Override
  public boolean isStarted() {
    return !this.state.isStartState();
  }

  @Override
  public boolean isActive() {
    return this.state.isActiveCoordinator();
  }

  @Override
  public boolean isStopped() {
    // XXX:: introduce a new state when stop is officially supported.
    return this.state.isStartState();
  }

  @Override
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

    if (this.terracottaConnector != null) {
      try {
        this.terracottaConnector.shutdown();
      } catch (Exception e) {
        logger.error("Error shutting down terracotta connector", e);
      } finally {
        this.terracottaConnector = null;
      }
    }

    try {
      getStageManager().stopAll();
    } catch (Exception e) {
      logger.error("Error shutting down stage manager", e);
    }

    if (this.httpServer != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Shutting down HTTP server...");
      }

      try {
        this.httpServer.stop();
      } catch (Exception e) {
        logger.error("Error shutting down HTTP server", e);
      } finally {
        this.httpServer = null;
      }
    }

    // this stops the jmx server then dso server
    if (this.dsoServer != null) {
      try {
        this.dsoServer.quickStop();
      } catch (Exception e) {
        logger.error("Error shutting down TSA server", e);
      } finally {
        this.dsoServer = null;
      }
    }

  }

  private class StartAction implements StartupAction {
    @Override
    public void execute() throws Throwable {
      if (logger.isDebugEnabled()) {
        logger.debug("Starting Terracotta server instance...");
      }

      TCServerImpl.this.startTime = System.currentTimeMillis();

      CommonL2Config commonL2Config = TCServerImpl.this.configurationSetupManager.commonl2Config();

      if (Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
        consoleLogger.info("Available Max Runtime Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB");
      }

      TCServerImpl.this.terracottaConnector = new TerracottaConnector(
                                                                      TCServerImpl.this.configurationSetupManager
                                                                          .getSecurity() != null);
      startHTTPServer(commonL2Config, TCServerImpl.this.terracottaConnector);
      Stage stage = getStageManager().createStage("dso-http-bridge",
                                                  new HttpConnectionHandler(TCServerImpl.this.terracottaConnector), 1,
                                                  100);
      stage.start(new NullContext(getStageManager()));

      // the following code starts the jmx server as well
      startDSOServer(stage.getSink());

      File warTempDir = new File(commonL2Config.dataPath(), "jetty");
      prepareJettyWarTempDir(warTempDir);
      addManagementWebApp(warTempDir);

      if (isActive()) {
        updateActivateTime();
        if (TCServerImpl.this.activationListener != null) {
          TCServerImpl.this.activationListener.serverActivated();
        }
      }

      if (updateCheckEnabled()) {
        UpdateCheckAction.start(updateCheckPeriodDays(), getMaxDataSize());
      }

      String l2Identifier = TCServerImpl.this.configurationSetupManager.getL2Identifier();
      if (l2Identifier != null) {
        logger.info("Server started as " + l2Identifier);
      }
    }

    private void prepareJettyWarTempDir(File warTempDir) throws IOException {
      if (warTempDir.isDirectory()) {
        FileUtils.deleteDirectory(warTempDir);
      }
      warTempDir.mkdirs();
      if (!warTempDir.isDirectory() || !warTempDir.canWrite()) { throw new IOException(
                                                                                       "Can't create jetty temp dir at "
                                                                                           + warTempDir); }
    }

  }

  private long getMaxDataSize() {
    long maxOffheap = 0L;
    DataStorage datastore = configurationSetupManager.dsoL2Config().getDataStorage();
    try {
      maxOffheap = Conversion.memorySizeAsLongBytes(datastore.getSize());
    } catch (MetricsFormatException e) {
      throw new TCRuntimeException("Problem converting max data size: ", e);
    }
    return maxOffheap;
  }

  private boolean updateCheckEnabled() {
    boolean skipped = Boolean.getBoolean("com.tc.skipUpdateCheck");
    return !skipped && this.configurationSetupManager.updateCheckConfig().getUpdateCheck().getEnabled();
  }

  private int updateCheckPeriodDays() {
    return this.configurationSetupManager.updateCheckConfig().getUpdateCheck().getPeriodDays();
  }

  protected void startServer() throws Exception {
    new StartupHelper(getThreadGroup(), new StartAction()).startUp();
  }

  private void startDSOServer(final Sink httpSink) throws Exception {
    Assert.assertTrue(this.state.isStartState());
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    ObjectStatsRecorder objectStatsRecorder = new ObjectStatsRecorder(
                                                                      tcProps
                                                                          .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_REQUEST_LOGGING_ENABLED),
                                                                      tcProps
                                                                          .getBoolean(TCPropertiesConsts.L2_TRANSACTIONMANAGER_LOGGING_PRINT_BROADCAST_STATS),
                                                                      tcProps
                                                                          .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_PERSISTOR_LOGGING_ENABLED));

    this.dsoServer = createDistributedObjectServer(this.configurationSetupManager, this.connectionPolicy, httpSink,
                                                   new TCServerInfo(this, this.state, objectStatsRecorder),
                                                   objectStatsRecorder, this.state, this);
    this.dsoServer.start();
    registerDSOServer();
  }

  protected DistributedObjectServer createDistributedObjectServer(L2ConfigurationSetupManager configSetupManager,
                                                                  ConnectionPolicy policy, Sink httpSink,
                                                                  TCServerInfo serverInfo,
                                                                  ObjectStatsRecorder objectStatsRecorder,
                                                                  L2State l2State, TCServerImpl serverImpl) {
    return new DistributedObjectServer(configSetupManager, getThreadGroup(), policy, httpSink, serverInfo,
                                       objectStatsRecorder, l2State, this, this, securityManager);
  }

  private void startHTTPServer(final CommonL2Config commonL2Config, final TerracottaConnector tcConnector)
      throws Exception {
    this.httpServer = new Server();
    this.httpServer.setSendServerVersion(false);
    this.httpServer.addConnector(tcConnector);
    this.contextHandlerCollection = new ContextHandlerCollection();

    ServletContextHandler context = new ServletContextHandler(null, "/", ServletContextHandler.NO_SESSIONS
                                                                         | ServletContextHandler.SECURITY);

    if (commonL2Config.isSecure()) {
      final String pathSpec = "/*";
      final TCUserRealm userRealm = new TCUserRealm(securityManager);
      setupBasicAuth(context, pathSpec, userRealm, HTTP_SECURITY_ROLE);
      logger.info("HTTPS Authentication enabled for path '" + pathSpec + "'");
    } else if (commonL2Config.httpAuthentication()) {
      final HashLoginService userRealm = new HashLoginService("Terracotta Statistics Gatherer",
                                                              commonL2Config.httpAuthenticationUserRealmFile());
      setupBasicAuth(context, STATISTICS_GATHERER_SERVLET_PATH, userRealm, HTTP_AUTHENTICATION_ROLE_STATISTICS);
      logger.info("HTTP Authentication enabled for path '" + STATISTICS_GATHERER_SERVLET_PATH
                  + "', using user realm file '" + commonL2Config.httpAuthenticationUserRealmFile() + "'");
    }

    context.setAttribute(ConfigServlet.CONFIG_ATTRIBUTE, this.configurationSetupManager);
    context.setAttribute(GroupInfoServlet.GROUP_INFO_ATTRIBUTE, this.configurationSetupManager);
    context.setAttribute(GroupIDMapServlet.GROUPID_MAP_ATTRIBUTE, this.configurationSetupManager);

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
    if (tcInstallDir != null && tcInstallDir.exists() && tcInstallDir.isDirectory() && tcInstallDir.canRead()) {
      tcInstallDirValid = true;
      resourceBaseDir = tcInstallDir;
    }
    context.setResourceBase(resourceBaseDir.getAbsolutePath());

    createAndAddServlet(servletHandler, VersionServlet.class.getName(), VERSION_SERVLET_PATH);
    createAndAddServlet(servletHandler, ConfigServlet.class.getName(), CONFIG_SERVLET_PATH);
    createAndAddServlet(servletHandler, GroupInfoServlet.class.getName(), GROUP_INFO_SERVLET_PATH);
    createAndAddServlet(servletHandler, GroupIDMapServlet.class.getName(), GROUPID_MAP_SERVLET_PATH);
    createAndAddServlet(servletHandler, L1ReconnectPropertiesServlet.class.getName(),
                        L1_RECONNECT_PROPERTIES_FROML2_SERVELET_PATH);

    if (TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.HTTP_DEFAULT_SERVLET_ENABLED, false)) {
      if (!tcInstallDirValid) {
        String msg = "Default HTTP servlet with file serving NOT enabled because the '"
                     + Directories.TC_INSTALL_ROOT_PROPERTY_NAME + "' system property is invalid.";
        consoleLogger.warn(msg);
        logger.warn(msg);
      } else {
        boolean aliases = TCPropertiesImpl.getProperties()
            .getBoolean(TCPropertiesConsts.HTTP_DEFAULT_SERVLET_ATTRIBUTE_ALIASES, false);
        boolean dirallowed = TCPropertiesImpl.getProperties()
            .getBoolean(TCPropertiesConsts.HTTP_DEFAULT_SERVLET_ATTRIBUTE_DIR_ALLOWED, false);
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
    contextHandlerCollection.addHandler(context);

    this.httpServer.setHandler(contextHandlerCollection);
    this.httpServer.setSessionIdManager(new TcHashSessionIdManager());

    try {
      this.httpServer.start();
    } catch (Exception e) {
      consoleLogger.warn("Couldn't start HTTP server", e);
      throw e;
    }
  }

  private void addManagementWebApp(File warTempDir) throws Exception {
    if (!TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.MANAGEMENT_REST_ENABLED, true)) {
      logger.info("RestManagement is disabled.");
      return;
    }
    // register REST webapp
    String warFile = System.getProperty("com.tc.management.war");
    String failureReason = null;
    if (warFile == null) {
      final String webAppPrefix = "management-tsa-war";
      File tcInstallDir;
      try {
        File managementDir = Directories.getServerLibFolder();

        String[] files = managementDir.list(new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return name.startsWith(webAppPrefix) && name.endsWith(".war");
          }
        });

        if (files != null && files.length > 0) {
          warFile = managementDir.getPath() + File.separator + files[0];
        } else {
          failureReason = "Could not find the management web archive named : " + webAppPrefix;
        }
      } catch (FileNotFoundException e) {
        failureReason = "Could not find the management web archive " + e.getMessage();
      }
    }

    if (warFile != null) {
      // DEV-8583: use a lock file in java.io.tmpdir to serialize deployments of the management webapp on a single
      // server
      fileLock();
      try {
        logger.info("deploying management REST services from archive " + warFile);
        WebAppContext restContext = new WebAppContext();
        restContext.setTempDirectory(warTempDir);

        // DEV-8020: add slf4j to the web app's system classes to avoid "multiple bindings" warning
        List<String> systemClasses = new ArrayList<String>(Arrays.asList(restContext.getSystemClasses()));
        systemClasses.add("org.slf4j.");
        restContext.setSystemClasses(systemClasses.toArray(new String[systemClasses.size()]));

        restContext.setContextPath("/tc-management-api");
        restContext.setWar(warFile);
        contextHandlerCollection.addHandler(restContext);

        if (contextHandlerCollection.isStarted()) {
          restContext.start();
        }
      } finally {
        fileUnlock();
      }
    } else {
        // there is no more hope of deploying the web app
        logger.info("impossible to deploy the webapp due to invalid installation dir location");
        logger.info(failureReason);
    }
  }

  private void fileLock() throws InterruptedException, IOException {
    while (true) {
      // check if the lock file is older than 60s, in that case assume another VM crashed and delete it
      if (TC_MANAGEMENT_API_LOCKFILE.lastModified() != 0L
          && System.currentTimeMillis() - TC_MANAGEMENT_API_LOCKFILE.lastModified() >= 60000) {
        TC_MANAGEMENT_API_LOCKFILE.delete();
      }

      // check if the file exists and if not, create it. That's atomic across the entire FS.
      boolean created = TC_MANAGEMENT_API_LOCKFILE.createNewFile();
      if (created) {
        break;
      }

      // if the file exists and is not older than 1 minute, sleep a bit and retry
      Thread.sleep(1000);
    }
  }

  private void fileUnlock() {
    // deleting the file unblocks all processes waiting to create it
    TC_MANAGEMENT_API_LOCKFILE.delete();
  }

  private void setupBasicAuth(final ServletContextHandler context, final String pathSpec,
                              final LoginService loginService, String... roles) {
    Constraint constraint = new Constraint();
    constraint.setName(Constraint.__BASIC_AUTH);
    constraint.setRoles(roles);
    constraint.setAuthenticate(true);

    ConstraintMapping cm = new ConstraintMapping();
    cm.setConstraint(constraint);
    cm.setPathSpec(pathSpec);

    ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
    sh.setAuthenticator(new BasicAuthenticator());
    sh.setRealmName(TCUserRealm.class.getSimpleName());
    sh.setLoginService(loginService);
    sh.setConstraintMappings(Collections.singletonList(cm));

    context.setSecurityHandler(sh);
  }

  private static void createAndAddServlet(final ServletHandler servletHandler, final String servletClassName,
                                          final String path) {
    ServletHolder holder = servletHandler.addServletWithMapping(servletClassName, path);
    holder.setInitParameter("scratchdir", "jsp"); // avoid jetty from creating a "jsp" directory
    servletHandler.addServlet(holder);
  }

  @Override
  public void dump() {
    if (this.dsoServer != null) {
      this.dsoServer.dump();
    }
  }

  private void registerDSOServer() throws InstanceAlreadyExistsException, MBeanRegistrationException,
      NotCompliantMBeanException, NullPointerException {

    ServerManagementContext mgmtContext = this.dsoServer.getManagementContext();
    ServerConfigurationContext configContext = this.dsoServer.getContext();
    MBeanServer mBeanServer = this.dsoServer.getMBeanServer();
    registerDSOMBeans(mgmtContext, configContext, mBeanServer);
  }

  protected void registerDSOMBeans(ServerManagementContext mgmtContext, ServerConfigurationContext configContext,
                                   MBeanServer mBeanServer) throws NotCompliantMBeanException,
      InstanceAlreadyExistsException, MBeanRegistrationException {
    GCStatsEventPublisher gcStatsPublisher = this.dsoServer.getGcStatsEventPublisher();
    TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider = this.dsoServer
        .getOperatorEventsHistoryProvider();
    DSOMBean dso = new DSO(mgmtContext, configContext, mBeanServer, gcStatsPublisher, operatorEventHistoryProvider,
                           this.dsoServer.getOffheapStats(), this.dsoServer.getStorageStats());
    mBeanServer.registerMBean(dso, L2MBeanNames.DSO);
  }

  // TODO: check that this is not needed then remove
  private TCServerActivationListener activationListener;

  public void setActivationListener(final TCServerActivationListener listener) {
    this.activationListener = listener;
  }

  private static class NullContext implements ConfigurationContext {

    private final StageManager manager;

    public NullContext(final StageManager manager) {
      this.manager = manager;
    }

    @Override
    public TCLogger getLogger(final Class clazz) {
      return TCLogging.getLogger(clazz);
    }

    @Override
    public Stage getStage(final String name) {
      return this.manager.getStage(name);
    }

  }

  @Override
  public void startBeanShell(final int port) {
    if (this.dsoServer != null) {
      this.dsoServer.startBeanShell(port);
    }
  }

  private synchronized void notifyShutdown() {
    shutdown = true;
    notifyAll();
  }

  @Override
  public synchronized void waitUntilShutdown() {
    while (!shutdown) {
      try {
        wait();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }
  }

  @Override
  public void reloadConfiguration() throws ConfigurationSetupException {
    dsoServer.reloadConfiguration();
  }

  @Override
  public String[] processArguments() {
    return configurationSetupManager.processArguments();
  }

  @Override
  public void dumpClusterState() {
    if (this.dsoServer != null) {
      this.dsoServer.dumpClusterState();
    }
  }

  @Override
  public String getRunningBackup() {
    return dsoServer.getBackupManager().getRunningBackup();
  }

  @Override
  public String getBackupStatus(final String name) throws IOException {
    return dsoServer.getBackupManager().getBackupStatus(name).toString();
  }

  @Override
  public String getBackupFailureReason(String name) throws IOException {
    return dsoServer.getBackupManager().getBackupFailureReason(name);
  }

  @Override
  public Map<String, String> getBackupStatuses() throws IOException {
    Map<String, String> result = new HashMap<String, String>();
    Map<String, ? extends Object> backups = dsoServer.getBackupManager().getBackupStatuses();
    for (String name : backups.keySet()) {
      result.put(name, backups.get(name).toString());
    }
    return result;
  }

  @Override
  public void backup(final String name) throws IOException {
    dsoServer.getBackupManager().backup(name);
  }

  @Override
  public String getResourceState() {
    return dsoServer.getResourceManager().getState().name();
  }

  @Override
  public boolean isSecure() {
    return securityManager != null;
  }

  @Override
  public String getSecurityServiceLocation() {
    if (configurationSetupManager.getSecurity() == null) { return null; }
    return configurationSetupManager.getSecurity().getSecurityServiceLocation();
  }

  @Override
  public Integer getSecurityServiceTimeout() {
    if (configurationSetupManager.getSecurity() == null) { return null; }
    return configurationSetupManager.getSecurity().getSecurityServiceTimeout();
  }

  @Override
  public String getSecurityHostname() {
    String securityHostname = null;
    if (configurationSetupManager.getSecurity() != null) {
      securityHostname = configurationSetupManager.getSecurity().getSecurityHostname();
    }
    if (securityHostname == null) {
      securityHostname = configurationSetupManager.commonl2Config().host();
    }
    return securityHostname;
  }

  @Override
  public String getIntraL2Username() {
    if (!isSecure()) { return null; }
    return securityManager.getIntraL2Username();
  }

  private static final class TCUserRealm extends MappedLoginService {
    private final TCSecurityManager securityManager;

    TCUserRealm(final TCSecurityManager securityManager) {
      this.securityManager = securityManager;
      setName(this.getClass().getSimpleName());
    }

    @Override
    public UserIdentity login(final String username, final Object credentials) {
      final Principal authenticatedUser = securityManager.authenticate(username, ((String) credentials).toCharArray());
      if (authenticatedUser == null) { return null; }

      UserIdentity user = _users.get(username);
      if (user == null) {
        String[] roles = new String[0];
        if (securityManager.isUserInRole(authenticatedUser, HTTP_SECURITY_ROLE)) {
          roles = new String[] { HTTP_SECURITY_ROLE };
        }
        user = putUser(username, new Password((String) credentials), roles);
      }
      return user;
    }

    @Override
    protected UserIdentity loadUser(String username) {
      return null;
    }

    @Override
    protected void loadUsers() {
      /**/
    }
  }
}
