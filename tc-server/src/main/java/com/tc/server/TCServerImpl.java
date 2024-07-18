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
import java.lang.management.ManagementFactory;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
    if (getStateManager().moveToStopStateIf(ServerMode.PASSIVE_STATES)) {
      stop(restartMode);
    } else {
      throw new UnexpectedStateException("Server is not in passive state, current state: " + getStateManager().getCurrentMode());
    }
  }

  @Override
  public void stopIfActive(StopAction...restartMode) throws PlatformStopException {
    if (getStateManager().moveToStopStateIf(EnumSet.of(ServerMode.ACTIVE))) {
      stop(restartMode);
    } else {
      throw new UnexpectedStateException("Server is not in active state, current state: " + getStateManager().getCurrentMode());
    }
  }

  @Override
  public void stop(StopAction...restartMode) {
    audit("Stop invoked", new Properties());
    TCLogging.getConsoleLogger().info("Stopping server");
    if (dsoServer != null) {
      try {
        getStateManager().moveToStopStateIf(EnumSet.complementOf(EnumSet.of(ServerMode.STOP)));
        EnumSet<StopAction> set = EnumSet.noneOf(StopAction.class);
        for (StopAction s : restartMode) {
          set.add(s);
        }
        if (set.contains(StopAction.ZAP)) {
          TCLogging.getConsoleLogger().info("Setting data to dirty");
          dsoServer.getPersistor().getClusterStatePersistor().setDBClean(false);
        }
        CompletableFuture<Void> dsoStop = dsoServer.destroy(set.contains(StopAction.IMMEDIATE));
        if (set.contains(StopAction.RESTART)) {
          TCLogging.getConsoleLogger().info("Requesting restart");
          dsoStop.thenRun(()->shutdownGate.complete(true));
        } else {
          dsoStop.thenRun(()->shutdownGate.complete(false));
        }
      } catch (Throwable e) {
        logger.error("trouble shutting down", e);
        shutdownGate.completeExceptionally(e);
      }
    } else {
      shutdownGate.complete(false);
    }
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
      consoleLogger.info("Server exiting...");
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
    this.dsoServer = createDistributedObjectServer(this.configurationSetupManager, this.connectionPolicy, this);
    MBeanServer mbean = subsystem.getServer();
    registerDSOServer();
    registerServerMBeans();
    this.dsoServer.openNetworkPorts();
    this.dsoStarted = true;
  }

  protected DistributedObjectServer createDistributedObjectServer(ServerConfigurationManager configSetupManager,
                                                                  ConnectionPolicy policy,
                                                                  TCServerImpl serverImpl) throws Exception {
    DistributedObjectServer dso = new DistributedObjectServer(configSetupManager, getThreadGroup(), policy, this, this);
    dso.bootstrap();
    return dso;
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
