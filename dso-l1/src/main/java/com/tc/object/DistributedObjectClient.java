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

import com.tc.async.api.PostInit;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.cluster.Cluster;
import com.tc.entity.DiagnosticMessageImpl;
import com.tc.entity.DiagnosticResponseImpl;
import com.tc.entity.NetworkVoltronEntityMessageImpl;
import com.tc.entity.ServerEntityMessageImpl;
import com.tc.entity.ServerEntityResponseMessageImpl;
import com.tc.entity.VoltronEntityAppliedResponseImpl;
import com.tc.entity.VoltronEntityMultiResponse;
import com.tc.entity.VoltronEntityMultiResponseImpl;
import com.tc.entity.VoltronEntityReceivedResponseImpl;
import com.tc.entity.VoltronEntityResponse;
import com.tc.entity.VoltronEntityRetiredResponseImpl;
import com.tc.exception.TCRuntimeException;
import com.tc.handler.CallbackDumpAdapter;
import com.tc.handler.CallbackDumpHandler;
import com.tc.lang.TCThreadGroup;
import com.tc.util.ProductID;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.ClientIDLoggerProvider;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.TCClient;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.HydrateContext;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.HealthCheckerConfigClientImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.ReconnectionRejectedHandlerL1;
import com.tc.object.config.ClientConfig;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.object.context.PauseContext;
import com.tc.object.handler.ClientCoordinationHandler;
import com.tc.object.handler.ClusterInternalEventsHandler;
import com.tc.object.handler.ClusterMembershipEventsHandler;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.handshakemanager.ClientHandshakeManagerImpl;
import com.tc.object.msg.ClientHandshakeAckMessageImpl;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.msg.ClientHandshakeRefusedMessageImpl;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.msg.InvokeRegisteredServiceMessage;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;
import com.tc.object.msg.ListRegisteredServicesMessage;
import com.tc.object.msg.ListRegisteredServicesResponseMessage;
import com.tc.object.request.MultiRequestReceiveHandler;
import com.tc.object.request.RequestReceiveHandler;
import com.tc.object.servermessage.ServerMessageReceiveHandler;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionManagerImpl;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.platform.rejoin.ClientChannelEventController;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.util.Assert;
import com.tc.util.CommonShutDownHook;
import com.tc.util.ProductInfo;
import com.tc.util.TCTimeoutException;
import com.tc.util.UUID;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.SetOnceRef;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;
import com.tcclient.cluster.ClusterInternal;
import com.tcclient.cluster.ClusterInternalEventsContext;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * This is the main point of entry into the DSO client.
 */
public class DistributedObjectClient implements TCClient {

  protected static final TCLogger                    DSO_LOGGER                          = CustomerLogging
                                                                                             .getDSOGenericLogger();
  private static final TCLogger                      CONSOLE_LOGGER                      = CustomerLogging
                                                                                             .getConsoleLogger();
  private static final int                           MAX_CONNECT_TRIES                   = -1;

  private static final String                        L1VMShutdownHookName                = "L1 VM Shutdown Hook";
  
  private final ClientBuilder                        clientBuilder;
  private final ClientConfig                         config;
  private final ClusterInternal                      cluster;
  private final TCThreadGroup                        threadGroup;

  protected final PreparedComponentsFromL2Connection connectionComponents;
  private final ProductID                            productId;

  private ClientMessageChannel                       channel;
  private CommunicationsManager                      communicationsManager;
  private ClientHandshakeManager                     clientHandshakeManager;

  private CounterManager                             counterManager;
  private final CallbackDumpHandler                  dumpHandler                         = new CallbackDumpHandler();

  private Stage<ClusterInternalEventsContext> clusterEventsStage;

  private final TCSecurityManager                    securityManager;

  private final String                                 uuid;
  private final String                               name;
  private final boolean                              diagnostic;

  private ClientShutdownManager                      shutdownManager;

  private final Thread                               shutdownAction;

