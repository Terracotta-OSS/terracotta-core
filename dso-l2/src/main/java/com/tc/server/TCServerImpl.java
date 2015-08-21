/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import org.apache.commons.io.IOUtils;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.SEDA;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.l2.state.StateManager;
import com.tc.lang.StartupHelper;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
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
import com.tc.net.protocol.HttpConnectionContext;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.ConnectionPolicyImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.stats.DSO;
import com.tc.stats.api.DSOMBean;
import com.tc.text.StringUtils;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

public class TCServerImpl extends SEDA<HttpConnectionContext> implements TCServer {
  public static final String                CONNECTOR_NAME_TERRACOTTA                    = "terracotta";

  private static final TCLogger             logger                                       = TCLogging
                                                                                             .getLogger(TCServer.class);
  private static final TCLogger             consoleLogger                                = CustomerLogging
                                                                                             .getConsoleLogger();

  private volatile long                     startTime                                    = -1;
  private volatile long                     activateTime                                 = -1;

  protected DistributedObjectServer         dsoServer;

  private final Object                      stateLock                                    = new Object();
  private final L2State                     state                                        = new L2State();

  private final L2ConfigurationSetupManager configurationSetupManager;
  protected final ConnectionPolicy          connectionPolicy;
  private boolean                           shutdown                                     = false;
  protected final TCSecurityManager         securityManager;

  /**
   * This should only be used for tests.
   */
  public TCServerImpl(L2ConfigurationSetupManager configurationSetupManager) {
    this(configurationSetupManager, new TCThreadGroup(new ThrowableHandlerImpl(TCLogging.getLogger(TCServer.class))));
  }

  public TCServerImpl(L2ConfigurationSetupManager configurationSetupManager, TCThreadGroup threadGroup) {
    this(configurationSetupManager, threadGroup, new ConnectionPolicyImpl(Integer.MAX_VALUE));
  }

  public TCServerImpl(L2ConfigurationSetupManager manager, TCThreadGroup group,
                      ConnectionPolicy connectionPolicy) {
    super(group);

    this.connectionPolicy = connectionPolicy;
    Assert.assertNotNull(manager);
    this.configurationSetupManager = manager;

    if (configurationSetupManager.isSecure()) {
// no security implemention
      this.securityManager = null;
    } else {
      this.securityManager = null;
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
      List<L2Info> memberList = new ArrayList<>();
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

        String host = config.tsaPort().getBind();
        if (TCSocketAddress.WILDCARD_IP.equals(host)) {
          host = config.host();
        }
        if (StringUtils.isBlank(host)) {
          host = name;
        }
        //XXX hard coded jmx port
        out[i] = new L2Info(name, host, config.tsaPort().getValue()+10, config.tsaPort().getValue(), config
            .tsaGroupPort().getBind(), config.tsaGroupPort().getValue(),config.tsaGroupPort().getValue() + 1,
                            getSecurityHostname());
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
  public String getConfig() {
    InputStream is = null;
    try {
      is = this.configurationSetupManager.rawConfigFile();
      return IOUtils.toString(is);
    } catch (IOException ioe) {
      return ioe.getLocalizedMessage();
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  @Override
  public boolean getRestartable() {
    return configurationSetupManager.dsoL2Config().getRestartable();
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

    try {
      unregisterDSOMBeans(this.dsoServer.getMBeanServer());
    } catch (Exception e) {
      logger.error("Error unregistering mbeans", e);
    }

    try {
      getStageManager().stopAll();
    } catch (Exception e) {
      logger.error("Error shutting down stage manager", e);
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

      if (Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
        consoleLogger.info("Available Max Runtime Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB");
      }

      // the following code starts the jmx server as well
      startDSOServer();

      if (isActive()) {
        updateActivateTime();
        if (TCServerImpl.this.activationListener != null) {
          TCServerImpl.this.activationListener.serverActivated();
        }
      }

      String l2Identifier = TCServerImpl.this.configurationSetupManager.getL2Identifier();
      if (l2Identifier != null) {
        logger.info("Server started as " + l2Identifier);
      }
    }
  }

  protected void startServer() throws Exception {
    new StartupHelper(getThreadGroup(), new StartAction()).startUp();
  }

  private void startDSOServer() throws Exception {
    Assert.assertTrue(this.state.isStartState());

    this.dsoServer = createDistributedObjectServer(this.configurationSetupManager, this.connectionPolicy, 
                                                   new TCServerInfo(this, this.state),
                                                   this.state, this);
    this.dsoServer.start();
    registerDSOServer();
  }

  protected DistributedObjectServer createDistributedObjectServer(L2ConfigurationSetupManager configSetupManager,
                                                                  ConnectionPolicy policy, 
                                                                  TCServerInfo serverInfo,
                                                                  L2State l2State, TCServerImpl serverImpl) {
    return new DistributedObjectServer(configSetupManager, getThreadGroup(), policy, serverInfo,
                                       l2State, this, this, securityManager);
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
    TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider = this.dsoServer
        .getOperatorEventsHistoryProvider();
    DSOMBean dso = new DSO(mgmtContext, configContext, mBeanServer, operatorEventHistoryProvider);
    mBeanServer.registerMBean(dso, L2MBeanNames.DSO);
  }

  protected void unregisterDSOMBeans(MBeanServer mbs) throws MBeanRegistrationException, InstanceNotFoundException {
    mbs.unregisterMBean(L2MBeanNames.DSO);
  }

  // TODO: check that this is not needed then remove
  private TCServerActivationListener activationListener;

  public void setActivationListener(TCServerActivationListener listener) {
    this.activationListener = listener;
  }

  private static class NullContext implements ConfigurationContext {

    private final StageManager manager;

    public NullContext(StageManager manager) {
      this.manager = manager;
    }

    @Override
    public TCLogger getLogger(Class<?> clazz) {
      return TCLogging.getLogger(clazz);
    }

    @Override
    public <EC> Stage<EC> getStage(String name, Class<EC> verification) {
      return this.manager.getStage(name, verification);
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
    return "";
  }

  @Override
  public String getBackupStatus(String name) throws IOException {
    return "";
  }

  @Override
  public String getBackupFailureReason(String name) throws IOException {
    return "";
  }

  @Override
  public Map<String, String> getBackupStatuses() throws IOException {
    Map<String, String> result = new HashMap<>();
    return result;
  }

  @Override
  public void backup(String name) throws IOException {
  }

  @Override
  public String getResourceState() {
    return "";
  }

  @Override
  public boolean isSecure() {
    return securityManager != null;
  }

  @Override
  public String getSecurityServiceLocation() {
    return null;
  }

  @Override
  public Integer getSecurityServiceTimeout() {
    return null;
  }

  @Override
  public String getSecurityHostname() {
    String securityHostname = null;
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
}
