/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.server;

import org.apache.commons.io.IOUtils;

import com.tc.async.api.SEDA;
import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.lang.StartupHelper;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2Dumper;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCDumper;
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
import com.tc.util.State;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
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


public class TCServerImpl extends SEDA<HttpConnectionContext> implements TCServer, StateChangeListener {
  public static final String                CONNECTOR_NAME_TERRACOTTA                    = "terracotta";

  private static final TCLogger             logger                                       = TCLogging
                                                                                             .getLogger(TCServer.class);
  private static final TCLogger             consoleLogger                                = CustomerLogging
                                                                                             .getConsoleLogger();

  private volatile long                     startTime                                    = -1;
  private volatile long                     activateTime                                 = -1;

  protected DistributedObjectServer         dsoServer;

  private final Object                      stateLock                                    = new Object();
  private State                             serverState                                  = StateManager.START_STATE;
  
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
  
  public synchronized void setState(State state) {
    if (!validateState(state)) { throw new AssertionError("Unrecognized server state: [" + state.getName() + "]"); }

    serverState = state;
  }

  private boolean validateState(State state) {
    return StateManager.validStates.contains(state);
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
      return "Enterprise capabilities";
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
      if (this.isStopped()) {
        stopServer();
        logger.info("Server stopped.");
      } else {
        logger.warn("Server in incorrect state (" + this.serverState + ") to be stopped.");
      }
    }

  }

  @Override
  public void start() {
    synchronized (this.stateLock) {
      if (!this.isStarted()) {
        try {
          startServer();
        } catch (Throwable t) {
          if (t instanceof RuntimeException) { throw (RuntimeException) t; }
          throw new RuntimeException(t);
        }
      } else {
        logger.warn("Server in incorrect state (" + this.serverState + ") to be started.");
      }
    }
  }

  @Override
  public boolean canShutdown() {
    synchronized (this.stateLock) {
      return serverState.equals(StateManager.PASSIVE_STANDBY) ||
       serverState.equals(StateManager.ACTIVE_COORDINATOR) || 
       serverState.equals(StateManager.PASSIVE_UNINITIALIZED);
    }
  }

  @Override
  public synchronized void shutdown() {
    if (canShutdown()) {
      setState(StateManager.STOP_STATE);
      consoleLogger.info("Server exiting...");
      notifyShutdown();
      Runtime.getRuntime().exit(0);
    } else {
      logger.warn("Server in incorrect state (" + serverState + ") to be shutdown.");
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
    synchronized (this.stateLock) {
      return !this.serverState.equals(StateManager.START_STATE);
    }
  }

  @Override
  public boolean isActive() {
    synchronized (this.stateLock) {
      return this.serverState.equals(StateManager.ACTIVE_COORDINATOR);
    }
  }

  @Override
  public boolean isStopped() {
    // XXX:: introduce a new state when stop is officially supported.
    synchronized (this.stateLock) {
      return this.serverState.equals(StateManager.START_STATE);
    }
  }

  @Override
  public boolean isPassiveUnitialized() {
    synchronized (this.stateLock) {
      return this.serverState.equals(StateManager.PASSIVE_UNINITIALIZED);
    }
  }

  @Override
  public boolean isPassiveStandby() {
    synchronized (this.stateLock) {
      return this.serverState.equals(StateManager.PASSIVE_STANDBY);
    }
  }

  @Override
  public boolean isRecovering() {
    synchronized (this.stateLock) {
      return this.serverState.equals(StateManager.RECOVERING);
    }
  }

  @Override
  public State getState() {
    return this.serverState;
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
    Assert.assertTrue(this.isStopped());
    this.dsoServer = createDistributedObjectServer(this.configurationSetupManager, this.connectionPolicy, this);
    this.dsoServer.start();
    registerDSOServer(dsoServer);
  }

  protected DistributedObjectServer createDistributedObjectServer(L2ConfigurationSetupManager configSetupManager,
                                                                  ConnectionPolicy policy, 
                                                                  TCServerImpl serverImpl) {
    DistributedObjectServer dso = new DistributedObjectServer(configSetupManager, getThreadGroup(), policy, this, this, securityManager);
    try {
      registerServerMBeans(dso, ManagementFactory.getPlatformMBeanServer());
    } catch (NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException exp) {
      throw new RuntimeException(exp);
    }
    return dso;
  }

  @Override
  public void dump() {
    if (this.dsoServer != null) {
      this.dsoServer.dump();
    }
  }

  private void registerDSOServer(TCDumper dumper) throws InstanceAlreadyExistsException, MBeanRegistrationException,
      NotCompliantMBeanException, NullPointerException {

    ServerManagementContext mgmtContext = this.dsoServer.getManagementContext();
    ServerConfigurationContext configContext = this.dsoServer.getContext();
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    registerDSOMBeans(mgmtContext, configContext, dumper, mBeanServer);
  }
  
  protected void registerServerMBeans(TCDumper tcDumper, MBeanServer mBeanServer) 
      throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
    mBeanServer.registerMBean(new TCServerInfo(this), L2MBeanNames.TC_SERVER_INFO);
    mBeanServer.registerMBean(new L2Dumper(tcDumper, mBeanServer), L2MBeanNames.DUMPER);
  }
  
  protected void unregisterServerMBeans(MBeanServer mbs) throws MBeanRegistrationException, InstanceNotFoundException {
    mbs.unregisterMBean(L2MBeanNames.TC_SERVER_INFO);
    mbs.unregisterMBean(L2MBeanNames.DUMPER);
  }
  protected void registerDSOMBeans(ServerManagementContext mgmtContext, ServerConfigurationContext configContext, TCDumper tcDumper,
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

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    synchronized (this.stateLock) {
      this.serverState = sce.getCurrentState();
    }
  } 
}
