/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.server;


import com.tc.stats.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.monitoring.PlatformStopException;

import com.tc.async.api.SEDA;
import com.tc.async.api.Stage;
import com.tc.config.ServerConfigurationManager;
import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateManager;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2Dumper;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfo;
import com.tc.net.ServerID;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.ConnectionPolicyImpl;
import com.tc.net.utils.L2Utils;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.GuardianContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.impl.JMXSubsystem;
import com.tc.productinfo.ProductInfo;
import com.tc.spi.Guardian;
import com.tc.stats.DSO;
import com.tc.util.Assert;
import com.tc.util.State;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import com.tc.text.PrettyPrinter;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.ThreadUtil;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServerFactory;

import org.terracotta.server.StopAction;


public class TCServerImpl extends SEDA implements TCServer {
  private static final Logger logger = LoggerFactory.getLogger(TCServer.class);
  private static final Logger consoleLogger = TCLogging.getConsoleLogger();

  private volatile long                     startTime                                    = -1;
  private volatile long                     activateTime                                 = -1;

  private DistributedObjectServer         dsoServer;
  private boolean                         dsoStarted;

  private final ServerConfigurationManager configurationSetupManager;
  protected final ConnectionPolicy          connectionPolicy;
  private final CompletableFuture<Boolean>                      shutdownGate                        = new CompletableFuture<>();

  private final JMXSubsystem                subsystem;
  private DSO dso;

  private final SetOnceFlag stopping = new SetOnceFlag();

  public TCServerImpl(ServerConfigurationManager configurationSetupManager, TCThreadGroup threadGroup) {
    this(null, configurationSetupManager, threadGroup,
            new JMXSubsystem(!threadGroup.isStoppable() ? ManagementFactory.getPlatformMBeanServer() : MBeanServerFactory.newMBeanServer()),
            new ConnectionPolicyImpl(Integer.MAX_VALUE));
  }

  TCServerImpl(DistributedObjectServer dso, ServerConfigurationManager manager, TCThreadGroup group, JMXSubsystem subsystem,
                      ConnectionPolicy connectionPolicy) {
    super(group);
    this.dsoServer = dso;
    if (dso != null) {
      dsoStarted = true;
    }
    this.subsystem = subsystem;
    this.connectionPolicy = connectionPolicy;
    Assert.assertNotNull(manager);
    this.configurationSetupManager = manager;
    GuardianContext.setServer(this);
  }

  @Override
  public JMXSubsystem getJMX() {
    return subsystem;
  }

  @Override
  public ProductInfo productInfo() {
    return configurationSetupManager.getProductInfo();
  }

  @Override
  public String getL2Identifier() {
    return configurationSetupManager.getServerConfiguration().getName();
  }

  @Override
  public void stopIfPassive(StopAction...restartMode) throws PlatformStopException {
    if (stopIf(ServerMode.PASSIVE_STATES)) {
      stopping(restartMode);
    } else {
      throw new UnexpectedStateException("Server is not in passive state, current state: " + getStateManager().getCurrentMode());
    }
  }

  @Override
  public void stopIfActive(StopAction...restartMode) throws PlatformStopException {
    if (stopIf(EnumSet.of(ServerMode.ACTIVE))) {
      stopping(restartMode);
    } else {
      throw new UnexpectedStateException("Server is not in active state, current state: " + getStateManager().getCurrentMode());
    }
  }

  @Override
  public void stop(StopAction...restartMode) {
    if (stopIf(EnumSet.complementOf(EnumSet.of(ServerMode.STOP)))) {
      stopping(restartMode);
    }
  }

  private boolean stopIf(Set<ServerMode> validStates) {
    if (dsoStarted && dsoServer != null) {
      return getStateManager().moveToStopStateIf(validStates);
    } else {
      return true;
    }
  }

  private void stopping(StopAction...restartMode) {
    audit("Stop invoked", new Properties());
    EnumSet<StopAction> set = EnumSet.noneOf(StopAction.class);
    set.addAll(Arrays.asList(restartMode));
    boolean restart = (set.contains(StopAction.RESTART));

    if (dsoServer == null) {
      shutdownGate.complete(restart);
    } else if (stopping.attemptSet()) {
      try {
        consoleLogger.info("Stopping server");

        if (set.contains(StopAction.ZAP)) {
          TCLogging.getConsoleLogger().info("Setting data to dirty");
          dsoServer.getPersistor().getClusterStatePersistor().setDBClean(false);
        }
        // if this server is stoppable (inline server), it needs to wait for
        // full completion before completing
        boolean immediate = set.contains(StopAction.IMMEDIATE) && !getThreadGroup().isStoppable();
        consoleLogger.info("Server Exiting...");
        if (restart) {
          consoleLogger.info("Requesting restart");
        }
        //  if not immediate, cleanup all the threadgroup threads before signalling shutdown complete
        if (!immediate) {
          CompletableFuture<Void> retire = retireThreadGroup();
          retire.handle((c, e)->shutdownGate.complete(restart));
        } else {
          shutdownGate.complete(restart);
        }
      } catch (Throwable e) {
        logger.error("trouble shutting down", e);
        shutdownGate.completeExceptionally(e);
      }
    }
  }

