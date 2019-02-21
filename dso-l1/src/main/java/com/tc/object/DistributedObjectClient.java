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
package com.tc.object;

import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.PostInit;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.entity.DiagnosticMessageImpl;
import com.tc.entity.DiagnosticResponseImpl;
import com.tc.entity.NetworkVoltronEntityMessageImpl;
import com.tc.entity.VoltronEntityAppliedResponseImpl;
import com.tc.entity.VoltronEntityMultiResponse;
import com.tc.entity.VoltronEntityReceivedResponseImpl;
import com.tc.entity.VoltronEntityResponse;
import com.tc.entity.VoltronEntityRetiredResponseImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.util.ProductID;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.ClientIDLoggerProvider;
import com.tc.management.TCClient;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.TCMessageHydrateSink;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.HealthCheckerConfigClientImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.ReconnectionRejectedHandlerL1;
import com.tc.net.protocol.transport.TransportHandshakeException;
import com.tc.object.config.ClientConfig;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.object.handler.ClientCoordinationHandler;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.handshakemanager.ClientHandshakeManagerImpl;
import com.tc.object.msg.ClientHandshakeAckMessageImpl;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.msg.ClientHandshakeRefusedMessageImpl;
import com.tc.object.msg.ClientHandshakeResponse;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.request.MultiRequestReceiveHandler;
import com.tc.object.request.RequestReceiveHandler;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionManagerImpl;
import com.tc.cluster.ClientChannelEventController;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.text.MapListPrettyPrint;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.CommonShutDownHook;
import com.tc.util.ProductInfo;
import com.tc.util.TCTimeoutException;
import com.tc.util.UUID;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.SetOnceRef;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;
import com.tc.entity.DiagnosticResponse;
import com.tc.entity.LinearVoltronEntityMultiResponse;
import com.tc.entity.ReplayVoltronEntityMultiResponse;
import com.tc.logging.CallbackOnExitState;
import com.tc.net.basic.BasicConnectionManager;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerImpl;
import com.tc.net.protocol.tcm.TCMessageHydrateAndConvertSink;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.util.runtime.ThreadDumpUtil;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


/**
 * This is the main point of entry into the DSO client.
 */
public class DistributedObjectClient implements TCClient {

  protected static final Logger DSO_LOGGER = LoggerFactory.getLogger(DistributedObjectClient.class);
  
  private final ClientBuilder                        clientBuilder;
  private final ClientConfig                         config;
  private final TCThreadGroup                        threadGroup;

  protected final PreparedComponentsFromL2Connection connectionComponents;

  private ClientMessageChannel                       channel;
  private TCConnectionManager                        connectionManager;
  private CommunicationsManager                      communicationsManager;
  private ClientHandshakeManager                     clientHandshakeManager;

  private CounterManager                             counterManager;

  private final String                                 uuid;
  private final String                               name;

  private ClientShutdownManager                      shutdownManager;

  private final SetOnceFlag                          clientStopped                       = new SetOnceFlag();
  private final SetOnceFlag                          connectionMade                      = new SetOnceFlag();
  private final SetOnceRef<Thread>                   connectionThread                    = new SetOnceRef<>();
  private final SetOnceRef<Exception>                exceptionMade                       = new SetOnceRef<>();
 
  private ClientEntityManager clientEntityManager;
  private RequestReceiveHandler singleMessageReceiver;
  private final StageManager communicationStageManager;
  
  private final boolean isAsync;

  
  public DistributedObjectClient(ClientConfig config, TCThreadGroup threadGroup,
                                 PreparedComponentsFromL2Connection connectionComponents,
                                 Properties properties) {
    this(config, ClientBuilderFactory.get().create(properties), threadGroup, connectionComponents,
         UUID.NULL_ID.toString(), "", false);
  }

  public DistributedObjectClient(ClientConfig config, ClientBuilder builder, TCThreadGroup threadGroup,
                                 PreparedComponentsFromL2Connection connectionComponents,
                                 String uuid, String name, boolean asyncDrive) {
    Assert.assertNotNull(config);
    this.config = config;
    this.connectionComponents = connectionComponents;
    this.threadGroup = threadGroup;
    this.clientBuilder = builder;
    this.uuid = uuid;
    this.name = name;
    this.isAsync = asyncDrive;
    
    // We need a StageManager to create the SEDA stages used for handling the messages.
    final SEDA seda = new SEDA(threadGroup);
    communicationStageManager = seda.getStageManager();
  }