  private final SetOnceFlag                          clientStopped                       = new SetOnceFlag();
  private final SetOnceFlag                          connectionMade                       = new SetOnceFlag();
  private final SetOnceRef<Exception>                exceptionMade                       = new SetOnceRef<Exception>();
 
  private ClientEntityManager clientEntityManager;
  private final StageManager communicationStageManager;

  
  public DistributedObjectClient(ClientConfig config, TCThreadGroup threadGroup,
                                 PreparedComponentsFromL2Connection connectionComponents,
                                 ClusterInternal cluster) {
    this(config, new StandardClientBuilder(), threadGroup, connectionComponents, cluster, null,
        UUID.NULL_ID.toString(), "", null, false);
  }

  public DistributedObjectClient(ClientConfig config, ClientBuilder builder, TCThreadGroup threadGroup,
                                 PreparedComponentsFromL2Connection connectionComponents,
                                 ClusterInternal cluster, TCSecurityManager securityManager,
                                 String uuid, String name, ProductID productId,  boolean diagnostic) {
    this.productId = productId;
    Assert.assertNotNull(config);
    this.config = config;
    this.securityManager = securityManager;
    this.connectionComponents = connectionComponents;
    this.cluster = cluster;
    this.threadGroup = threadGroup;
    this.clientBuilder = builder;
    this.uuid = uuid;
    this.name = name;
    this.diagnostic = diagnostic;
    this.shutdownAction = new Thread(new ShutdownAction(), L1VMShutdownHookName);
    Runtime.getRuntime().addShutdownHook(this.shutdownAction);
    
    // We need a StageManager to create the SEDA stages used for handling the messages.
    final SEDA<Void> seda = new SEDA<Void>(threadGroup);
    communicationStageManager = seda.getStageManager();
  }

  private void validateSecurityConfig() {
    if (config.getSecurityInfo().isSecure() && securityManager == null) { throw new TCRuntimeException(
                                                                                                       "client configured as secure but was constructed without securityManager"); }
    if (!config.getSecurityInfo().isSecure() && securityManager != null) { throw new TCRuntimeException(
                                                                                                        "client not configured as secure but was constructed with securityManager"); }
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

  public Stage<ClusterInternalEventsContext> getClusterEventsStage() {
    return clusterEventsStage;
  }

  public synchronized void start() {
    validateSecurityConfig();

    final TCProperties tcProperties = TCPropertiesImpl.getProperties();
    final int maxSize = tcProperties.getInt(TCPropertiesConsts.L1_SEDA_STAGE_SINK_CAPACITY);

    final SessionManager sessionManager = new SessionManagerImpl(new SessionManagerImpl.SequenceFactory() {
      @Override
      public Sequence newSequence() {
        return new SimpleSequence();
      }
    });

    this.threadGroup.addCallbackOnExitDefaultHandler(new CallbackOnExitHandler() {
      @Override
      public void callbackOnExit(CallbackOnExitState state) {
        cluster.fireNodeError();
      }
    });
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.communicationStageManager));

    final ReconnectConfig l1ReconnectConfig = getReconnectPropertiesFromServer();

    final boolean useOOOLayer = l1ReconnectConfig.getReconnectEnabled();
    final NetworkStackHarnessFactory networkStackHarnessFactory = getNetworkStackHarnessFactory(useOOOLayer,
                                                                                                l1ReconnectConfig);

    this.counterManager = new CounterManagerImpl();
    final MessageMonitor mm = MessageMonitorImpl.createMonitor(tcProperties, DSO_LOGGER);
    final TCMessageRouter messageRouter = new TCMessageRouterImpl();

    this.communicationsManager = this.clientBuilder
        .createCommunicationsManager(mm,
                                     messageRouter,
                                     networkStackHarnessFactory,
                                     new NullConnectionPolicy(),
                                     1,
                                     new HealthCheckerConfigClientImpl(tcProperties
                                         .getPropertiesFor(TCPropertiesConsts.L1_L2_HEALTH_CHECK_CATEGORY), "DSO Client"),
                                     getMessageTypeClassMapping(),
            ReconnectionRejectedHandlerL1.SINGLETON, securityManager, productId);