  private CompletableFuture<Void> retireThreadGroup() {
    CompletableFuture<Void> retiring = new CompletableFuture<>();
    TCThreadGroup threadGroup = getThreadGroup();
    ThreadUtil.executeInThread(threadGroup.getParent(), ()->{
      try {
        dsoServer.waitForShutdown();
      } catch (Exception e) {
        logger.warn("error waiting for shutdown", e);
      }
      getStageManager().stopAll();
      if (!threadGroup.retire(TimeUnit.SECONDS.toMillis(30L), e->L2Utils.handleInterrupted(logger, e))) {
        logger.warn("unable to shutdown server threads");
        threadGroup.printLiveThreads(logger::warn);
        threadGroup.interrupt();
      }
      retiring.complete(null);
    }, "server shutdown thread", true);
    return retiring;
  }

  @Override
  public void start() {
    if (this.dsoServer == null) {
      try {
        startServer().get();
      } catch (Throwable t) {
        if (t instanceof RuntimeException) { throw (RuntimeException) t; }
        throw new RuntimeException(t);
      }
    } else {
      logger.warn("Server in incorrect state (" + getStateManager().getCurrentMode().getName() + ") to be started.");
    }
  }

  @Override
  public boolean canShutdown() {
    ServerMode serverState = getStateManager().getCurrentMode();
    return serverState == ServerMode.PASSIVE ||
       serverState == ServerMode.ACTIVE ||
       serverState == ServerMode.UNINITIALIZED ||
       serverState == ServerMode.SYNCING;
  }