  private ReconnectConfig getReconnectPropertiesFromServer() {
    ReconnectConfig reconnectConfig = new ReconnectConfig() {

      @Override
      public boolean getReconnectEnabled() {
        return false;
      }

      @Override
      public int getReconnectTimeout() {
        return 5000;
      }

      @Override
      public int getSendQueueCapacity() {
        return 5000;
      }

      @Override
      public int getMaxDelayAcks() {
        return 16;
      }

      @Override
      public int getSendWindow() {
        return 32;
      }
    };
    return reconnectConfig;
  }

  private NetworkStackHarnessFactory getNetworkStackHarnessFactory(boolean useOOOLayer,
                                                                   ReconnectConfig l1ReconnectConfig) {
    if (useOOOLayer) {
      return new OOONetworkStackHarnessFactory(new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(), l1ReconnectConfig);
    } else {
      return new PlainNetworkStackHarnessFactory();
    }
  }

  public synchronized void start() {
    final TCProperties tcProperties = TCPropertiesImpl.getProperties();
    final int maxSize = tcProperties.getInt(TCPropertiesConsts.L1_SEDA_STAGE_SINK_CAPACITY);

    final SessionManager sessionManager = new SessionManagerImpl(new SessionManagerImpl.SequenceFactory() {
      @Override
      public Sequence newSequence() {
        return new SimpleSequence();
      }
    });
//  weak reference to allow garbage collection if ref is dropped    
    Reference<DistributedObjectClient> ref = new WeakReference<>(this);
    this.threadGroup.addCallbackOnExitDefaultHandler((CallbackOnExitState state) -> {
      DistributedObjectClient client = ref.get();
      if (client != null) {
        DSO_LOGGER.info(client.getClientState());
      }
      Thread.dumpStack();
    });

    final ReconnectConfig l1ReconnectConfig = getReconnectPropertiesFromServer();

    final boolean useOOOLayer = l1ReconnectConfig.getReconnectEnabled();
    final NetworkStackHarnessFactory networkStackHarnessFactory = getNetworkStackHarnessFactory(useOOOLayer,
                                                                                                l1ReconnectConfig);

    this.counterManager = new CounterManagerImpl();
    final MessageMonitor mm = MessageMonitorImpl.createMonitor(tcProperties, DSO_LOGGER);
    final TCMessageRouter messageRouter = new TCMessageRouterImpl();
    final HealthCheckerConfig hc = new HealthCheckerConfigClientImpl(tcProperties
                                         .getPropertiesFor(TCPropertiesConsts.L1_L2_HEALTH_CHECK_CATEGORY), "TC Client");

    this.connectionManager = (isAsync) ?
            new TCConnectionManagerImpl(communicationsManager.COMMSMGR_CLIENT, 0, hc, this.clientBuilder.createBufferManagerFactory())
            :
            new BasicConnectionManager(this.clientBuilder.createBufferManagerFactory());
    this.communicationsManager = this.clientBuilder
        .createCommunicationsManager(mm,
                                     messageRouter,
                                     networkStackHarnessFactory,
                                     new NullConnectionPolicy(),
                                     connectionManager,
                                     hc,
                                     getMessageTypeClassMapping(),
                                     ReconnectionRejectedHandlerL1.SINGLETON);

    DSO_LOGGER.debug("Created CommunicationsManager.");

    final int socketConnectTimeout = tcProperties.getInt(TCPropertiesConsts.L1_SOCKET_CONNECT_TIMEOUT);

    if (socketConnectTimeout < 0) { throw new IllegalArgumentException("invalid socket time value: "
                                                                       + socketConnectTimeout); }
    ClientMessageChannel clientChannel = this.clientBuilder.createClientMessageChannel(this.communicationsManager,
                                                                 sessionManager, socketConnectTimeout, this);
    this.channel = clientChannel;
    // add this listener so that the whole system is shutdown
    // if the transport is closed from underneath.
    //  this typically happens when the transport is disconnected and 
    // reconnect is disabled
    clientChannel.addListener(new ChannelEventListener() {
      @Override
      public void notifyChannelEvent(ChannelEvent event) {
        switch(event.getType()) {
          case TRANSPORT_CLOSED_EVENT:
          case TRANSPORT_RECONNECTION_REJECTED_EVENT:
            shutdown();
        }
      }
    });

    final ClientIDLoggerProvider cidLoggerProvider = new ClientIDLoggerProvider(clientChannel::getClientID);
    this.communicationStageManager.setLoggerProvider(cidLoggerProvider);

    DSO_LOGGER.debug("Created channel.");

    this.clientEntityManager = this.clientBuilder.createClientEntityManager(clientChannel, this.communicationStageManager);
    this.singleMessageReceiver = new RequestReceiveHandler(this.clientEntityManager);
    MultiRequestReceiveHandler mutil = new MultiRequestReceiveHandler(this.clientEntityManager);
    Stage<VoltronEntityMultiResponse> multiResponseStage = this.communicationStageManager.createStage(ClientConfigurationContext.VOLTRON_ENTITY_MULTI_RESPONSE_STAGE, VoltronEntityMultiResponse.class, mutil, 1, maxSize);

    final ProductInfo pInfo = ProductInfo.getInstance();
    
    ClientHandshakeMessageFactory chmf = (u, n, c)->{
      ClientMessageChannel cmc = channel;
      if (cmc != null) {
        final ClientHandshakeMessage rv = (ClientHandshakeMessage)cmc.createMessage(TCMessageType.CLIENT_HANDSHAKE_MESSAGE);
        rv.setClientVersion(c);
        rv.setClientPID(getPID());
        rv.setUUID(u);
        rv.setName(n);
        return rv;
      } else {
        return null;
      }
    };
    
    this.clientHandshakeManager = this.clientBuilder
        .createClientHandshakeManager(new ClientIDLogger(clientChannel, LoggerFactory
                                          .getLogger(ClientHandshakeManagerImpl.class)), chmf, sessionManager,
                                          this.uuid, this.name, pInfo.version(), this.clientEntityManager);

    ClientChannelEventController.connectChannelEventListener(clientChannel, clientHandshakeManager);

    this.shutdownManager = new ClientShutdownManager(this);

    final ClientConfigurationContext cc = new ClientConfigurationContext(this.communicationStageManager);
    // DO NOT create any stages after this call
    
    String[] exclusion = clientChannel.getProductID() == ProductID.DIAGNOSTIC || !isAsync ? 
      new String[] {
        ClientConfigurationContext.VOLTRON_ENTITY_MULTI_RESPONSE_STAGE
      } 
              :
      new String[] {
      };

    this.communicationStageManager.startAll(cc, Collections.<PostInit> emptyList(), exclusion);

    EventHandler<ClientHandshakeResponse> handshake = new ClientCoordinationHandler(this.clientHandshakeManager);

    initChannelMessageRouter(messageRouter, EventHandler.directSink(handshake), isAsync ? multiResponseStage.getSink() : EventHandler.directSink(mutil));
    connectionThread.set(new Thread(threadGroup, ()->{
          while (!clientStopped.isSet()) {
            try {
              openChannel(clientChannel);
              waitForHandshake(clientChannel);
              connectionMade();
              break;
            } catch (RuntimeException runtime) {
              synchronized (connectionMade) {
                exceptionMade.set(runtime);
                connectionMade.notifyAll();
              }
              break;
            } catch (InterruptedException ie) {
              synchronized (connectionMade) {
                exceptionMade.set(ie);
                connectionMade.notifyAll();
              }              // We are in the process of letting the thread terminate so we don't handle this in a special way.
              break;
            }
          }
          //  don't reset interrupted, thread is done
        }, "Connection Maker - " + uuid));
      connectionThread.get().start();
  }

