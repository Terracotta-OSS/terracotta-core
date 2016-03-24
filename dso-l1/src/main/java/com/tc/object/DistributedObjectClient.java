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
import com.tc.entity.NetworkVoltronEntityMessageImpl;
import com.tc.entity.ServerEntityMessageImpl;
import com.tc.entity.ServerEntityResponseMessageImpl;
import com.tc.entity.VoltronEntityAppliedResponseImpl;
import com.tc.entity.VoltronEntityReceivedResponseImpl;
import com.tc.entity.VoltronEntityResponse;
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
import com.tc.object.config.ConnectionInfoConfig;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.object.context.PauseContext;
import com.tc.object.handler.ClientCoordinationHandler;
import com.tc.object.handler.ClusterInternalEventsHandler;
import com.tc.object.handler.ClusterMembershipEventsHandler;
import com.tc.object.handler.LockResponseHandler;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.handshakemanager.ClientHandshakeManagerImpl;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.ClientLockManagerConfigImpl;
import com.tc.object.locks.ClientServerExchangeLockContext;
import static com.tc.object.locks.ServerLockContext.Type.GREEDY_HOLDER;
import static com.tc.object.locks.ServerLockContext.Type.HOLDER;
import static com.tc.object.locks.ServerLockContext.Type.PENDING;
import static com.tc.object.locks.ServerLockContext.Type.TRY_PENDING;
import com.tc.object.msg.ClientHandshakeAckMessageImpl;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.msg.ClientHandshakeRefusedMessageImpl;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.msg.InvokeRegisteredServiceMessage;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;
import com.tc.object.msg.ListRegisteredServicesMessage;
import com.tc.object.msg.ListRegisteredServicesResponseMessage;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockResponseMessage;
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
import com.tc.runtime.TCMemoryManagerImpl;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.util.Assert;
import com.tc.util.CommonShutDownHook;
import com.tc.util.ProductInfo;
import com.tc.util.TCTimeoutException;
import com.tc.util.UUID;
import com.tc.util.concurrent.Runners;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.LockState;
import com.tc.util.runtime.ThreadIDManager;
import com.tc.util.runtime.ThreadIDManagerImpl;
import com.tc.util.runtime.ThreadIDMap;
import com.tc.util.runtime.ThreadIDMapImpl;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;
import com.tcclient.cluster.ClusterInternal;
import com.tcclient.cluster.ClusterInternalEventsContext;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
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
  private final ThreadIDMap                          threadIDMap;

  protected final PreparedComponentsFromL2Connection connectionComponents;
  private final ProductID                            productId;

  private ClientMessageChannel                       channel;
  private ClientLockManager                          lockManager;
  private CommunicationsManager                      communicationsManager;
  private ClientHandshakeManager                     clientHandshakeManager;
  private TCProperties                               l1Properties;
  private boolean                                    createDedicatedMBeanServer          = false;
  private CounterManager                             counterManager;
  private ThreadIDManager                            threadIDManager;
  private final CallbackDumpHandler                  dumpHandler                         = new CallbackDumpHandler();
  private TCMemoryManagerImpl                        tcMemManager;

  private Stage<ClusterInternalEventsContext> clusterEventsStage;

  private final TCSecurityManager                    securityManager;

  private final UUID                                 uuid;

  private final TaskRunner                           taskRunner;

  private ClientShutdownManager                      shutdownManager;

  private final Thread                               shutdownAction;

  private final SetOnceFlag                          clientStopped                       = new SetOnceFlag();
  private final SetOnceFlag                          connectionMade                       = new SetOnceFlag();
  private ClientEntityManager clientEntityManager;
  private final StageManager communicationStageManager;

  
  public DistributedObjectClient(ClientConfig config, TCThreadGroup threadGroup,
                                 PreparedComponentsFromL2Connection connectionComponents,
                                 ClusterInternal cluster) {
    this(config, threadGroup, connectionComponents, cluster, null,
        UUID.NULL_ID, null);
  }

  public DistributedObjectClient(ClientConfig config, TCThreadGroup threadGroup,
                                 PreparedComponentsFromL2Connection connectionComponents,
                                 ClusterInternal cluster, TCSecurityManager securityManager,
                                 UUID uuid, ProductID productId) {
    this.productId = productId;
    Assert.assertNotNull(config);
    this.config = config;
    this.securityManager = securityManager;
    this.connectionComponents = connectionComponents;
    this.cluster = cluster;
    this.threadGroup = threadGroup;
    this.threadIDMap = new ThreadIDMapImpl();
    this.clientBuilder = createClientBuilder();
    this.uuid = uuid;
    this.taskRunner = Runners.newDefaultCachedScheduledTaskRunner(threadGroup);
    this.shutdownAction = new Thread(new ShutdownAction(), L1VMShutdownHookName);
    Runtime.getRuntime().addShutdownHook(this.shutdownAction);
    
    // We need a StageManager to create the SEDA stages used for handling the messages.
    final SEDA<Void> seda = new SEDA<Void>(threadGroup);
    communicationStageManager = seda.getStageManager();
    this.tcMemManager = new TCMemoryManagerImpl(threadGroup);
  }

  protected ClientBuilder createClientBuilder() {
    return new StandardClientBuilder();
  }

  @Override
  public ThreadIDMap getThreadIDMap() {
    return this.threadIDMap;
  }

  @Override
  public void addAllLocksTo(LockInfoByThreadID lockInfo) {
    if (this.lockManager != null) {
      for (final ClientServerExchangeLockContext c : this.lockManager.getAllLockContexts()) {
        switch (c.getState().getType()) {
          case GREEDY_HOLDER:
          case HOLDER:
            lockInfo.addLock(LockState.HOLDING, c.getThreadID(), c.getLockID().toString());
            break;
          case WAITER:
            lockInfo.addLock(LockState.WAITING_ON, c.getThreadID(), c.getLockID().toString());
            break;
          case TRY_PENDING:
          case PENDING:
            lockInfo.addLock(LockState.WAITING_TO, c.getThreadID(), c.getLockID().toString());
            break;
          default:
            throw new AssertionError(c.getState().getType());
        }
      }
    } else {
      DSO_LOGGER.error("LockManager not initialised still. LockInfo for threads cannot be updated");
    }
  }

  public void setCreateDedicatedMBeanServer(boolean createDedicatedMBeanServer) {
    this.createDedicatedMBeanServer = createDedicatedMBeanServer;
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
        return true;
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
    final boolean checkClientServerVersions = tcProperties.getBoolean(TCPropertiesConsts.VERSION_COMPATIBILITY_CHECK);
    this.l1Properties = tcProperties.getPropertiesFor("l1");
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
                                     this.connectionComponents.createConnectionInfoConfigItemByGroup().length,
                                     new HealthCheckerConfigClientImpl(this.l1Properties
                                         .getPropertiesFor("healthcheck.l2"), "DSO Client"),
                                     getMessageTypeClassMapping(),
            ReconnectionRejectedHandlerL1.SINGLETON, securityManager, productId);

    DSO_LOGGER.debug("Created CommunicationsManager.");

    final ConnectionInfoConfig[] connectionInfoItems = this.connectionComponents
        .createConnectionInfoConfigItemByGroup();
    final ConnectionInfo[] connectionInfo = connectionInfoItems[0].getConnectionInfos();
    final String serverHost = connectionInfo[0].getHostname();
    final int serverPort = connectionInfo[0].getPort();

    clusterEventsStage = this.communicationStageManager.createStage(ClientConfigurationContext.CLUSTER_EVENTS_STAGE, ClusterInternalEventsContext.class, new ClusterInternalEventsHandler<ClusterInternalEventsContext>(cluster), 1, maxSize);

    final int socketConnectTimeout = tcProperties.getInt(TCPropertiesConsts.L1_SOCKET_CONNECT_TIMEOUT);

    if (socketConnectTimeout < 0) { throw new IllegalArgumentException("invalid socket time value: "
                                                                       + socketConnectTimeout); }
    this.channel = this.clientBuilder.createClientMessageChannel(this.communicationsManager,
                                                                 this.connectionComponents, sessionManager,
                                                                 MAX_CONNECT_TRIES, socketConnectTimeout, this);

    final ClientIDLoggerProvider cidLoggerProvider = new ClientIDLoggerProvider(this.channel);
    this.communicationStageManager.setLoggerProvider(cidLoggerProvider);

    DSO_LOGGER.debug("Created channel.");

    this.clientEntityManager = this.clientBuilder.createClientEntityManager(this.channel, this.communicationStageManager);
    RequestReceiveHandler receivingHandler = new RequestReceiveHandler(this.clientEntityManager);
    Stage<VoltronEntityResponse> entityResponseStage = this.communicationStageManager.createStage(ClientConfigurationContext.VOLTRON_ENTITY_RESPONSE_STAGE, VoltronEntityResponse.class, receivingHandler, 1, maxSize);

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

    final long timeOut = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.LOGGING_LONG_GC_THRESHOLD);
    final LongGCLogger gcLogger = this.clientBuilder.createLongGCLogger(timeOut);
    this.tcMemManager.registerForMemoryEvents(gcLogger);
    // CDV-1181 warn if using CMS
    this.tcMemManager.checkGarbageCollectors();

    this.threadIDManager = new ThreadIDManagerImpl(this.threadIDMap);
    // Setup the lock manager
    this.lockManager = this.clientBuilder
        .createLockManager(this.channel,
                           new ClientIDLogger(this.channel, TCLogging
                               .getLogger(ClientLockManager.class)), sessionManager, this.channel
                               .getLockRequestMessageFactory(), this.threadIDManager,
            new ClientLockManagerConfigImpl(this.l1Properties.getPropertiesFor("lockmanager")),
            this.taskRunner);
    final CallbackDumpAdapter lockDumpAdapter = new CallbackDumpAdapter(this.lockManager);
    this.threadGroup.addCallbackOnExitDefaultHandler(lockDumpAdapter);
    this.dumpHandler.registerForDump(lockDumpAdapter);

    // Create the SEDA stages
    final Stage<Void> lockResponse = this.communicationStageManager.createStage(ClientConfigurationContext.LOCK_RESPONSE_STAGE, Void.class, new LockResponseHandler<Void>(sessionManager), 1, maxSize);

    final Stage<HydrateContext> hydrateStage = this.communicationStageManager.createStage(ClientConfigurationContext.HYDRATE_MESSAGE_STAGE, HydrateContext.class, new HydrateHandler(), 1, maxSize);

    // By design this stage needs to be single threaded. If it wasn't then cluster membership messages could get
    // processed before the client handshake ack, and this client would get a faulty view of the cluster at best, or
    // more likely an AssertionError
    final Stage<PauseContext> pauseStage = this.communicationStageManager.createStage(ClientConfigurationContext.CLIENT_COORDINATION_STAGE, PauseContext.class, new ClientCoordinationHandler<PauseContext>(), 1, maxSize);
    final Sink<PauseContext> pauseSink = pauseStage.getSink();

    final Stage<Void> clusterMembershipEventStage = this.communicationStageManager.createStage(ClientConfigurationContext.CLUSTER_MEMBERSHIP_EVENT_STAGE, Void.class, new ClusterMembershipEventsHandler<Void>(cluster), 1, maxSize);
    final List<ClientHandshakeCallback> clientHandshakeCallbacks = new ArrayList<ClientHandshakeCallback>();
    clientHandshakeCallbacks.add(this.lockManager);
    clientHandshakeCallbacks.add(this.clientEntityManager);
    final ProductInfo pInfo = ProductInfo.getInstance();
    this.clientHandshakeManager = this.clientBuilder
        .createClientHandshakeManager(new ClientIDLogger(this.channel, TCLogging
                                          .getLogger(ClientHandshakeManagerImpl.class)), this.channel
                                          .getClientHandshakeMessageFactory(), pauseSink, sessionManager,
                                      cluster, pInfo.version(), Collections
                                          .unmodifiableCollection(clientHandshakeCallbacks));

    ClientChannelEventController.connectChannelEventListener(channel, pauseSink, clientHandshakeManager);

    this.shutdownManager = new ClientShutdownManager(this, connectionComponents);

    final ClientConfigurationContext cc = new ClientConfigurationContext(this.communicationStageManager, this.lockManager,
                                                                         this.clientEntityManager,
                                                                         this.clientHandshakeManager);
    // DO NOT create any stages after this call
    this.communicationStageManager.startAll(cc, Collections.<PostInit> emptyList());

    initChannelMessageRouter(messageRouter, hydrateStage.getSink(), lockResponse.getSink(), pauseSink, clusterMembershipEventStage.getSink(), entityResponseStage.getSink(), serverMessageStage.getSink());
    new Thread(threadGroup, new Runnable() {
        public void run() {
          boolean interrupted = false;
          while (!clientStopped.isSet()) {
            try {
              openChannel(serverHost, serverPort);
              waitForHandshake();
              connectionMade();
              break;
            } catch (InterruptedException ie) {
              interrupted = true;
            }
          }
          //  don't reset interrupted, thread is done
        }
      }, "Connection Establisher - " + uuid).start();    
  }
  
  private synchronized void connectionMade() {
    connectionMade.attemptSet();
    notifyAll();
  }
  
  public synchronized boolean waitForConnection(long timeout, TimeUnit units) throws InterruptedException {
    long left = timeout > 0 ? units.toMillis(timeout) : Long.MAX_VALUE;
    while (!connectionMade.isSet() && left > 0) {
      long start = System.currentTimeMillis();
      this.wait(units.toMillis(timeout));
      left -= (System.currentTimeMillis() - start);
    }
    return connectionMade.isSet();
  }

  private synchronized void openChannel(String serverHost, int serverPort) throws InterruptedException {
    while (!clientStopped.isSet()) {
      try {
        DSO_LOGGER.debug("Trying to open channel....");
        final char[] pw;
        if (config.getSecurityInfo().hasCredentials()) {
          Assert.assertNotNull(securityManager);
          pw = securityManager.getPasswordForTC(config.getSecurityInfo().getUsername(), serverHost, serverPort);
        } else {
          pw = null;
        }
        this.channel.open(pw);
        DSO_LOGGER.debug("Channel open");
        break;
      } catch (final TCTimeoutException tcte) {
        CONSOLE_LOGGER.warn("Timeout connecting to server: " + tcte.getMessage());
        this.wait(5000);
      } catch (final ConnectException e) {
        CONSOLE_LOGGER.warn("Connection refused from server: " + e);
        this.wait(5000);
      } catch (final MaxConnectionsExceededException e) {
        DSO_LOGGER.fatal(e.getMessage());
        CONSOLE_LOGGER.fatal(e.getMessage());
        throw new IllegalStateException(e.getMessage(), e);
      } catch (final CommStackMismatchException e) {
        DSO_LOGGER.fatal(e.getMessage());
        CONSOLE_LOGGER.fatal(e.getMessage());
        throw new IllegalStateException(e.getMessage(), e);
      } catch (final IOException ioe) {
        CONSOLE_LOGGER.warn("IOException connecting to server: " + serverHost + ":" + serverPort + ". "
                            + ioe.getMessage());
        this.wait(5000);
      }
    }

  }

  private synchronized void waitForHandshake() {
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

    messageTypeClassMapping.put(TCMessageType.LOCK_REQUEST_MESSAGE, LockRequestMessage.class);
    messageTypeClassMapping.put(TCMessageType.LOCK_RESPONSE_MESSAGE, LockResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.LOCK_RECALL_MESSAGE, LockResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, LockResponseMessage.class);
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
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_APPLIED_RESPONSE, VoltronEntityAppliedResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.SERVER_ENTITY_MESSAGE, ServerEntityMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SERVER_ENTITY_RESPONSE_MESSAGE, ServerEntityResponseMessageImpl.class);
    return messageTypeClassMapping;
  }

  private void initChannelMessageRouter(TCMessageRouter messageRouter, Sink<HydrateContext> hydrateSink, Sink<Void> lockResponseSink,
                                        Sink<PauseContext> pauseSink,
                                        Sink<Void> clusterMembershipEventSink, Sink<VoltronEntityResponse> responseSink, Sink<Void> serverEntityMessageSink) {
    messageRouter.routeMessageType(TCMessageType.LOCK_RESPONSE_MESSAGE, lockResponseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, lockResponseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.LOCK_RECALL_MESSAGE, lockResponseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, pauseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_REFUSED_MESSAGE, pauseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, clusterMembershipEventSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_APPLIED_RESPONSE, responseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE, responseSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.SERVER_ENTITY_MESSAGE, serverEntityMessageSink, hydrateSink);
    DSO_LOGGER.debug("Added message routing types.");
  }

  private void setLoggerOnExit() {
    CommonShutDownHook.addShutdownHook(new Runnable() {
      @Override
      public void run() {
        DSO_LOGGER.info("L1 Exiting...");
      }
    });
  }

  public ClientLockManager getLockManager() {
    return this.lockManager;
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

    if (this.tcMemManager != null) {
      try {
        this.tcMemManager.shutdown();
      } catch (final Throwable t) {
        logger.error("Error stopping memory manager", t);
      } finally {
        this.tcMemManager = null;
      }
    }

    if (this.lockManager != null) {
      try {
        this.lockManager.shutdown(false);
      } catch (final Throwable t) {
        logger.error("Error stopping lock manager", t);
      } finally {
        this.lockManager = null;
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

    if (taskRunner != null) {
      logger.info("Shutting down TaskRunner");
      taskRunner.shutdown();
    }

    CommonShutDownHook.shutdown();
    this.cluster.shutdown();

    if (this.threadGroup != null) {
      boolean interrupted = false;

      try {
        final long end = System.currentTimeMillis()
                         + TCPropertiesImpl.getProperties()
                             .getLong(TCPropertiesConsts.L1_SHUTDOWN_THREADGROUP_GRACETIME);

        while (this.threadGroup.activeCount() > 0 && System.currentTimeMillis() < end) {
          try {
            Thread.sleep(1000);
          } catch (final InterruptedException e) {
            interrupted = true;
          }
        }
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
    return uuid.toString();
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
      DSO_LOGGER.info("shuting down Terracotta Client hook=" + fromShutdownHook + " force=" + forceImmediate);
      shutdownClient(fromShutdownHook, forceImmediate);
    } else {
      DSO_LOGGER.info("Client already shutdown.");
    }
    synchronized (this) {
//  notify in case the connection establisher is waiting for something
      notifyAll();
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