    DSO_LOGGER.debug("Created CommunicationsManager.");

    clusterEventsStage = this.communicationStageManager.createStage(ClientConfigurationContext.CLUSTER_EVENTS_STAGE, ClusterInternalEventsContext.class, new ClusterInternalEventsHandler<ClusterInternalEventsContext>(cluster), 1, maxSize);

    final int socketConnectTimeout = tcProperties.getInt(TCPropertiesConsts.L1_SOCKET_CONNECT_TIMEOUT);

    if (socketConnectTimeout < 0) { throw new IllegalArgumentException("invalid socket time value: "
                                                                       + socketConnectTimeout); }
    this.channel = this.clientBuilder.createClientMessageChannel(this.communicationsManager,
                                                                 sessionManager,
                                                                 MAX_CONNECT_TRIES, socketConnectTimeout, this);

    final ClientIDLoggerProvider cidLoggerProvider = new ClientIDLoggerProvider(this.channel);
    this.communicationStageManager.setLoggerProvider(cidLoggerProvider);

    DSO_LOGGER.debug("Created channel.");

    this.clientEntityManager = this.clientBuilder.createClientEntityManager(this.channel, this.communicationStageManager);
    RequestReceiveHandler receivingHandler = new RequestReceiveHandler(this.clientEntityManager);
    MultiRequestReceiveHandler mutil = new MultiRequestReceiveHandler(this.clientEntityManager);
    Stage<VoltronEntityResponse> entityResponseStage = this.communicationStageManager.createStage(ClientConfigurationContext.VOLTRON_ENTITY_RESPONSE_STAGE, VoltronEntityResponse.class, receivingHandler, 1, maxSize);
    Stage<VoltronEntityMultiResponse> multiResponseStage = this.communicationStageManager.createStage(ClientConfigurationContext.VOLTRON_ENTITY_MULTI_RESPONSE_STAGE, VoltronEntityMultiResponse.class, mutil, 1, maxSize);
    Stage<Void> serverMessageStage = this.communicationStageManager.createStage(ClientConfigurationContext.SERVER_ENTITY_MESSAGE_STAGE, Void.class, new ServerMessageReceiveHandler<Void>(channel), 1, maxSize);

    TerracottaOperatorEventLogging.setNodeNameProvider(new ClientNameProvider(this.cluster));

    final SampledRateCounterConfig sampledRateCounterConfig = new SampledRateCounterConfig(1, 300, true);
    this.counterManager.createCounter(sampledRateCounterConfig);
    this.counterManager.createCounter(sampledRateCounterConfig);