  private void connectionMade() {
    connectionMade.attemptSet();
    synchronized (connectionMade) {
      connectionMade.notifyAll();
    }
  }
  
  public boolean waitForConnection(long timeout, TimeUnit units) throws InterruptedException {
    if (!connectionThread.isSet()) {
      throw new IllegalStateException("not started");
    }
    connectionThread.get().join(units.toMillis(timeout));

    if (exceptionMade.isSet()) {
      Exception exp = exceptionMade.get();
      throw new RuntimeException(exp);
    }
    return connectionMade.isSet();
  }

  private void openChannel(ClientMessageChannel channel) throws InterruptedException {
    Collection<ConnectionInfo> infos = Arrays.asList(this.connectionComponents.createConnectionInfoConfigItem().getConnectionInfos());
    if (infos.isEmpty()) {
//  can't open a connection to nowhere
      return;
    }
    while (!clientStopped.isSet()) {
      try {
        DSO_LOGGER.debug("Trying to open channel....");
        channel.open(infos);
        DSO_LOGGER.debug("Channel open");
        break;
      } catch (final TCTimeoutException tcte) {
        DSO_LOGGER.info("Unable to connect to server/s {} ...sleeping for 5 sec.", infos);
        DSO_LOGGER.debug("Timeout connecting to server/s: {} {}", infos, tcte.getMessage());
        synchronized(clientStopped) {
          clientStopped.wait(5000);
        }
      } catch (final ConnectException e) {
        DSO_LOGGER.info("Unable to connect to server/s {} ...sleeping for 5 sec.", infos);
        DSO_LOGGER.debug("Connection refused from server/s: {} {}", infos, e.getMessage());
        synchronized(clientStopped) {
          clientStopped.wait(5000);
        }
      } catch (final MaxConnectionsExceededException e) {
        DSO_LOGGER.error(e.getMessage());
        throw new IllegalStateException(e.getMessage(), e);
      } catch (final CommStackMismatchException e) {
        DSO_LOGGER.error(e.getMessage());
        throw new IllegalStateException(e.getMessage(), e);
      } catch (TransportHandshakeException handshake) {
        DSO_LOGGER.error(handshake.getMessage());
        throw new IllegalStateException(handshake.getMessage(), handshake);
      } catch (final IOException ioe) {
        DSO_LOGGER.info("Unable to connect to server/s {} ...sleeping for 5 sec.", infos);
        DSO_LOGGER.debug("IOException connecting to server/s: {} {}", infos, ioe.getMessage());
        synchronized(clientStopped) {
          clientStopped.wait(5000);
        }
      }
    }
  }

