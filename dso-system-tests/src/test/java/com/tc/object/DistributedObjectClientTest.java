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
package com.tc.object;

import com.tc.abortable.NullAbortableOperationManager;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterImpl;
import com.tc.cluster.DsoClusterListener;
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
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.platform.rejoin.RejoinManagerImpl;
import com.tc.platform.rejoin.RejoinManagerInternal;
import com.tc.properties.TCPropertiesConsts;
import com.tc.server.TCServer;
import com.tc.server.TCServerImpl;
import com.tc.util.PortChooser;
import com.tcclient.cluster.DsoClusterInternal;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DistributedObjectClientTest extends BaseDSOTestCase {

  private PreparedComponentsFromL2Connection preparedComponentsFromL2Connection;
  private boolean                            originalReconnect;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    originalReconnect = tcProps.getBoolean(TCPropertiesConsts.L2_L1RECONNECT_ENABLED);
    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
  }

  protected final TCThreadGroup group = new TCThreadGroup(
          new MockThrowableHandlerImpl(TCLogging
                  .getLogger(DistributedObjectServer.class)));

  protected class StartAction implements StartupHelper.StartupAction {
    private final int tsaPort;
    private final int jmxPort;
    private TCServer  server = null;

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
      manager.dsoL2Config().tsaPort().setBind("127.0.0.1");

      manager.commonl2Config().jmxPort().setIntValue(jmxPort);
      manager.commonl2Config().jmxPort().setBind("127.0.0.1");
      server = new TCServerImpl(manager);
      server.start();
    }
  }

  protected TCServer startupServer(final int tsaPort, final int jmxPort) {
    StartAction start_action = new StartAction(tsaPort, jmxPort);
    new StartupHelper(group, start_action).startUp();
    final TCServer server = start_action.getServer();
    return server;
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
      for(CallbackOnExitHandler h : handlers) {
        h.callbackOnExit(new CallbackOnExitState(t));
      }
    }

    @Override
    public void addCallbackOnExitDefaultHandler(CallbackOnExitHandler callbackOnExitHandler) {
      this.handlers.add(callbackOnExitHandler);
    }
  }

  public void testNodeErrorEventWhenUncaughtExceptionTriggered() throws Exception {
    final PortChooser pc = new PortChooser();
    final int tsaPort = pc.chooseRandomPort();
    final int jmxPort = pc.chooseRandomPort();

    final TCServerImpl server = (TCServerImpl) startupServer(tsaPort, jmxPort);
    server.getDSOServer().getCommunicationsManager().addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);

    configFactory().addServerToL1Config("127.0.0.1", tsaPort, jmxPort);
    L1ConfigurationSetupManager manager = super.createL1ConfigManager();
    preparedComponentsFromL2Connection = new PreparedComponentsFromL2Connection(manager);

    RejoinManagerInternal rejoinManagerInternal = new RejoinManagerImpl(false);
    TCThreadGroup threadGroup = new TCThreadGroup(new MockThrowableHandlerImpl(TCLogging.getLogger(DistributedObjectClient.class)));
    DsoClusterInternal dsoCluster = new DsoClusterImpl(rejoinManagerInternal);
    DistributedObjectClient client = new DistributedObjectClient(new StandardDSOClientConfigHelperImpl(manager),
            threadGroup,
            new MockClassProvider(),
            preparedComponentsFromL2Connection,
            dsoCluster,
            new NullAbortableOperationManager(),
            rejoinManagerInternal);
    client.start();
    dsoCluster.init(client.getClusterMetaDataManager(), client.getObjectManager(), client.getClusterEventsStage());
    DsoClusterListener mockDsoClusterListener = mock(DsoClusterListener.class);
    dsoCluster.addClusterListener(mockDsoClusterListener);

    Thread workerThread = new Thread(threadGroup, new Runnable() {
      @Override
      public void run() {
        throw new TCRuntimeException("uncaught exception");
      }
    });
    workerThread.start();
    Thread.sleep(1000);
    verify(mockDsoClusterListener).nodeError(any(DsoClusterEvent.class));
  }

  @Override
  protected synchronized void tearDown() throws Exception {
    super.tearDown();
    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "" + originalReconnect);
    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "" + originalReconnect);
  }
}
