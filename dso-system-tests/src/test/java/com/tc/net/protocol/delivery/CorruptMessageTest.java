/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.delivery;

/**
 * Created by alsu on 12/5/14.
 */
import com.tc.abortable.NullAbortableOperationManager;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.cluster.DsoClusterImpl;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.exception.TCRuntimeException;
import com.tc.lang.StartupHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2State;
import com.tc.management.beans.TCServerInfo;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.net.core.TCConnection;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.DisabledHealthCheckerConfigImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.platform.rejoin.RejoinManagerImpl;
import com.tc.platform.rejoin.RejoinManagerInternal;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCPropertiesConsts;
import com.tc.server.TCServer;
import com.tc.server.TCServerImpl;
import com.tc.util.PortChooser;
import com.tcclient.cluster.DsoClusterInternal;
import org.junit.Test;

import org.mockito.Mockito;
import org.terracotta.test.util.WaitUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;

public class CorruptMessageTest extends BaseDSOTestCase {

  public static final String LOCALHOST = "localhost";

  private PreparedComponentsFromL2Connection preparedComponentsFromL2Connection;
  private boolean                            originalReconnect;

  protected final TCThreadGroup group = new TCThreadGroup(
      new MockThrowableHandlerImpl(TCLogging
          .getLogger(DistributedObjectServer.class)));

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    originalReconnect = tcProps.getBoolean(TCPropertiesConsts.L2_L1RECONNECT_ENABLED);
    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
    tcProps.setProperty(TCPropertiesConsts.L1_SERVER_EVENT_DELIVERY_TIMEOUT_INTERVAL, "1");
    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
  }

  @Test
  public void testServerBehaviourOnCorruptMessage() throws Exception {
    final PortChooser pc = new PortChooser();
    final int tsaPort = pc.chooseRandomPort();
    final int jmxPort = pc.chooseRandomPort();

    final TCServerImpl server = (TCServerImpl) startupServer(tsaPort, jmxPort);

    configFactory().addServerToL1Config(LOCALHOST, tsaPort, jmxPort);
    L1ConfigurationSetupManager manager = super.createL1ConfigManager();
    PreparedComponentsFromL2Connection preparedComponentsFromL2Connection =
        new PreparedComponentsFromL2Connection(manager);

    RejoinManagerInternal rejoinManagerInternal = new RejoinManagerImpl(false);
    TCThreadGroup threadGroup =
        new TCThreadGroup(new MockThrowableHandlerImpl(TCLogging.getLogger(DistributedObjectClient.class)));
    DsoClusterInternal dsoCluster = new DsoClusterImpl(rejoinManagerInternal);
    final DistributedObjectClient client = new DistributedObjectClient(new StandardDSOClientConfigHelperImpl(manager),
        threadGroup,
        new MockClassProvider(),
        preparedComponentsFromL2Connection,
        dsoCluster,
        new NullAbortableOperationManager(),
        rejoinManagerInternal);
    Thread clientThread = new Thread() {
      @Override
      public void run() {
        client.start();
      }
    };
    clientThread.start();

    // Waiting till a connection is created between the client and server
    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return server.getDSOServer().getCommunicationsManager().getConnectionManager().getAllConnections().length > 0;
      }
    });

    // Waiting till the buggy client connection is closed by the server
    final TCConnection conn =
        server.getDSOServer().getCommunicationsManager().getConnectionManager().getAllConnections()[0];
    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return conn.isClosed();
      }
    });
  }

  protected TCServer startupServer(final int tsaPort, final int jmxPort) {
    StartAction start_action = new StartAction(tsaPort, jmxPort);
    new StartupHelper(group, start_action).startUp();
    final TCServer server = start_action.getServer();
    return server;
  }

  @Override
  protected synchronized void tearDown() throws Exception {
    super.tearDown();
    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "" + originalReconnect);
    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "" + originalReconnect);
  }

  private static class MyDistributedObjectServer extends DistributedObjectServer {

    public MyDistributedObjectServer(L2ConfigurationSetupManager configSetupManager, TCThreadGroup threadGroup,
                                     ConnectionPolicy connectionPolicy, Sink httpSink,
                                     TCServerInfoMBean tcServerInfoMBean, ObjectStatsRecorder objectStatsRecorder,
                                     L2State l2State, SEDA seda, TCServer server, TCSecurityManager securityManager) {
      super(configSetupManager, threadGroup, connectionPolicy, httpSink,
          tcServerInfoMBean, objectStatsRecorder, l2State, seda, server, securityManager);
    }

    @Override
    protected CommunicationsManagerImpl createCommunicationsManager(TCMessageRouter messageRouter) {

      OnceAndOnlyOnceProtocolNetworkLayerFactory oooFactory = new MyOnceAndOnlyOnceProtocolNetworkLayerFactoryImpl();
      NetworkStackHarnessFactory harnessFactory = new OOONetworkStackHarnessFactory(
          oooFactory,
          new L1ReconnectConfigImpl(true, 15000, 5000, 16, 32));

      return new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(), messageRouter,
          harnessFactory, new NullConnectionPolicy(),
          new DisabledHealthCheckerConfigImpl(),
          Collections.EMPTY_MAP,
          Collections.EMPTY_MAP);
    }
  }


  private static class MyTCServerImpl extends TCServerImpl{

    public MyTCServerImpl(L2ConfigurationSetupManager configurationSetupManager) {
      super(configurationSetupManager);
    }

    @Override
    protected DistributedObjectServer createDistributedObjectServer(L2ConfigurationSetupManager configSetupManager,
                                                                    ConnectionPolicy policy, Sink httpSink,
                                                                    TCServerInfo serverInfo,
                                                                    ObjectStatsRecorder objectStatsRecorder,
                                                                    L2State l2State, TCServerImpl serverImpl) {
      return new MyDistributedObjectServer(configSetupManager, getThreadGroup(), policy, httpSink, serverInfo,
          objectStatsRecorder, l2State, this, this, securityManager);
    }
  }

  private static class MyOnceAndOnlyOnceProtocolNetworkLayerFactoryImpl
      extends OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl {

    @Override
    public synchronized OnceAndOnlyOnceProtocolNetworkLayer createNewServerInstance(ReconnectConfig reconnectConfig) {
      OOOProtocolMessageFactory messageFactory = Mockito.mock(OOOProtocolMessageFactory.class);
      OOOProtocolMessageParser messageParser = Mockito.mock(OOOProtocolMessageParser.class);

      return new MyOnceAndOnlyOnceProtocolNetworkLayerImpl(messageFactory, messageParser, reconnectConfig, false);
    }

  }
  private static class MyOnceAndOnlyOnceProtocolNetworkLayerImpl extends OnceAndOnlyOnceProtocolNetworkLayerImpl {

    public MyOnceAndOnlyOnceProtocolNetworkLayerImpl(OOOProtocolMessageFactory messageFactory,
                                                     OOOProtocolMessageParser messageParser,
                                                     ReconnectConfig reconnectConfig, boolean isClient) {
      super(messageFactory, messageParser, reconnectConfig, isClient);
    }

    @Override
    public void receive(TCByteBuffer[] msgData) {
      throw new TCRuntimeException("OOO message parsing exception");
    }

  }

  private static class MockThrowableHandlerImpl extends ThrowableHandlerImpl {

    private ArrayList<CallbackOnExitHandler> handlers = new ArrayList<CallbackOnExitHandler>();

    /**
     * Construct a new ThrowableHandler with a logger
     *
     * @param logger Logger
     */
    public MockThrowableHandlerImpl(TCLogger logger) {
      super(logger);
    }

    @Override
    public void handleThrowable(final Thread thread, final Throwable t) {
      //do nothing
      for (CallbackOnExitHandler h : handlers) {
        h.callbackOnExit(new CallbackOnExitState(t));
      }
    }

    @Override
    public void addCallbackOnExitDefaultHandler(CallbackOnExitHandler callbackOnExitHandler) {
      this.handlers.add(callbackOnExitHandler);
    }
  }

  protected class StartAction implements StartupHelper.StartupAction {
    private final int tsaPort;
    private final int jmxPort;
    private TCServer server = null;

    private StartAction(final int tsaPort, final int jmxPort) {
      this.tsaPort = tsaPort;
      this.jmxPort = jmxPort;
    }

    public int getTsaPort() {
      return tsaPort;
    }

    public int getJmxPort() {
      return jmxPort;
    }

    public TCServer getServer() {
      return server;
    }

    @Override
    public void execute() throws Throwable {
      ManagedObjectStateFactory.disableSingleton(true);
      TestConfigurationSetupManagerFactory factory = configFactory();
      L2ConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null, true);

      manager.dsoL2Config().tsaPort().setIntValue(tsaPort);
      manager.dsoL2Config().tsaPort().setBind(LOCALHOST);

      manager.dsoL2Config().getDataStorage().setSize("512m");
      manager.dsoL2Config().getOffheap().setSize("512m");

      manager.commonl2Config().jmxPort().setIntValue(jmxPort);
      manager.commonl2Config().jmxPort().setBind(LOCALHOST);
      server = new MyTCServerImpl(manager);
      server.start();
    }
  }

}