  private void waitForHandshake(ClientMessageChannel channel) {
    this.clientHandshakeManager.waitForHandshake();
    if (channel != null) {
      final TCSocketAddress remoteAddress = channel.getRemoteAddress();
      final String infoMsg = "Connection successfully established to server at " + remoteAddress;
      if (!channel.getProductID().isInternal() && channel.isConnected()) {
        DSO_LOGGER.info(infoMsg);
      }
    }
  }

  private Map<TCMessageType, Class<? extends TCMessage>> getMessageTypeClassMapping() {
    final Map<TCMessageType, Class<? extends TCMessage>> messageTypeClassMapping = new HashMap<TCMessageType, Class<? extends TCMessage>>();

    messageTypeClassMapping.put(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, ClientHandshakeMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, ClientHandshakeAckMessageImpl.class);
    messageTypeClassMapping
        .put(TCMessageType.CLIENT_HANDSHAKE_REFUSED_MESSAGE, ClientHandshakeRefusedMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, ClusterMembershipMessage.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_MESSAGE, NetworkVoltronEntityMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE, VoltronEntityReceivedResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE, VoltronEntityAppliedResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_RETIRED_RESPONSE, VoltronEntityRetiredResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_MULTI_RESPONSE, LinearVoltronEntityMultiResponse.class);
    messageTypeClassMapping.put(TCMessageType.DIAGNOSTIC_REQUEST, DiagnosticMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.DIAGNOSTIC_RESPONSE, DiagnosticResponseImpl.class);
    return messageTypeClassMapping;
  }

