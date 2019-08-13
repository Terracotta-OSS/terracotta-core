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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.monitoring.PlatformService.RestartMode;
import org.terracotta.monitoring.PlatformStopException;

import com.tc.async.api.SEDA;
import com.tc.config.ServerConfigurationManager;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateManager;
import com.tc.lang.ServerExitStatus;
import com.tc.lang.StartupHelper;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2Dumper;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfo;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.ConnectionPolicyImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.stats.DSO;
import com.tc.stats.api.DSOMBean;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;
import com.tc.util.State;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.Date;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import com.tc.objectserver.core.api.Guardian;
import com.tc.objectserver.core.api.GuardianContext;
import com.tc.text.PrettyPrinter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;


public class TCServerImpl extends SEDA implements TCServer {
  private static final Logger logger = LoggerFactory.getLogger(TCServer.class);
  private static final Logger consoleLogger = TCLogging.getConsoleLogger();

  private volatile long                     startTime                                    = -1;
  private volatile long                     activateTime                                 = -1;

  private DistributedObjectServer         dsoServer;
  private StateManager                    stateManager;
  
  private final ServerConfigurationManager configurationSetupManager;
  protected final ConnectionPolicy          connectionPolicy;
  private boolean                           shutdown                                     = false;

  /**
   * This should only be used for tests.
   */
  public TCServerImpl(ServerConfigurationManager configurationSetupManager) {
    this(configurationSetupManager, new TCThreadGroup(new ThrowableHandlerImpl(logger)));
  }

  public TCServerImpl(ServerConfigurationManager configurationSetupManager, TCThreadGroup threadGroup) {
    this(configurationSetupManager, threadGroup, new ConnectionPolicyImpl(Integer.MAX_VALUE));
  }

  public TCServerImpl(ServerConfigurationManager manager, TCThreadGroup group,
                      ConnectionPolicy connectionPolicy) {
    super(group);

    this.connectionPolicy = connectionPolicy;
    Assert.assertNotNull(manager);
    this.configurationSetupManager = manager;
  }

  private boolean validateState(ServerMode state) {
    return ServerMode.VALID_STATES.contains(state);
  }

  @Override
  public String getL2Identifier() {
    return configurationSetupManager.getServerConfiguration().getName();
  }

  @Override
  public String getDescriptionOfCapabilities() {
    if (ProductInfo.getInstance().isEnterprise()) {
      return "Enterprise capabilities";
    } else {
      return "Open source capabilities";
    }
  }

  @Override
  public void stop() {
    stop(RestartMode.STOP_ONLY);
  }

  @Override
  public void stopIfPassive(RestartMode restartMode) throws PlatformStopException {
    if (stateManager.moveToStopStateIf(ServerMode.PASSIVE_STATES)) {
      stop(restartMode);
    } else {
      throw new UnexpectedStateException("Server is not in passive state, current state: " + stateManager.getCurrentMode());
    }
  }

  @Override
  public void stopIfActive(RestartMode restartMode) throws PlatformStopException {
    if (stateManager.moveToStopStateIf(EnumSet.of(ServerMode.ACTIVE))) {
      stop(restartMode);
    } else {
      throw new UnexpectedStateException("Server is not in active state, current state: " + stateManager.getCurrentMode());
    }
  }

  @Override
  public void stop(RestartMode restartMode) {
    dsoServer.getContext().getL2Coordinator().getStateManager().moveToStopState();
    if (restartMode == RestartMode.STOP_ONLY) {
      exitWithStatus(0);
    } else if (restartMode == RestartMode.STOP_AND_RESTART) {
      exitWithStatus(ServerExitStatus.EXITCODE_RESTART_REQUEST);
    } else {
      throw new IllegalArgumentException("Unknown RestartMode: " + restartMode);
    }
  }

  @Override
  public synchronized void start() {
    if (!this.isStarted()) {
      try {
        startServer();
      } catch (Throwable t) {
        if (t instanceof RuntimeException) { throw (RuntimeException) t; }
        throw new RuntimeException(t);
      }
    } else {
      logger.warn("Server in incorrect state (" + this.stateManager.getCurrentMode().getName() + ") to be started.");
    }
  }

  @Override
  public boolean canShutdown() {
    ServerMode serverState = stateManager.getCurrentMode();
    return serverState == ServerMode.PASSIVE ||
       serverState == ServerMode.ACTIVE || 
       serverState == ServerMode.UNINITIALIZED ||
       serverState == ServerMode.SYNCING;
  }