    // for SRA L1 Tx count
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, true, 0L);
    this.counterManager.createCounter(sampledCounterConfig);

    this.threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(this.clientEntityManager));
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.clientEntityManager));

    final Stage<HydrateContext> hydrateStage = this.communicationStageManager.createStage(ClientConfigurationContext.HYDRATE_MESSAGE_STAGE, HydrateContext.class, new HydrateHandler(), 1, maxSize);

    // By design this stage needs to be single threaded. If it wasn't then cluster membership messages could get
    // processed before the client handshake ack, and this client would get a faulty view of the cluster at best, or
    // more likely an AssertionError
    final Stage<PauseContext> pauseStage = this.communicationStageManager.createStage(ClientConfigurationContext.CLIENT_COORDINATION_STAGE, PauseContext.class, new ClientCoordinationHandler<PauseContext>(), 1, maxSize);
    final Sink<PauseContext> pauseSink = pauseStage.getSink();

    final Stage<Void> clusterMembershipEventStage = this.communicationStageManager.createStage(ClientConfigurationContext.CLUSTER_MEMBERSHIP_EVENT_STAGE, Void.class, new ClusterMembershipEventsHandler<Void>(cluster), 1, maxSize);

    final ProductInfo pInfo = ProductInfo.getInstance();
    this.clientHandshakeManager = this.clientBuilder
        .createClientHandshakeManager(new ClientIDLogger(this.channel, TCLogging
                                          .getLogger(ClientHandshakeManagerImpl.class)), this.channel
                                          .getClientHandshakeMessageFactory(), sessionManager,
                                      cluster, this.uuid, this.name, pInfo.version(), this.clientEntityManager);

    ClientChannelEventController.connectChannelEventListener(channel, clientHandshakeManager);

    this.shutdownManager = new ClientShutdownManager(this, connectionComponents);

    final ClientConfigurationContext cc = new ClientConfigurationContext(this.communicationStageManager,
                                                                         this.clientEntityManager,
                                                                         this.clientHandshakeManager);
    // DO NOT create any stages after this call
    
    String[] exclusion = (diagnostic) ? 
    new String[] {
      ClientConfigurationContext.CLUSTER_EVENTS_STAGE,
      ClientConfigurationContext.CLUSTER_MEMBERSHIP_EVENT_STAGE,
      ClientConfigurationContext.LOCK_RESPONSE_STAGE,
      ClientConfigurationContext.VOLTRON_ENTITY_MULTI_RESPONSE_STAGE,
      ClientConfigurationContext.SERVER_ENTITY_MESSAGE_STAGE} : 
    new String[] {
      ClientConfigurationContext.LOCK_RESPONSE_STAGE,
    };

    this.communicationStageManager.startAll(cc, Collections.<PostInit> emptyList(), exclusion);

    initChannelMessageRouter(messageRouter, hydrateStage.getSink(), pauseSink, clusterMembershipEventStage.getSink(), entityResponseStage.getSink(), multiResponseStage.getSink(), serverMessageStage.getSink());
    new Thread(threadGroup, new Runnable() {
        public void run() {
          while (!clientStopped.isSet()) {
            try {
              openChannel();
              waitForHandshake();
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
        }
      }, "Connection Maker - " + uuid).start();    
  }

  private void connectionMade() {
    connectionMade.attemptSet();
    synchronized (connectionMade) {
      connectionMade.notifyAll();
    }
  }
  
  public boolean waitForConnection(long timeout, TimeUnit units) throws InterruptedException {
    long left = timeout > 0 ? units.toMillis(timeout) : Long.MAX_VALUE;
    synchronized(connectionMade) {
      while (!connectionMade.isSet() && !exceptionMade.isSet() && left > 0) {
        long start = System.currentTimeMillis();
        connectionMade.wait(units.toMillis(timeout));
        left -= (System.currentTimeMillis() - start);
      }
    }
    if (exceptionMade.isSet()) {
      Exception exp = exceptionMade.get();
      throw new RuntimeException(exp);
    }
    return connectionMade.isSet();
  }

  private void openChannel() throws InterruptedException {
    String hostname;
    int port;
    Collection<ConnectionInfo> infos = Arrays.asList(this.connectionComponents.createConnectionInfoConfigItem().getConnectionInfos());
    if (infos.isEmpty()) {
//  can't open a connection to nowhere
      return;
    }
    ConnectionInfo info = infos.iterator().next();
    hostname = info.getHostname();
    port = info.getPort();
    synchronized(clientStopped) {
      while (!clientStopped.isSet()) {
        try {
          DSO_LOGGER.debug("Trying to open channel....");
          final char[] pw;
          final String username;
          if (config.getSecurityInfo().hasCredentials()) {
            Assert.assertNotNull(securityManager);
            username = config.getSecurityInfo().getUsername();
            pw = securityManager.getPasswordForTC(config.getSecurityInfo().getUsername(), hostname, port);
          } else {
            pw = null;
            username = null;
          }
          this.channel.open(infos, username, pw);
          DSO_LOGGER.debug("Channel open");
          break;
        } catch (final TCTimeoutException tcte) {
          CONSOLE_LOGGER.warn("Timeout connecting to server: " + tcte.getMessage());
          clientStopped.wait(5000);
        } catch (final ConnectException e) {
          CONSOLE_LOGGER.warn("Connection refused from server: " + e);
          clientStopped.wait(5000);
        } catch (final MaxConnectionsExceededException e) {
          DSO_LOGGER.fatal(e.getMessage());
          CONSOLE_LOGGER.fatal(e.getMessage());
          throw new IllegalStateException(e.getMessage(), e);
        } catch (final CommStackMismatchException e) {
          DSO_LOGGER.fatal(e.getMessage());
          CONSOLE_LOGGER.fatal(e.getMessage());
          throw new IllegalStateException(e.getMessage(), e);
        } catch (final IOException ioe) {
          CONSOLE_LOGGER.warn("IOException connecting to server: " + hostname + ":" + port + ". "
                              + ioe.getMessage());
          clientStopped.wait(5000);
        }
      }
    }
  }

  private void waitForHandshake() {
    this.clientHandshakeManager.waitForHandshake();
    if (this.channel != null) {
      final TCSocketAddress remoteAddress = this.channel.getRemoteAddress();
      final String infoMsg = "Connection successfully established to server at " + remoteAddress;
      CONSOLE_LOGGER.info(infoMsg);
      DSO_LOGGER.info(infoMsg);
    }
  }

  private Map<TCMessageType, Class<? extends TCMessage>> getMessageTypeClassMapping() {
    final Map<TCMessageType, Class<? extends TCMessage>> messageTypeClassMapping = new HashMap<TCMessageType, Class<? extends TCMessage>>();

    messageTypeClassMapping.put(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, ClientHandshakeMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, ClientHandshakeAckMessageImpl.class);
    messageTypeClassMapping
        .put(TCMessageType.CLIENT_HANDSHAKE_REFUSED_MESSAGE, ClientHandshakeRefusedMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, ClusterMembershipMessage.class);
    messageTypeClassMapping.put(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE, ListRegisteredServicesMessage.class);
    messageTypeClassMapping.put(TCMessageType.LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE,
                                ListRegisteredServicesResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE, InvokeRegisteredServiceMessage.class);
    messageTypeClassMapping.put(TCMessageType.INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE,
                                InvokeRegisteredServiceResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_MESSAGE, NetworkVoltronEntityMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE, VoltronEntityReceivedResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE, VoltronEntityAppliedResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_RETIRED_RESPONSE, VoltronEntityRetiredResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_MULTI_RESPONSE, VoltronEntityMultiResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.DIAGNOSTIC_REQUEST, DiagnosticMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.DIAGNOSTIC_RESPONSE, DiagnosticResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.SERVER_ENTITY_MESSAGE, ServerEntityMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SERVER_ENTITY_RESPONSE_MESSAGE, ServerEntityResponseMessageImpl.class);
    return messageTypeClassMapping;
  }

  private void initChannelMessageRouter(TCMessageRouter messageRouter, Sink<HydrateContext> hydrateSink,
                                        Sink<PauseContext> pauseSink,
                                        Sink<Void> clusterMembershipEventSink, Sink<VoltronEntityResponse> responseSink, Sink<VoltronEntityMultiResponse> multiSink, Sink<Void> serverEntityMessageSink) {
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, pauseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_REFUSED_MESSAGE, pauseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_REDIRECT_MESSAGE, pauseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, clusterMembershipEventSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE, responseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE, responseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_RETIRED_RESPONSE, responseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_MULTI_RESPONSE, multiSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.DIAGNOSTIC_RESPONSE, responseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.SERVER_ENTITY_MESSAGE, serverEntityMessageSink, hydrateSink);
    DSO_LOGGER.debug("Added message routing types.");
  }

  public ClientEntityManager getEntityManager() {
    return this.clientEntityManager;
  }

  public CommunicationsManager getCommunicationsManager() {
    return this.communicationsManager;
  }

  public ClientMessageChannel getChannel() {
    return this.channel;
  }

  public ClientHandshakeManager getClientHandshakeManager() {
    return this.clientHandshakeManager;
  }

  @Override
  public void dump() {
    this.dumpHandler.dump();
  }

  protected ClientConfig getClientConfigHelper() {
    return this.config;
  }

  public void shutdown() {
    shutdown(false, false);
  }

  void shutdownResources() {
    final TCLogger logger = DSO_LOGGER;

    if (this.counterManager != null) {
      try {
        this.counterManager.shutdown();
      } catch (final Throwable t) {
        logger.error("error shutting down counter manager", t);
      } finally {
        this.counterManager = null;
      }
    }

    try {
      this.communicationStageManager.stopAll();
    } catch (final Throwable t) {
      logger.error("Error stopping stage manager", t);
    }

    if (this.channel != null) {
      try {
        this.channel.close();
      } catch (final Throwable t) {
        logger.error("Error closing channel", t);
      } finally {
        this.channel = null;
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

    CommonShutDownHook.shutdown();
    this.cluster.shutdown();

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
        for (int x=0;x<threadCount;x++) {
          long start = System.currentTimeMillis();
          while (System.currentTimeMillis() < end && t[x].isAlive()) {
            t[x].join(1000);
          }
          logger.info("Destroyed thread " + t[x].getName() + " time to destroy:" + (System.currentTimeMillis() - start) + " millis");
        }
        logger.info("time to destroy thread group:"  + TimeUnit.SECONDS.convert(System.currentTimeMillis() - time, TimeUnit.MILLISECONDS) + " seconds");

        if (this.threadGroup.activeCount() > 0) {
          logger.warn("Timed out waiting for TC thread group threads to die - probable shutdown memory leak\n"
                      + "Live threads: " + getLiveThreads(this.threadGroup));

          Thread threadGroupCleanerThread = new Thread(this.threadGroup.getParent(),
                                                       new TCThreadGroupCleanerRunnable(threadGroup),
                                                       "TCThreadGroup last chance cleaner thread");
          threadGroupCleanerThread.setDaemon(true);
          threadGroupCleanerThread.start();
          logger.warn("Spawning TCThreadGroup last chance cleaner thread");
        } else {
          logger.info("Destroying TC thread group");
          this.threadGroup.destroy();
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
        final List<Thread> l = new ArrayList<Thread>(count);
        for (final Thread t : threads) {
          if (t != null) {
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
      while (threadGroup.activeCount() > 0) {
        for (Thread liveThread : getLiveThreads(threadGroup)) {
          liveThread.interrupt();
        }
        try {
          Thread.sleep(1000);
        } catch (final InterruptedException e) {
          // ignore
        }
      }
      try {
        threadGroup.destroy();
      } catch (Exception e) {
        // the logger is closed by now so we can't even log that
      }
    }
  }

  public Cluster getCluster() {
    return this.cluster;
  }

  private void shutdownClient(boolean fromShutdownHook, boolean forceImmediate) {
    if (this.shutdownManager != null) {
      try {
        this.shutdownManager.execute(fromShutdownHook, forceImmediate);
      } finally {
        // If we're not being called as a result of the shutdown hook, de-register the hook
        if (Thread.currentThread() != this.shutdownAction) {
          try {
            Runtime.getRuntime().removeShutdownHook(this.shutdownAction);
          } catch (final Exception e) {
            // ignore
          }
        }
      }
    }
  }

  private void shutdown(boolean fromShutdownHook, boolean forceImmediate) {
    if (clientStopped.attemptSet()) {
      synchronized (clientStopped) {
        clientStopped.notifyAll();
      }
      DSO_LOGGER.info("shuting down Terracotta Client hook=" + fromShutdownHook + " force=" + forceImmediate);
      shutdownClient(fromShutdownHook, forceImmediate);
    } else {
      DSO_LOGGER.info("Client already shutdown.");
    }
  }

  private class ShutdownAction implements Runnable {
    @Override
    public void run() {
      DSO_LOGGER.info("Running L1 VM shutdown hook");
      shutdown(true, false);
    }
  }
}