  private void initChannelMessageRouter(TCMessageRouter messageRouter, Sink<ClientHandshakeResponse> ack, 
                                         Sink<VoltronEntityMultiResponse> multiSink) {
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, new TCMessageHydrateSink<>(ack));
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_REFUSED_MESSAGE, new TCMessageHydrateSink<>(ack));
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_REDIRECT_MESSAGE, new TCMessageHydrateSink<>(ack));
    messageRouter.routeMessageType(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, new TCMessageHydrateSink<>((context) -> {/* black hole for compatibility */}));
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE, new TCMessageHydrateAndConvertSink<>(multiSink, this::convertSingleToMulti));
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE, new TCMessageHydrateAndConvertSink<>(multiSink, this::convertSingleToMulti));
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_RETIRED_RESPONSE, new TCMessageHydrateAndConvertSink<>(multiSink, this::convertSingleToMulti));
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_MULTI_RESPONSE, new TCMessageHydrateSink<>(multiSink));
    messageRouter.routeMessageType(TCMessageType.DIAGNOSTIC_RESPONSE, new TCMessageHydrateAndConvertSink<>(multiSink, this::convertSingleToMulti));
    DSO_LOGGER.debug("Added message routing types.");
  }
    
  private VoltronEntityMultiResponse convertSingleToMulti(VoltronEntityResponse response) {
    if (response instanceof DiagnosticResponse) {
      // dont convert, just directly execute
      this.clientEntityManager.complete(response.getTransactionID(), ((DiagnosticResponse)response).getResponse());
      return null;
    } else {
      return new ReplayVoltronEntityMultiResponse() {
        @Override
        public int replay(VoltronEntityMultiResponse.ReplayReceiver receiver) {
          try {
            singleMessageReceiver.handleEvent(response);
            return 1;
          } catch (EventHandlerException ee) {
            throw new RuntimeException(ee);
          }
        }
      };
    }
  }

  public ClientEntityManager getEntityManager() {
    return this.clientEntityManager;
  }

  public CommunicationsManager getCommunicationsManager() {
    return this.communicationsManager;
  }

  public ClientHandshakeManager getClientHandshakeManager() {
    return this.clientHandshakeManager;
  }
  
  private String getClientState() {
    PrettyPrinter printer = new MapListPrettyPrint();
    this.communicationStageManager.prettyPrint(printer);
    this.clientEntityManager.prettyPrint(printer);
    return printer.toString();
  }

  public void dump() {
    DSO_LOGGER.info(getClientState());
  }

  protected ClientConfig getClientConfigHelper() {
    return this.config;
  }

  public void shutdown() {
    shutdown(false);
  }

  void shutdownResources() {
    final Logger logger = DSO_LOGGER;

    if (this.counterManager != null) {
      try {
        this.counterManager.shutdown();
      } catch (final Throwable t) {
        logger.error("error shutting down counter manager", t);
      } finally {
        this.counterManager = null;
      }
    }

    ClientMessageChannel clientChannel = this.channel;
    if (clientChannel != null) {
      try {
        clientChannel.close();
      } catch (final Throwable t) {
        logger.error("Error closing channel", t);
      } finally {

      }
    }

    if (this.communicationsManager != null) {
      try {
        this.communicationsManager.shutdown();
      } catch (final Throwable t) {
        logger.error("Error shutting down communications manager", t);
      } finally {
        this.communicationsManager = null;
      }
    }
    
    if (this.connectionManager != null) {
      try {
        this.connectionManager.shutdown();
      } catch (final Throwable t) {
        logger.error("Error shutting down connection manager", t);
      } finally {
        this.connectionManager = null;
      }
    }

    try {
      this.communicationStageManager.stopAll();
    } catch (final Throwable t) {
      logger.error("Error stopping stage manager", t);
    }
    
    CommonShutDownHook.shutdown();

    if (this.threadGroup != null) {
      boolean interrupted = false;

      try {
        final long end = System.currentTimeMillis()
                         + TCPropertiesImpl.getProperties()
                             .getLong(TCPropertiesConsts.L1_SHUTDOWN_THREADGROUP_GRACETIME);

        int threadCount = this.threadGroup.activeCount();
        Thread[] t = new Thread[threadCount];
        threadCount = this.threadGroup.enumerate(t);
        final long time = System.currentTimeMillis();
        boolean leaked = false;
        for (int x=0;x<threadCount;x++) {
          if (System.currentTimeMillis() > end) {
            break;
          }
          long start = System.currentTimeMillis();
          if (t[x].isAlive() && Thread.currentThread() != t[x]) {
            t[x].join(1000);
            if (t[x].isAlive()) {
              Exception printer = new Exception();
              printer.setStackTrace(printer.getStackTrace());
              DSO_LOGGER.warn("thread leak", printer);
              leaked = true;
            }
          }
          logger.debug("Destroyed thread " + t[x].getName() + " time to destroy:" + (System.currentTimeMillis() - start) + " millis");
        }
        logger.debug("time to destroy thread group:"  + TimeUnit.SECONDS.convert(System.currentTimeMillis() - time, TimeUnit.MILLISECONDS) + " seconds");

        if (leaked) {
          logger.warn("Timed out waiting for TC thread group threads to die - probable shutdown memory leak\n"
                      + "Live threads: " + getLiveThreads(this.threadGroup));

          Thread threadGroupCleanerThread = new Thread(this.threadGroup.getParent(),
                                                       new TCThreadGroupCleanerRunnable(threadGroup),
                                                       "TCThreadGroup last chance cleaner thread");
          logger.warn(ThreadDumpUtil.getThreadDump());
          threadGroupCleanerThread.setDaemon(true);
          threadGroupCleanerThread.start();
          logger.warn("Spawning TCThreadGroup last chance cleaner thread");
        } else {
          logger.debug("Destroying TC thread group");
          if (this.threadGroup != Thread.currentThread().getThreadGroup()) {
            this.threadGroup.destroy();
          }
        }
      } catch (final Throwable t) {
        logger.error("Error destroying TC thread group", t);
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }

    if (TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L1_SHUTDOWN_FORCE_FINALIZATION)) System
        .runFinalization();
  }

  private static List<Thread> getLiveThreads(ThreadGroup group) {
    final int estimate = group.activeCount();

    Thread[] threads = new Thread[estimate + 1];

    while (true) {
      final int count = group.enumerate(threads);

      if (count < threads.length) {
        final List<Thread> l = new ArrayList<>(count);
        for (final Thread t : threads) {
          if (t != null && t != Thread.currentThread()) {
            l.add(t);
          }
        }
        return l;
      } else {
        threads = new Thread[threads.length * 2];
      }
    }
  }

  @Override
  public String[] processArguments() {
    return null;
  }

  @Override
  public String getUUID() {
    return uuid;
  }

  private static class TCThreadGroupCleanerRunnable implements Runnable {
    private final TCThreadGroup threadGroup;

    public TCThreadGroupCleanerRunnable(TCThreadGroup threadGroup) {
      this.threadGroup = threadGroup;
    }

    @Override
    public void run() {
      for (Thread liveThread : getLiveThreads(threadGroup)) {
        Exception e = new Exception("thread is stuck " + liveThread.getName());
        e.setStackTrace(liveThread.getStackTrace());
        DSO_LOGGER.warn("stray connection threads not stopping", e);
        liveThread.interrupt();
      }
      try {
        threadGroup.destroy();
      } catch (Exception e) {
        // the logger is closed by now so we can't even log that
      }
    }
  }

  private void shutdownClient(boolean forceImmediate) {
    if (this.shutdownManager != null) {
      try {
        this.shutdownManager.execute(forceImmediate);
      } finally {
        
      }
    }
  }

  private void shutdown(boolean forceImmediate) {
    if (connectionThread.isSet()) {
      connectionThread.get().interrupt();
    }
    if (clientStopped.attemptSet()) {
      synchronized (clientStopped) {
        clientStopped.notifyAll();
      }
      ClientMessageChannel clientChannel = this.channel;
      if (clientChannel != null && !clientChannel.getProductID().isInternal() && clientChannel.isConnected()) {
        DSO_LOGGER.info("closing down Terracotta Connection force=" + forceImmediate + " channel=" + clientChannel.getChannelID() + " client=" + clientChannel.getClientID());
      }
      shutdownClient(forceImmediate);
    }
  }
  
  private int getPID() {
    String vmName = ManagementFactory.getRuntimeMXBean().getName();
    int index = vmName.indexOf('@');

    if (index < 0) { throw new RuntimeException("unexpected format: " + vmName); }

    return Integer.parseInt(vmName.substring(0, index));
  }
}
