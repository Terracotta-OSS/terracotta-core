package com.tc.memorydatastore.server;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.async.impl.StageManagerImpl;
import com.tc.exception.TCRuntimeException;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.memorydatastore.message.MemoryDataStoreRequestMessage;
import com.tc.memorydatastore.message.MemoryDataStoreResponseMessage;
import com.tc.memorydatastore.server.handler.MemoryDataStoreRequestHandler;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.session.NullSessionManager;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.util.Collections;

public class MemoryDataStoreServer {
  public static final int       DEFAULT_PORT                    = 9001;

  private static final String   MEMORY_DATA_STORE_REQUEST_STAGE = "memory_data_store_request_stage";
  private final static int      STARTED                         = 1;
  private final static int      STOPPED                         = 2;

  private int                   serverPort;
  private int                   state;
  private NetworkListener       lsnr;
  private CommunicationsManager communicationManager;

  public static MemoryDataStoreServer createInstance() {
    return new MemoryDataStoreServer(DEFAULT_PORT);
  }

  public static MemoryDataStoreServer createInstance(int port) {
    return new MemoryDataStoreServer(port);
  }

  private MemoryDataStoreServer(int serverPort) {
    super();
    this.serverPort = serverPort;
  }

  public int getListenPort() {
    return lsnr.getBindPort();
  }

  private StageManager getStageManager() {
    ThrowableHandler throwableHandler = new ThrowableHandler(TCLogging.getLogger(MemoryDataStoreServer.class));
    TCThreadGroup threadGroup = new TCThreadGroup(throwableHandler);
    return new StageManagerImpl(threadGroup);
  }

  private void setupListener(int serverPort) {
    this.communicationManager = new CommunicationsManagerImpl(new NullMessageMonitor(),
        new PlainNetworkStackHarnessFactory(), new NullConnectionPolicy());
    this.lsnr = communicationManager.createListener(new NullSessionManager(), new TCSocketAddress(
        TCSocketAddress.WILDCARD_ADDR, serverPort), true, new DefaultConnectionIdFactory());
  }

  public void start() throws IOException {
    StageManager stageManager = getStageManager();
    setupListener(serverPort);

    lsnr.addClassMapping(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE, MemoryDataStoreRequestMessage.class);
    lsnr.addClassMapping(TCMessageType.MEMORY_DATA_STORE_RESPONSE_MESSAGE, MemoryDataStoreResponseMessage.class);

    Stage hydrateStage = stageManager.createStage("hydrate_message_stage", new HydrateHandler(), 1, 500); // temporary
    // hardcoded

    MemoryDataStoreRequestHandler memoryDataStoreRequestHandler = new MemoryDataStoreRequestHandler();
    Stage memoryDataStoreRequestStage = stageManager.createStage(MEMORY_DATA_STORE_REQUEST_STAGE,
        memoryDataStoreRequestHandler, 1, 1);
    lsnr.routeMessageType(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE, memoryDataStoreRequestStage.getSink(),
        hydrateStage.getSink());

    stageManager.startAll(new NullContext(stageManager)); // temporary hack to
    // start the stage
    lsnr.start(Collections.EMPTY_SET);
    this.state = STARTED;
  }

  public void shutdown() throws TCTimeoutException {
    this.lsnr.stop(5000);
    this.communicationManager.shutdown();
    this.state = STOPPED;
  }

  public int getState() {
    return this.state;
  }

  // Temporary hack
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

  public static void main(String[] args) {
    MemoryDataStoreServer server = createInstance(0);
    try {
      server.start();

      while (server.getState() == STARTED) {
        Thread.sleep(Long.MAX_VALUE);
      }
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    } catch (IOException e) {
      throw new TCRuntimeException(e);
    }
  }
}