  @Override
  public synchronized void shutdown() {
    if (canShutdown()) {
      if (GuardianContext.validate(Guardian.Op.SERVER_EXIT, "shutdown")) {
        consoleLogger.info("Server exiting...");
        notifyShutdown();
        stop();
      } else {
        logger.info("shutdown operation not permitted by guardian");
      }
    } else {
      logger.warn("Server in incorrect state (" + stateManager.getCurrentMode().getName() + ") to be shutdown.");
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
    try (InputStream is = this.configurationSetupManager.rawConfigFile()) {
      ByteArrayOutputStream writer = new ByteArrayOutputStream();
      int c = is.read();
      while (c >= 0) {
        writer.write((byte)c);
        c = is.read();
      }
      return new String(writer.toByteArray(), Charset.defaultCharset());
    } catch (IOException ioe) {
      return ioe.getLocalizedMessage();
    }
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
  public synchronized boolean isStarted() {
    return this.stateManager != null && this.stateManager.getCurrentMode() != ServerMode.START;
  }

  @Override
  public boolean isActive() {
    return this.stateManager.isActiveCoordinator();
  }

  @Override
  public synchronized boolean isStopped() {
    // XXX:: introduce a new state when stop is officially supported.
    return this.stateManager == null || this.stateManager.getCurrentMode() != ServerMode.STOP;
  }

  @Override
  public boolean isPassiveUnitialized() {
    return this.stateManager.getCurrentMode() == ServerMode.UNINITIALIZED;
  }

  @Override
  public boolean isPassiveStandby() {
    return this.stateManager.getCurrentMode() == ServerMode.PASSIVE;
  }

  @Override
  public boolean isReconnectWindow() {
    return dsoServer.getContext().getClientHandshakeManager().isStarting();
  }

  @Override
  public int getReconnectWindowTimeout() {
    return configurationSetupManager.getServerConfiguration().getClientReconnectWindow();
  }
  
  @Override
  public State getState() {
    return this.stateManager.getCurrentMode().getState();
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
        if (TCServerImpl.this.activationListener != null) {
          TCServerImpl.this.activationListener.serverActivated();
        }
      }

      String serverName = TCServerImpl.this.configurationSetupManager.getServerConfiguration().getName();
      if (serverName != null) {
        logger.info("Server started as " + serverName);
      }
    }
  }

  protected void startServer() throws Exception {
    new StartupHelper(getThreadGroup(), new StartAction()).startUp();
  }

  private void startDSOServer() throws Exception {
    Assert.assertTrue(this.isStopped());
    DistributedObjectServer server = createDistributedObjectServer(this.configurationSetupManager, this.connectionPolicy, this);
    server.start();
    registerDSOServer(server, ManagementFactory.getPlatformMBeanServer());
  }

  protected DistributedObjectServer createDistributedObjectServer(ServerConfigurationManager configSetupManager,
                                                                  ConnectionPolicy policy,
                                                                  TCServerImpl serverImpl) {
    DistributedObjectServer dso = new DistributedObjectServer(configSetupManager, getThreadGroup(), policy, this, this);
    try {
      registerServerMBeans(dso, ManagementFactory.getPlatformMBeanServer());
    } catch (NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException exp) {
      throw new RuntimeException(exp);
    }
    return dso;
  }

  @Override
  public void dump() {
    if (GuardianContext.validate(Guardian.Op.SERVER_DUMP, "dump")) {
      TCLogging.getDumpLogger().info(new String(this.dsoServer.getClusterState(Charset.defaultCharset(), null), Charset.defaultCharset()));
    } else {
      logger.info("dump operation not permitted by guardian");
    }
  }

  protected synchronized void registerDSOServer(DistributedObjectServer server, MBeanServer mBeanServer) throws InstanceAlreadyExistsException, MBeanRegistrationException,
      NotCompliantMBeanException, NullPointerException {
    this.dsoServer = server;
    ServerManagementContext mgmtContext = this.dsoServer.getManagementContext();
    ServerConfigurationContext configContext = this.dsoServer.getContext();
    registerDSOMBeans(mgmtContext, configContext, server, mBeanServer);
    stateManager = dsoServer.getContext().getL2Coordinator().getStateManager();
  }
  
  protected void registerServerMBeans(DistributedObjectServer tcDumper, MBeanServer mBeanServer) 
      throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
    mBeanServer.registerMBean(new TCServerInfo(this), L2MBeanNames.TC_SERVER_INFO);
    mBeanServer.registerMBean(new L2Dumper(this, mBeanServer), L2MBeanNames.DUMPER);
  }
  
  protected void unregisterServerMBeans(MBeanServer mbs) throws MBeanRegistrationException, InstanceNotFoundException {
    mbs.unregisterMBean(L2MBeanNames.TC_SERVER_INFO);
    mbs.unregisterMBean(L2MBeanNames.DUMPER);
  }
  protected void registerDSOMBeans(ServerManagementContext mgmtContext, ServerConfigurationContext configContext, DistributedObjectServer tcDumper,
                                   MBeanServer mBeanServer) throws NotCompliantMBeanException,
      InstanceAlreadyExistsException, MBeanRegistrationException {
    DSOMBean dso = new DSO(mgmtContext, configContext, mBeanServer);
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
    return configurationSetupManager.getProcessArguments();
  }

  @Override
  public String getResourceState() {
    return "";
  }

  private void exitWithStatus(int status) {
    if (GuardianContext.validate(Guardian.Op.SERVER_EXIT, "stop")) {
      Runtime.getRuntime().exit(status);
    } else {
      logger.info("stop operation not allowed by guardian");
    }
  }

  @Override
  public String getClusterState(PrettyPrinter form) {
    return new String(dsoServer.getClusterState(Charset.defaultCharset(), form), Charset.defaultCharset());
  }

  @Override
  public void pause(String path) {
    this.getStageManager().getStage(path, Object.class).pause();
  }

  @Override
  public void unpause(String path) {
    this.getStageManager().getStage(path, Object.class).unpause();
  }

  @Override
  public Map<String, ?> getStateMap() {
    return this.getStageManager().getStateMap();
  }  
}