  @Override
  public synchronized void shutdown() {
    if (canShutdown()) {
      stop();
    } else {
      logger.warn("Server in incorrect state (" + getStateManager().getCurrentMode().getName() + ") to be shutdown.");
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

  public void audit(String msg, Properties additional) {
    GuardianContext.validate(Guardian.Op.AUDIT_OP, msg, additional);
  }

  public void security(String msg, Properties additional) {
    GuardianContext.validate(Guardian.Op.SECURITY_OP, msg, additional);
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

  private StateManager getStateManager() {
    return this.dsoServer.getContext().getL2Coordinator().getStateManager();
  }

  @Override
  public synchronized boolean isStarted() {
    return !this.dsoStarted || getStateManager().getCurrentMode().isStartup();
  }

  @Override
  public boolean isActive() {
    return this.dsoStarted && getStateManager().isActiveCoordinator();
  }

  @Override
  public synchronized boolean isStopped() {
    return this.dsoStarted && getStateManager().getCurrentMode() == ServerMode.STOP;
  }

  @Override
  public boolean isPassiveUnitialized() {
    return this.dsoStarted && getStateManager().getCurrentMode() == ServerMode.UNINITIALIZED;
  }

  @Override
  public boolean isPassiveStandby() {
    return this.dsoStarted && getStateManager().getCurrentMode() == ServerMode.PASSIVE;
  }

  @Override
  public boolean isReconnectWindow() {
    return this.dsoStarted && dsoServer.getContext().getClientHandshakeManager().isStarting();
  }

  @Override
  public boolean isAcceptingClients() {
    return this.dsoStarted && dsoServer.isL1Listening() && dsoServer.getContext().getClientHandshakeManager().isStarted();
  }

  @Override
  public int getReconnectWindowTimeout() {
    return configurationSetupManager.getServerConfiguration().getClientReconnectWindow();
  }

  @Override
  public State getState() {
    return getStateManager().getCurrentMode().getState();
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


  private class StartAction implements Runnable {
    private final CompletableFuture<Void> finish;

    public StartAction(CompletableFuture<Void> finish) {
      this.finish = finish;
    }

    public void run() {
      if (logger.isDebugEnabled()) {
        logger.debug("Starting Terracotta server instance...");
      }

      TCServerImpl.this.startTime = System.currentTimeMillis();

      if (Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
        consoleLogger.info("Available Max Runtime Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB");
      }

      // the following code starts the jmx server as well
      try {
        startDSOServer();
      } catch (Throwable e) {
        finish.completeExceptionally(e);
      }

      String serverName = TCServerImpl.this.configurationSetupManager.getServerConfiguration().getName();
      if (serverName != null) {
        logger.info("Server started as " + serverName);
      }

      finish.complete(null);
    }
  }

  protected void warnOfStall(String name, long delay, int queueDepth) {

  }

  protected Future<Void> startServer() throws Exception {
    CompletableFuture<Void> complete = new CompletableFuture<>();
    new Thread(getThreadGroup(), new StartAction(complete), "Server Startup Thread").start();
    return complete;
  }

  private void startDSOServer() throws Exception {
    Assert.assertTrue(this.dsoServer == null);
    this.dsoServer = createDistributedObjectServer(this.configurationSetupManager, this.connectionPolicy);
    registerDSOServer();
    registerServerMBeans();
    this.dsoServer.openNetworkPorts();
    this.dsoStarted = true;
  }

  protected DistributedObjectServer createDistributedObjectServer(ServerConfigurationManager configSetupManager,
                                                                  ConnectionPolicy policy) throws Exception {
    DistributedObjectServer newDSOServer = new DistributedObjectServer(configSetupManager, getThreadGroup(), policy, this, this);
    newDSOServer.bootstrap();
    return newDSOServer;
  }

  @Override
  public void dump() {
    audit("Dump invoked", new Properties());
    TCLogging.getDumpLogger().info(new String(this.dsoServer.getClusterState(Charset.defaultCharset(), null), Charset.defaultCharset()));
  }

  private synchronized void registerDSOServer() throws InstanceAlreadyExistsException, MBeanRegistrationException,
      NotCompliantMBeanException {
    ServerManagementContext mgmtContext = this.dsoServer.getManagementContext();
    ServerConfigurationContext configContext = this.dsoServer.getContext();
    registerDSOMBeans(mgmtContext, configContext, this.subsystem.getServer());
  }

  private void registerServerMBeans()
      throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
    MBeanServer mBeanServer = subsystem.getServer();
    mBeanServer.registerMBean(new TCServerInfo(this), L2MBeanNames.TC_SERVER_INFO);
    mBeanServer.registerMBean(new L2Dumper(this, mBeanServer), L2MBeanNames.DUMPER);
  }

  private void registerDSOMBeans(ServerManagementContext mgmtContext, ServerConfigurationContext configContext,
                                   MBeanServer mBeanServer) throws NotCompliantMBeanException,
      InstanceAlreadyExistsException, MBeanRegistrationException {
      dso = new DSO(mgmtContext, configContext, mBeanServer);
      mBeanServer.registerMBean(dso, L2MBeanNames.DSO);
  }

  @Override
  public boolean waitUntilShutdown() {
    try {
      return shutdownGate.get();
    } catch (ExecutionException ee) {
      Throwable cause = ee.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException)cause;
      } else {
        throw new RuntimeException(cause);
      }
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    }
  }

  @Override
  public String[] processArguments() {
    return configurationSetupManager.getProcessArguments();
  }

  @Override
  public String getClusterState(PrettyPrinter form) {
    return new String(dsoServer.getClusterState(Charset.defaultCharset(), form), Charset.defaultCharset());
  }

  @Override
  public List<Client> getConnectedClients()
  {
    return dso.getConnectedClients();
  }

  @Override
  public void disconnectPeer(String nodeName) {
    dsoServer.getGroupManager().closeMember(nodeName);
  }

  @Override
  public void leaveGroup() {
    dsoServer.getGroupManager().disconnect();
    dsoServer.getContext().getL2Coordinator().getStateManager().startElectionIfNecessary(ServerID.NULL_ID);
  }

  @Override
  public void pause(String path) {
    if (path.equalsIgnoreCase("L1")) {
      try {
        dsoServer.getCommunicationsManager().getConnectionManager().getTcComm().pause();
      } catch (NullPointerException npe) {

      }
    } else if (path.equalsIgnoreCase("L2")) {
      try {
        dsoServer.getGroupManager().getConnectionManager().getTcComm().pause();
      } catch (NullPointerException npe) {

      }
    } else {
      Stage s = this.getStageManager().getStage(path, Object.class);
      if (s != null) {
        s.pause();
      }
    }
  }

  @Override
  public void unpause(String path) {
    if (path.equalsIgnoreCase("L1")) {
      try {
        dsoServer.getCommunicationsManager().getConnectionManager().getTcComm().unpause();
      } catch (NullPointerException npe) {

      }
    } else if (path.equalsIgnoreCase("L2")) {
      try {
        dsoServer.getGroupManager().getConnectionManager().getTcComm().unpause();
      } catch (NullPointerException npe) {

      }
    } else {
      Stage s = this.getStageManager().getStage(path, Object.class);
      if (s != null) {
        s.unpause();
      }
    }
  }

  @Override
  public Map<String, ?> getStateMap() {
    return this.getStageManager().getStateMap();
  }
}
