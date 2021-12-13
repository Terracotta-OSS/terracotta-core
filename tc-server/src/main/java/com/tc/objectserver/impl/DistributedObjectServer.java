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
package com.tc.objectserver.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventHandlerException;

import com.tc.config.ServerConfigurationManager;
import com.tc.config.GroupConfiguration;
import com.tc.l2.state.DiagnosticModeConsistencyManager;
import com.tc.l2.state.SafeStartupManagerImpl;
import com.tc.logging.TCLogging;
import com.tc.net.core.ClearTextBufferManagerFactory;
import com.tc.objectserver.api.EntityManager;
import com.tc.services.PlatformConfigurationImpl;
import com.tc.services.PlatformServiceProvider;
import com.tc.services.SingleThreadedTimer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.persistence.IPlatformPersistence;

import com.tc.async.api.PostInit;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.async.impl.OrderedSink;
import com.tc.async.impl.StageController;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.entity.DiagnosticMessageImpl;
import com.tc.entity.DiagnosticResponseImpl;
import com.tc.entity.LinearVoltronEntityMultiResponse;
import com.tc.entity.NetworkVoltronEntityMessageImpl;
import com.tc.entity.VoltronEntityAppliedResponseImpl;
import com.tc.entity.VoltronEntityMessage;
import com.tc.entity.VoltronEntityReceivedResponseImpl;
import com.tc.entity.VoltronEntityRetiredResponseImpl;
import com.tc.exception.TCRuntimeException;
import com.tc.exception.TCServerRestartException;
import com.tc.exception.TCShutdownServerException;
import com.tc.exception.ZapDirtyDbServerNodeException;
import com.tc.exception.ZapServerNodeException;
import com.tc.handler.CallbackGroupExceptionHandler;
import com.tc.handler.CallbackZapDirtyDbExceptionAdapter;
import com.tc.handler.CallbackZapServerNodeExceptionAdapter;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.BlockTimeWeightGenerator;
import com.tc.l2.ha.ChannelWeightGenerator;
import com.tc.l2.ha.ConnectionIDWeightGenerator;
import com.tc.l2.ha.ConsistencyManagerWeightGenerator;
import com.tc.l2.ha.GenerationWeightGenerator;
import com.tc.l2.ha.HASettingsChecker;
import com.tc.l2.ha.InitialStateWeightGenerator;
import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.l2.ha.SequenceIDWeightGenerator;
import com.tc.l2.ha.ServerUptimeWeightGenerator;
import com.tc.l2.ha.StripeIDStateManagerImpl;
import com.tc.l2.ha.TopologyWeightGenerator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.handler.GroupEvent;
import com.tc.l2.handler.GroupEventsDispatchHandler;
import com.tc.l2.handler.L2StateMessageHandler;
import com.tc.l2.handler.PlatformInfoRequestHandler;
import com.tc.l2.logging.TCLogbackLogging;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.msg.PlatformInfoRequest;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.msg.SyncReplicationActivity;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.FLUSH_LOCAL_PIPELINE;
import static java.lang.Math.max;

import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.ThreadDumpHandler;
import com.tc.net.AddressChecker;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.TransportHandshakeErrorNullHandler;
import com.tc.net.utils.L2Utils;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.msg.ClientHandshakeAckMessageImpl;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.msg.ClientHandshakeRefusedMessageImpl;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerImpl;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.entity.ActiveToPassiveReplication;
import com.tc.objectserver.handler.ClientChannelLifeCycleHandler;
import com.tc.objectserver.handler.ClientHandshakeHandler;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.persistence.ClientStatePersistor;
import com.tc.objectserver.persistence.Persistor;
import com.tc.objectserver.persistence.NullPlatformStorageServiceProvider;
import com.tc.objectserver.persistence.NullPlatformStorageProviderConfiguration;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.TCServer;
import com.tc.services.CommunicatorService;
import com.tc.services.EntityMessengerProvider;
import com.tc.services.LocalMonitoringProducer;
import com.tc.services.TerracottaServiceProviderRegistryImpl;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;
import com.tc.util.TCTimeoutException;
import com.tc.util.UUID;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import com.tc.objectserver.entity.ClientEntityStateManager;
import com.tc.objectserver.entity.ClientEntityStateManagerImpl;
import com.tc.objectserver.entity.EntityManagerImpl;
import com.tc.objectserver.entity.LocalPipelineFlushMessage;
import com.tc.objectserver.entity.ReplicationSender;
import com.tc.objectserver.entity.RequestProcessor;
import com.tc.objectserver.handler.GenericHandler;
import com.tc.objectserver.handler.ReplicatedTransactionHandler;
import com.tc.objectserver.handler.VoltronMessageHandler;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.services.InternalServiceRegistry;
import com.tc.text.MapListPrettyPrint;
import com.tc.net.core.ProductID;
import java.net.BindException;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.terracotta.entity.BasicServiceConfiguration;
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.ConsistencyManagerImpl;
import com.tc.l2.state.ServerMode;
import com.tc.net.ClientID;
import com.tc.net.core.BufferManagerFactory;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerImpl;
import com.tc.net.protocol.tcm.HydrateContext;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageHydrateSink;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.DisabledHealthCheckerConfigImpl;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.NullConnectionIDFactoryImpl;
import com.tc.objectserver.handler.ResponseMessage;
import java.io.PrintWriter;
import java.io.StringWriter;
import com.tc.objectserver.entity.VoltronMessageSink;
import com.tc.objectserver.handler.ReplicationReceivingAction;
import com.tc.objectserver.handler.ReplicationSendingAction;
import com.tc.objectserver.handshakemanager.ClientHandshakePrettyPrintable;
import com.tc.objectserver.persistence.NullPersistor;
import com.tc.spi.DiagnosticFormat;
import com.tc.spi.Guardian;
import com.tc.spi.NetworkTranslator;
import com.tc.spi.ProductCapabilities;
import org.terracotta.configuration.Configuration;
import org.terracotta.configuration.ServerConfiguration;
import java.net.InetSocketAddress;

import java.util.stream.Collectors;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.ThreadUtil;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.terracotta.configuration.FailoverBehavior;
import org.terracotta.server.ServerEnv;

/**
 * Startup and shutdown point. Builds and starts the server
 */
public class DistributedObjectServer {
  private final ConnectionPolicy                 connectionPolicy;
  private final TCServer                         server;
  private final ServerBuilder                    serverBuilder;
  protected final ServerConfigurationManager configSetupManager;

  private static final Logger logger = LoggerFactory.getLogger(DistributedObjectServer.class);
  private static final Logger consoleLogger = TCLogging.getConsoleLogger();
  private final TopologyManager topologyManager;

  private ServerID                               thisServerNodeID = ServerID.NULL_ID;
  protected NetworkListener                      l1Listener;
  protected NetworkListener                      l1Diagnostics;
  private CommunicationsManager                  communicationsManager;
  private TCConnectionManager                    connectionManager;
  private ServerConfigurationContext             context;
  private CounterManager                         sampledCounterManager;
  private ServerManagementContext                managementContext;
  private Persistor                              persistor;

  private L2Coordinator                          l2Coordinator;

  private TCProperties                           tcProperties;

  private ConnectionIDFactoryImpl                connectionIdFactory;

  private final TCThreadGroup                    threadGroup;
  private final SEDA                            seda;

  private GroupManager<AbstractGroupMessage> groupCommManager;
  private StripeIDStateManagerImpl               stripeIDStateManager;

  private final SingleThreadedTimer timer;
  private final TerracottaServiceProviderRegistryImpl serviceRegistry;
  private WeightGeneratorFactory globalWeightGeneratorFactory;
  private EntityManagerImpl entityManager;

  private final SetOnceFlag  stopping = new SetOnceFlag();

  // used by a test
  public DistributedObjectServer(ServerConfigurationManager configSetupManager, TCThreadGroup threadGroup,
                                 ConnectionPolicy connectionPolicy) {
    this(configSetupManager, threadGroup, connectionPolicy, new SEDA(threadGroup), null);

  }

  public DistributedObjectServer(ServerConfigurationManager configSetupManager, TCThreadGroup threadGroup,
                                 ConnectionPolicy connectionPolicy,
                                 SEDA seda,
                                 TCServer server) {
    // This assertion is here because we want to assume that all threads spawned by the server (including any created in
    // 3rd party libs) inherit their thread group from the current thread . Consider this before removing the assertion.
    // Even in tests, we probably don't want different thread group configurations
    Assert.assertEquals(threadGroup, Thread.currentThread().getThreadGroup());

    this.configSetupManager = configSetupManager;
    this.connectionPolicy = connectionPolicy;
    this.threadGroup = threadGroup;
    this.seda = seda;
    this.server = server;
    this.serverBuilder = createServerBuilder(configSetupManager.getGroupConfiguration(), logger, server);
    this.timer = new SingleThreadedTimer(null, threadGroup);
    this.timer.start();
    this.serviceRegistry = new TerracottaServiceProviderRegistryImpl();
    this.topologyManager = new TopologyManager(this.configSetupManager.getGroupConfiguration().getHostPorts(), ()-> {
      Configuration config = this.configSetupManager.getConfiguration();
      FailoverBehavior consistent = config.getFailoverPriority();
      if (this.configSetupManager.isPartialConfiguration() || consistent == null || consistent.isAvailability()) {
        return -1;
      } else {
        return consistent.getExternalVoters();
      }
    });
  }

  protected final ServerBuilder createServerBuilder(GroupConfiguration groupConfiguration, Logger tcLogger,
                                                    TCServer server) {
    return new StandardServerBuilder(groupConfiguration, tcLogger);
  }

  protected ServerBuilder getServerBuilder() {
    return this.serverBuilder;
  }

  public byte[] getClusterState(Charset set, PrettyPrinter pp) {
    try {
      if (pp == null) {
        pp = this.serviceRegistry.subRegistry(0).getService(new BasicServiceConfiguration<>(PrettyPrinter.class));
      }
    } catch (ServiceException se) {
      logger.warn("error getting printer for cluster state", se);
    }
    try {
      if (pp == null) {
        DiagnosticFormat format = this.serviceRegistry.subRegistry(0).getService(new BasicServiceConfiguration<>(DiagnosticFormat.class));
        if (format != null) {
            pp = new PrettyPrinter() {
            @Override
            public PrettyPrinter println(Object o) {
              format.print(o);
              return this;
            }

            @Override
            public void flush() {
            }

            @Override
            public String toString() {
              return format.toString();
            }
          };
        }
      }
    } catch (ServiceException se) {
      logger.warn("error getting printer for cluster state", se);
    }
    if (pp == null) {
      pp = new MapListPrettyPrint();
    }
    collectState(this.seda.getStageManager(), pp);
    collectState(this.persistor, pp);
    collectState(this.communicationsManager, pp);
    collectState(new ClientHandshakePrettyPrintable(this.context.getChannelManager().getActiveChannels()), pp);
    collectState(this.groupCommManager, pp);
    collectState(this.l2Coordinator, pp);
    collectState(this.entityManager, pp);
    collectState(this.serviceRegistry, pp);
    collectState(this.managementContext, pp);
    addExtendedConfigState(pp);

    return pp.toString().getBytes(set);
  }

  private static void collectState(PrettyPrintable prettyPrintable, PrettyPrinter prettyPrinter) {
    try {
      prettyPrintable.prettyPrint(prettyPrinter);
    } catch (Throwable t) {
      prettyPrinter.println("unable to collect cluster state for " + prettyPrintable + " : " + t.getLocalizedMessage());
      StringWriter w = new StringWriter();
      PrintWriter p = new PrintWriter(w);
      t.printStackTrace(p);
      p.close();
      prettyPrinter.println(w.toString());
    }
  }

  public void dumpOnExit() {
    // this is on exit so do not guard
    String clusterState = new String(getClusterState(Charset.defaultCharset(), null), Charset.defaultCharset());
    TCLogging.getDumpLogger().info(clusterState);
  }

  private void addExtendedConfigState(PrettyPrinter prettyPrinter) {
    try {
      Map<String, Object> state = new HashMap<>();
      state.put("ExtendedConfigs", this.configSetupManager.getStateMap());
      prettyPrinter.println(state);
    } catch (Throwable t) {
      prettyPrinter.println("unable to collect cluster state for ExtendedConfigs" + " : " + t.getLocalizedMessage());
      StringWriter w = new StringWriter();
      PrintWriter p = new PrintWriter(w);
      t.printStackTrace(p);
      p.close();
      prettyPrinter.println(w.toString());
    }
  }

  public synchronized void start() throws IOException, LocationNotCreatedException, FileNotCreatedException {
    threadGroup.addCallbackOnExitDefaultHandler(new ThreadDumpHandler());
    threadGroup.addCallbackOnExitDefaultHandler((state) -> dumpOnExit());
    threadGroup.addCallbackOnExitExceptionHandler(TCServerRestartException.class, state -> {
      consoleLogger.error("Restarting server: " + state.getThrowable().getMessage());
      state.setRestartNeeded();
    });
    threadGroup.addCallbackOnExitExceptionHandler(TCShutdownServerException.class, state -> {
      Throwable t = state.getThrowable();
      if(t.getCause() != null) {
        consoleLogger.error("Server exiting: " + t.getMessage(), t.getCause());
      } else {
        consoleLogger.error("Server exiting: " + t.getMessage());
      }
    });

    this.thisServerNodeID = makeServerNodeID(this.configSetupManager.getServerConfiguration());
    ThisServerNodeId.setThisServerNodeId(thisServerNodeID);


    final List<PostInit> toInit = new ArrayList<>();

    // perform the DSO network config verification
    final ServerConfiguration l2DSOConfig = this.configSetupManager.getServerConfiguration();
    // verify user input host name, DEV-2293
    final String host = l2DSOConfig.getHost();
    final InetAddress ip = InetAddress.getByName(host);
    if (!ip.isLoopbackAddress() && (NetworkInterface.getByInetAddress(ip) == null)) {
      final String msg = "Unable to find local network interface for " + host;
      consoleLogger.error(msg);
      logger.error(msg, new TCRuntimeException(msg));
      ServerEnv.getServer().stop();
    }

    String bindAddress = this.configSetupManager.getServerConfiguration().getTsaPort().getHostName();

    final InetAddress jmxBind = InetAddress.getByName(bindAddress);
    final AddressChecker addressChecker = new AddressChecker();
    if (!addressChecker.isLegalBindAddress(jmxBind)) { throw new IOException("Invalid bind address [" + jmxBind
                                                                             + "]. Local addresses are "
                                                                             + addressChecker.getAllLocalAddresses()); }

    this.tcProperties = TCPropertiesImpl.getProperties();

    TCByteBufferFactory.setPoolingEnabled(tcProperties.getBoolean(TCPropertiesConsts.BYTEBUFFER_POOLING, false));
    final int maxStageSize = tcProperties.getInt(TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY);
    final int fastStageSize = 1024;
    final StageManager stageManager = this.seda.getStageManager();

    this.sampledCounterManager = new CounterManagerImpl();

    // Set up the ServiceRegistry.
    Configuration configuration = this.configSetupManager.getConfiguration();
    PlatformConfiguration platformConfiguration =
        new PlatformConfigurationImpl(configSetupManager.getServerConfiguration(), configuration);
    serviceRegistry.initialize(platformConfiguration, configuration);
    serviceRegistry.registerImplementationProvided(new PlatformServiceProvider(server));

    final EntityMessengerProvider messengerProvider = new EntityMessengerProvider();
    this.serviceRegistry.registerImplementationProvided(messengerProvider);

    // See if we need to add an in-memory service for IPlatformPersistence.
    if (!this.serviceRegistry.hasUserProvidedServiceProvider(IPlatformPersistence.class)) {
      // In this case, we do still need to provide an implementation of IPlatformPersistence, backed by memory, so that entities can request a service which is as persistent as this server is.
      NullPlatformStorageServiceProvider nullPlatformStorageServiceProvider = new NullPlatformStorageServiceProvider();
      nullPlatformStorageServiceProvider.initialize(new NullPlatformStorageProviderConfiguration(), platformConfiguration);
      serviceRegistry.registerExternal(nullPlatformStorageServiceProvider);
    }

    // We want to register our IMonitoringProducer shim.
    // (note that it requires a PlatformServer instance of THIS server).
    String hostAddress = "";
    try {
      hostAddress = java.net.InetAddress.getByName(host).getHostAddress();
    } catch (UnknownHostException unknown) {
      // ignore
    }
    final int serverPort = l2DSOConfig.getTsaPort().getPort();
    final ProductInfo pInfo = server.productInfo();
    PlatformServer thisServer = new PlatformServer(server.getL2Identifier(), host, hostAddress, bindAddress, serverPort, l2DSOConfig.getGroupPort().getPort(), pInfo.buildVersion(), pInfo.buildID(), ServerEnv.getServer().getStartTime());

    final LocalMonitoringProducer monitoringShimService = new LocalMonitoringProducer(this.configSetupManager.getServiceLocator().getServiceLoader(), this.serviceRegistry, thisServer, this.timer);
    this.serviceRegistry.registerImplementationProvided(monitoringShimService);

    // ***** NOTE:  At this point, since we are about to create a subregistry for the platform, the serviceRegistry must be complete!

    // The platform gets the reserved consumerID 0.
    long platformConsumerID = 0;
    InternalServiceRegistry platformServiceRegistry = serviceRegistry.subRegistry(platformConsumerID);

    Set<ProductID> capablities = EnumSet.allOf(ProductID.class);

    if (serviceRegistry.hasUserProvidedServiceProvider(ProductCapabilities.class)) {
      try {
        capablities = platformServiceRegistry.getService(new BasicServiceConfiguration<>(ProductCapabilities.class)).supportedClients();
      } catch (ServiceException s) {
        logger.warn("multiple service providers for " + ProductCapabilities.class.getName());
      }
    }

    if (configuration.isPartialConfiguration()) {
      // don't persist anything for partial configurations
      persistor = new NullPersistor();
    } else {
      persistor = serverBuilder.createPersistor(platformServiceRegistry);
      boolean wasZapped = false;
      while(!persistor.start(capablities.contains(ProductID.PERMANENT))) {
        wasZapped = true;
        // make sure peristor is not using any storage service
        persistor.close();
        // Log that that the state was not clean so we are going to clear all service provider state.
        logger.warn("DB state not clean!  Clearing all ServiceProvider state (ZAP request)");
        serviceRegistry.clearServiceProvidersState();
        // create the persistor once again as underlying storage service might have cleared its internal state
        persistor = serverBuilder.createPersistor(platformServiceRegistry);
      }
      //  if the DB was zapped and not started in diagnostic mode, reset the flag until the server has finished sync
      persistor.getClusterStatePersistor().setDBClean(!wasZapped);
    }

    new ServerPersistenceVersionChecker(pInfo).checkAndBumpPersistedVersion(persistor.getClusterStatePersistor());

    this.threadGroup
        .addCallbackOnExitExceptionHandler(ZapDirtyDbServerNodeException.class,
                                           new CallbackZapDirtyDbExceptionAdapter(logger, consoleLogger, this.persistor
                                               .getClusterStatePersistor()));
    this.threadGroup
        .addCallbackOnExitExceptionHandler(ZapServerNodeException.class,
                                           new CallbackZapServerNodeExceptionAdapter(logger, consoleLogger,
                                                                                     this.persistor
                                                                                         .getClusterStatePersistor()));


    final int commWorkerThreadCount = L2Utils.getOptimalCommWorkerThreads();

    final NetworkStackHarnessFactory networkStackHarnessFactory;
    networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();

    final MessageMonitor mm = MessageMonitorImpl.createMonitor(tcProperties, logger);

    final TCMessageRouter messageRouter = new TCMessageRouterImpl();

    BufferManagerFactory bufferManagerFactory = getBufferManagerFactory(platformServiceRegistry);

    this.connectionManager = new TCConnectionManagerImpl(configSetupManager.getServerConfiguration().getName() + " - " + CommunicationsManager.COMMSMGR_SERVER, commWorkerThreadCount, bufferManagerFactory);
    this.communicationsManager = new CommunicationsManagerImpl(mm,
                                                               messageRouter, networkStackHarnessFactory,
                                                               this.connectionManager,
                                                               this.connectionPolicy,
                                                               new DisabledHealthCheckerConfigImpl(),
                                                               this.thisServerNodeID,
                                                               new TransportHandshakeErrorNullHandler(),
                                                               getMessageTypeClassMappings(), Collections.emptyMap(),
                                                               bufferManagerFactory
    );

    NullConnectionIDFactoryImpl infoConnections = new NullConnectionIDFactoryImpl();
    ClientStatePersistor clientStateStore = this.persistor.getClientStatePersistor();
    this.connectionIdFactory = new ConnectionIDFactoryImpl(infoConnections, clientStateStore, capablities);
    int voteCount =
        ConsistencyManager.parseVoteCount(configuration.getFailoverPriority(), configuration.getServerConfigurations().size());
    int knownPeers = this.configSetupManager.allCurrentlyKnownServers().length - 1;

    if (voteCount >= 0 && (voteCount + knownPeers + 1) % 2 == 0) {
      consoleLogger.warn("It is recommended to keep the total number of servers and external voters to be an odd number");
    }

    if (knownPeers % 2 == 0 && voteCount > 0) {
      consoleLogger.warn("It is not recommended to configure external voters when there is an odd number of servers in the stripe");
    }

    ConsistencyManager consistencyMgr = createConsistencyManager(configSetupManager, knownPeers, voteCount);

    final InetSocketAddress dsoBind = new InetSocketAddress(l2DSOConfig.getTsaPort().getHostString(), l2DSOConfig.getTsaPort().getPort());
    this.l1Listener = this.communicationsManager.createListener(dsoBind, (MessageChannel c)->!c.getProductID().isReconnectEnabled() || !server.isReconnectWindow(),
                                                                this.connectionIdFactory, (MessageTransport t)->{
                                                                  return getContext().getClientHandshakeManager().isStarting() || t.getConnectionID().getProductId() == ProductID.DIAGNOSTIC || consistencyMgr.requestTransition(context.getL2Coordinator().getStateManager().getCurrentMode(),
                                                                      t.getConnectionID().getClientID(), ConsistencyManager.Transition.ADD_CLIENT);
                                                                });
    this.l1Diagnostics = createDiagnosticsListener(dsoBind, infoConnections);

    this.stripeIDStateManager = new StripeIDStateManagerImpl(this.persistor.getClusterStatePersistor());

    final DSOChannelManager channelManager = new DSOChannelManagerImpl(this.l1Listener.getChannelManager(), pInfo.version());
    channelManager.addEventListener(this.connectionIdFactory);

    final boolean availableMode = voteCount < 0;
    final WeightGeneratorFactory weightGeneratorFactory = new WeightGeneratorFactory();
    // At this point, we can create the weight generator factory we will use for elections and other inter-server consensus decisions.
    // Generators to produce:
    // 1)  ConsistencyWeightGenerator - needs the ConsistencyManagerImpl if being used.
    final ConsistencyManagerWeightGenerator consistency = new ConsistencyManagerWeightGenerator(()->l2Coordinator.getStateManager(), availableMode);
    weightGeneratorFactory.add(consistency);
    // 1.5) ConsistencyBlockingTimeWeightGenerator - obsolete weight generator kept for compatibility.
    final BlockTimeWeightGenerator blocking = new BlockTimeWeightGenerator();
    weightGeneratorFactory.add(blocking);
    // 2)  ChannelWeightGenerator - needs the DSOChannelManager.
    final ChannelWeightGenerator connectedClientCountWeightGenerator = new ChannelWeightGenerator(()->l2Coordinator.getStateManager(), channelManager, availableMode);
    weightGeneratorFactory.add(connectedClientCountWeightGenerator);
    // 3)  ConnectionIDWeightGenerator - How many connection ids have been created.  Greater wins
    final ConnectionIDWeightGenerator connectionsMade = new ConnectionIDWeightGenerator(connectionIdFactory);
    weightGeneratorFactory.add(connectionsMade);
    // 4)  InitialStateWeightGenerator - If it gets down to here, give some weight to a persistent server that went down as active
    final InitialStateWeightGenerator initialState = new InitialStateWeightGenerator(persistor.getClusterStatePersistor());
    weightGeneratorFactory.add(initialState);
    // 5)  Topology weight is the number nodes this stripe believes are in the cluster
    final TopologyWeightGenerator topoWeight = new TopologyWeightGenerator(this.configSetupManager.getConfiguration());
    weightGeneratorFactory.add(topoWeight);
    // 6)  SequenceID weight is the number of replication activities handled by this passive server
    final SequenceIDWeightGenerator sequenceWeight = new SequenceIDWeightGenerator();
    weightGeneratorFactory.add(sequenceWeight);
    // 7)  ServerUptimeWeightGenerator.
    final ServerUptimeWeightGenerator serverUptimeWeightGenerator = new ServerUptimeWeightGenerator(availableMode);
    weightGeneratorFactory.add(serverUptimeWeightGenerator);
    // 8)  RandomWeightGenerator.
    final RandomWeightGenerator randomWeightGenerator = new RandomWeightGenerator(new SecureRandom(), availableMode);
    weightGeneratorFactory.add(randomWeightGenerator);
    // 9)  ConsistencyGenerationGeneration.  (not currently used, only for information sharing)
    final GenerationWeightGenerator generationWeightGenerator = new GenerationWeightGenerator(consistencyMgr);
    weightGeneratorFactory.add(generationWeightGenerator);
    // -We can now install the generator as it is built.

    this.globalWeightGeneratorFactory = weightGeneratorFactory.complete();


    final ChannelStatsImpl channelStats = new ChannelStatsImpl(sampledCounterManager, channelManager);
    channelManager.addEventListener(channelStats);

    // Note that the monitoring service interface can be null if there is no monitoring support loaded into the server.
    IMonitoringProducer serviceInterface = null;

    try {
      serviceInterface = platformServiceRegistry.getService(new BasicServiceConfiguration<>(IMonitoringProducer.class));
    } catch (ServiceException multi) {
      Assert.fail("Multiple IMonitoringProducer implementations found!");
    }

    boolean USE_DIRECT = !tcProperties.getBoolean(TCPropertiesConsts.L2_SEDA_STAGE_DISABLE_DIRECT_SINKS, false);
    if (!USE_DIRECT) {
      logger.info("disabling the use for direct sinks");
    }
    RequestProcessor processor = new RequestProcessor(stageManager, maxStageSize, USE_DIRECT);

    ManagementTopologyEventCollector eventCollector = new ManagementTopologyEventCollector(serviceInterface);
    ClientEntityStateManager clientEntityStateManager = new ClientEntityStateManagerImpl();

    entityManager = new EntityManagerImpl(this.serviceRegistry, clientEntityStateManager, eventCollector, processor, this::flushLocalPipeline, this.configSetupManager.getServiceLocator());
    // We need to set up a stage to point at the ProcessTransactionHandler and we also need to register it for events, below.
    final ProcessTransactionHandler processTransactionHandler = new ProcessTransactionHandler(this.persistor, channelManager, entityManager);
    stageManager.createStage(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE, VoltronEntityMessage.class, processTransactionHandler.getVoltronMessageHandler(), 1, fastStageSize, USE_DIRECT, true).setSpinningCount(1000);
    stageManager.createStage(ServerConfigurationContext.RESPOND_TO_REQUEST_STAGE, ResponseMessage.class, processTransactionHandler.getMultiResponseSender(), L2Utils.getOptimalCommWorkerThreads(), maxStageSize, false, true);
//  add the server -> client communicator service
    final CommunicatorService communicatorService = new CommunicatorService(processTransactionHandler.getClientMessageSender());
    channelManager.addEventListener(communicatorService);
    communicatorService.initialized();
    serviceRegistry.registerImplementationProvided(communicatorService);

    VoltronMessageHandler voltron = new VoltronMessageHandler(channelManager, USE_DIRECT);
    // We need to connect the IInterEntityMessengerProvider to the voltronMessageSink.

    Stage<VoltronEntityMessage> fast = stageManager.createStage(ServerConfigurationContext.SINGLE_THREADED_FAST_PATH, VoltronEntityMessage.class, voltron, 1, maxStageSize);
    messengerProvider.setMessageSink(fast.getSink());
    entityManager.setMessageSink(fast.getSink());

    this.groupCommManager = this.serverBuilder.createGroupCommManager(this.configSetupManager, stageManager,
                                                                      this.thisServerNodeID,
                                                                      this.stripeIDStateManager, this.globalWeightGeneratorFactory,
                                                                      bufferManagerFactory, this.topologyManager);

    if (consistencyMgr instanceof GroupEventsListener) {
      this.groupCommManager.registerForGroupEvents((GroupEventsListener)consistencyMgr);
    }

    final Stage<ClientHandshakeMessage> clientHandshake = stageManager.createStage(ServerConfigurationContext.CLIENT_HANDSHAKE_STAGE, ClientHandshakeMessage.class, createHandShakeHandler(entityManager, processTransactionHandler), 1, maxStageSize);

    Stage<HydrateContext> hydrator = stageManager.createStage(ServerConfigurationContext.HYDRATE_MESSAGE_STAGE, HydrateContext.class, new HydrateHandler(), L2Utils.getOptimalCommWorkerThreads(), maxStageSize);
    Stage<TCMessage> diagStage = stageManager.createStage(ServerConfigurationContext.MONITOR_STAGE, TCMessage.class, new DiagnosticsHandler(this, this.server.getJMX()), 1, 1);

    VoltronMessageSink voltronSink = new VoltronMessageSink(hydrator, fast.getSink(), entityManager);
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, new TCMessageHydrateSink<>(clientHandshake.getSink()));
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_MESSAGE, voltronSink);
    messageRouter.routeMessageType(TCMessageType.DIAGNOSTIC_REQUEST, m -> diagStage.getSink().addToSink(m));

    HASettingsChecker haChecker = new HASettingsChecker(configSetupManager, tcProperties);
    haChecker.validateHealthCheckSettingsForHighAvailability();

    StateManager state = new StateManagerImpl(DistributedObjectServer.consoleLogger, this.groupCommManager,
        createStageController(processTransactionHandler), eventCollector, stageManager,
        configSetupManager.getGroupConfiguration().getMembers().length,
        configSetupManager.getGroupConfiguration().getElectionTimeInSecs(),
        this.globalWeightGeneratorFactory, consistencyMgr,
        this.persistor.getClusterStatePersistor(), this.topologyManager);

    // And the stage for handling their response batching/serialization.
    Stage<Runnable> replicationResponseStage = stageManager.createStage(ServerConfigurationContext.PASSIVE_OUTGOING_RESPONSE_STAGE, Runnable.class,
        new GenericHandler<>(), 1, maxStageSize);
//  routing for passive to receive replication
    ReplicatedTransactionHandler replicatedTransactionHandler = new ReplicatedTransactionHandler(state, replicationResponseStage, this.persistor, entityManager, groupCommManager);
    sequenceWeight.setReplicatedTransactionHandler(replicatedTransactionHandler);
// This requires both the stage for handling the replication/sync messages.
    Stage<ReplicationMessage> replicationStage = stageManager.createStage(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE, ReplicationMessage.class,
        replicatedTransactionHandler.getEventHandler(), 1, maxStageSize);

    final ClientChannelLifeCycleHandler channelLifeCycleHandler = new ClientChannelLifeCycleHandler(this.communicationsManager,
                                                                                        stageManager, channelManager,
                                                                                        clientEntityStateManager,
                                                                                        processTransactionHandler, eventCollector);
    channelManager.addEventListener(channelLifeCycleHandler);
    this.l1Diagnostics.getChannelManager().addEventListener(channelLifeCycleHandler);

    this.l2Coordinator = this.serverBuilder.createL2HACoordinator(consoleLogger, this,
                                                                  state,
                                                                  this.groupCommManager,
                                                                  this.persistor,
                                                                  this.globalWeightGeneratorFactory,
                                                                  this.stripeIDStateManager,
                                                                  consistencyMgr);

    connectServerStateToReplicatedState(monitoringShimService, state, clientEntityStateManager, l2Coordinator.getReplicatedClusterStateManager());
// setup replication
    final Sink<ReplicationSendingAction> replicationSenderStage =
        stageManager.createStage(ServerConfigurationContext.ACTIVE_TO_PASSIVE_DRIVER_STAGE,
                                 ReplicationSendingAction.class, new GenericHandler<>(), max(3, knownPeers), maxStageSize).getSink();
    ReplicationSender replicationSender = new ReplicationSender(replicationSenderStage, groupCommManager);
    final Sink<ReplicationReceivingAction> replicationReceivingStage =
        stageManager.createStage(ServerConfigurationContext.PASSIVE_TO_ACTIVE_DRIVER_STAGE,
                                 ReplicationReceivingAction.class, new GenericHandler<>(), max(3, knownPeers), maxStageSize).getSink();
    final ActiveToPassiveReplication passives = new ActiveToPassiveReplication(consistencyMgr, processTransactionHandler, this.persistor.getEntityPersistor(), replicationSender, replicationReceivingStage, this.getGroupManager());
    processor.setReplication(passives);

    Stage<ReplicationMessageAck> replicationStageAck = stageManager.createStage(ServerConfigurationContext.PASSIVE_REPLICATION_ACK_STAGE, ReplicationMessageAck.class,
      new AbstractEventHandler<ReplicationMessageAck>() {
          @Override
          protected void initialize(ConfigurationContext context) {
            super.initialize(context);
            passives.enterActiveState(state.getPassiveStandbys());
          }

          @Override
          public void handleEvent(ReplicationMessageAck context) throws EventHandlerException {
            switch (context.getType()) {
              case ReplicationMessageAck.BATCH:
                passives.batchAckReceived(context);
                break;
              case ReplicationMessageAck.START_SYNC:
                try {
                  l2Coordinator.getReplicatedClusterStateManager().publishClusterState(context.messageFrom());
                } catch (GroupException ge) {
                  logger.warn("error syncing state", ge);
                }
                passives.startPassiveSync(context.messageFrom());
                break;
              default:
                throw new AssertionError("bad message " + context);
          }
          }
        }, 1, maxStageSize);

//  handle cluster state
    Sink<L2StateMessage> stateMessageSink = stageManager.createStage(ServerConfigurationContext.L2_STATE_MESSAGE_HANDLER_STAGE, L2StateMessage.class, new L2StateMessageHandler(), 1, maxStageSize).getSink();
    this.groupCommManager.routeMessages(L2StateMessage.class, stateMessageSink);
//  handle passives
    GroupEventsDispatchHandler dispatchHandler = new GroupEventsDispatchHandler();
    dispatchHandler.addListener(this.l2Coordinator);
    dispatchHandler.addListener(passives);

    Stage<GroupEvent> groupEvents = stageManager.createStage(ServerConfigurationContext.GROUP_EVENTS_DISPATCH_STAGE, GroupEvent.class, dispatchHandler, 1, maxStageSize);
    this.groupCommManager.registerForGroupEvents(dispatchHandler.createDispatcher(groupEvents.getSink()));
  //  TODO:  These stages should probably be activated and destroyed dynamically
//  Replicated messages need to be ordered
    Sink<ReplicationMessage> replication = new OrderedSink<>(logger, replicationStage.getSink());
    this.groupCommManager.routeMessages(ReplicationMessage.class, replication);

    this.groupCommManager.routeMessages(ReplicationMessageAck.class, replicationStageAck.getSink());
    Sink<PlatformInfoRequest> info = createPlatformInformationStages(stageManager, maxStageSize, monitoringShimService);
    dispatchHandler.addListener(connectPassiveEvents(info, monitoringShimService));

    final ServerClientHandshakeManager clientHandshakeManager = new ServerClientHandshakeManager(
        LoggerFactory.getLogger(ServerClientHandshakeManager.class),
        consistencyMgr,
        channelManager,
        new Timer("Reconnect timer", true),
        () -> l2DSOConfig.getClientReconnectWindow() * 1000L, //need to pass reconnect window as milliseconds
        fast.getSink(),
        pInfo,
        consoleLogger
    );

    this.context = this.serverBuilder.createServerConfigurationContext(configSetupManager.getServerConfiguration().getName(), stageManager, channelManager,
                                                                       channelStats, this.l2Coordinator,
                                                                       clientHandshakeManager,
                                                                       this.connectionIdFactory,
                                                                       maxStageSize);
    this.context.addShutdownItem(passives::close);
    toInit.add(this.serverBuilder);

    startStages(stageManager, toInit);

    // XXX: yucky casts
    this.managementContext = new ServerManagementContext((DSOChannelManagerMBean) channelManager,channelStats,
                                                         connectionPolicy, getOperationGuardian(platformServiceRegistry,
                                                                 channelLifeCycleHandler), voltron, voltronSink);

    final CallbackOnExitHandler handler = new CallbackGroupExceptionHandler(logger, consoleLogger);
    this.threadGroup.addCallbackOnExitExceptionHandler(GroupException.class, handler);
// don't join the group if the configuration is not complete
    if (!configuration.isPartialConfiguration()) {
      startGroupManagers();
      this.l2Coordinator.start();
    } else {
      this.l2Coordinator.getStateManager().moveToDiagnosticMode();
      TCLogging.getConsoleLogger().info("Started the server in diagnostic mode");
    }
  }

  public CompletableFuture<Void> stop() throws Exception {
    return stop(false);
  }

  public CompletableFuture<Void> stop(boolean immediate) throws Exception {
    CompletableFuture<Void> stopped = new CompletableFuture<>();
    if (this.threadGroup.isStoppable() && this.stopping.attemptSet()) {
      CountDownLatch barrier = new CountDownLatch(immediate ? 0 : 1);
      ThreadUtil.executeInThread(threadGroup.getParent(), ()->{
        try {
          if (!barrier.await(60, TimeUnit.SECONDS)) {
            logger.warn("Timeout waiting for clean shutdown.");
          }
          killThreads(stopped, immediate);
        }catch(InterruptedException ie) {
          logger.warn("shutdown thread failed", ie);
        }
      }, "server shutdown thread", true);
      this.l2Coordinator.shutdown();
      this.persistor.shutdown();
      this.connectionManager.shutdown();
      this.groupCommManager.shutdown();
      this.l1Diagnostics.stop(60000);
      this.l1Listener.stop(60000);
      this.context.shutdown();
      this.entityManager.shutdown();
      this.serviceRegistry.shutdown();
      this.timer.cancelAll();
      this.timer.stop();
      this.configSetupManager.close();
      barrier.countDown();
    } else {
      consoleLogger.info("Server Exiting...");
      TCLogbackLogging.resetLogging();
      stopped.complete(null);
    }
    return stopped;
  }

  private void killThreads(CompletableFuture<Void> stopped, boolean immediate) {
    try {
      this.seda.getStageManager().stopAll();
      if (immediate) {
        threadGroup.interrupt();
      } else if (!threadGroup.retire(TimeUnit.SECONDS.toMillis(30L), e->L2Utils.handleInterrupted(logger, e))) {
        consoleLogger.warn("unable to retire server threads");
        threadGroup.printLiveThreads(logger::warn);
        threadGroup.interrupt();
      }
      consoleLogger.info("Server Exiting...");
    } finally {
      TCLogbackLogging.resetLogging();
      stopped.complete(null);
    }
  }

  private ConsistencyManager createConsistencyManager(ServerConfigurationManager configSetupManager,
                                                             int knownPeers,
                                                             int voteCount) {
    // start the server in diagnostic mode if the configuration is not complete
    if (configSetupManager.isPartialConfiguration()) {
      if (knownPeers != 0) {
        throw new RuntimeException("Diagnostic mode is not supported with multi-server stripe");
      }
      return new DiagnosticModeConsistencyManager();
    }

    boolean consistentStartup = knownPeers > 0 && (configSetupManager.getConfiguration().isConsistentStartup() || voteCount >= 0);
    return new SafeStartupManagerImpl(
        consistentStartup,
        knownPeers,
        new ConsistencyManagerImpl(this.topologyManager)
    );
  }

  private Guardian getOperationGuardian(ServiceRegistry platformRegistry, ClientChannelLifeCycleHandler handler) {
    Collection<Guardian> userProvided = platformRegistry.getServices(()->Guardian.class);

    return (o, p)->{
      try {
        return userProvided.stream().map(g->g.validate(o, p)).reduce(Boolean.TRUE, Boolean::logicalAnd);
      } catch (Throwable t) {
        logger.warn("guardian failed", t);
        return true;
      }
    };
  }

  private BufferManagerFactory getBufferManagerFactory(ServiceRegistry platformRegistry) {
    BufferManagerFactory bufferManagerFactory = null;
    try {
      bufferManagerFactory = platformRegistry.getService(new BasicServiceConfiguration<>(BufferManagerFactory.class));
    } catch (ServiceException e) {
      Assert.fail("Multiple BufferManagerFactory implementations found!");
    }
    if (bufferManagerFactory == null) {
      bufferManagerFactory = new ClearTextBufferManagerFactory();
    }
    return bufferManagerFactory;
  }

  private NetworkListener createDiagnosticsListener(InetSocketAddress address, ConnectionIDFactory idFactoryForInfoConnections) throws UnknownHostException {
    boolean enabled = tcProperties.getBoolean(TCPropertiesConsts.L2_L1REDIRECT_ENABLED, true);
    NetworkTranslator translator = null;
    try {
      translator = this.serviceRegistry.subRegistry(0).getService(new BasicServiceConfiguration<>(NetworkTranslator.class));
    } catch (ServiceException se) {
      logger.warn("error getting printer for cluster state", se);
    }
    NetworkTranslator finalTranslator = translator == null ? (src,redirect)->redirect : translator;
    return this.communicationsManager.createListener(address, true, idFactoryForInfoConnections, (final InetSocketAddress srcOfRequest) -> {
      StateManager stateMgr = l2Coordinator.getStateManager();
      // only provide an active name if this server is not active
      ServerID server1 = !stateMgr.isActiveCoordinator() ? (ServerID)stateMgr.getActiveNodeID() : ServerID.NULL_ID;
      if (enabled && !server1.isNull()) {
        return finalTranslator.redirectTo(srcOfRequest, server1.getName());
      }
      return null;
    });
  }

  private Sink<PlatformInfoRequest> createPlatformInformationStages(StageManager stageManager, int maxStageSize, LocalMonitoringProducer monitoringSupport) {
    Stage<PlatformInfoRequest> stage = stageManager.createStage(ServerConfigurationContext.PLATFORM_INFORMATION_REQUEST,
        PlatformInfoRequest.class, new PlatformInfoRequestHandler(groupCommManager, monitoringSupport).getEventHandler(), 1, maxStageSize);
    groupCommManager.routeMessages(PlatformInfoRequest.class, stage.getSink());
//  publish state change events to everyone in the stripe
    return stage.getSink();
  }

  private void startStages(StageManager stageManager, List<PostInit> toInit) {
//  exclude from startup specific stages that are controlled by the stage controller.
    // NOTE:  PASSIVE_OUTGOING_RESPONSE_STAGE must be active whenever PASSIVE_REPLICATION_STAGE is.
    stageManager.startAll(this.context, toInit,
        ServerConfigurationContext.SINGLE_THREADED_FAST_PATH,
        ServerConfigurationContext.REQUEST_PROCESSOR_DURING_SYNC_STAGE,
        ServerConfigurationContext.HYDRATE_MESSAGE_STAGE,
        ServerConfigurationContext.VOLTRON_MESSAGE_STAGE,
        ServerConfigurationContext.RESPOND_TO_REQUEST_STAGE,
        ServerConfigurationContext.ACTIVE_TO_PASSIVE_DRIVER_STAGE,
        ServerConfigurationContext.PASSIVE_REPLICATION_STAGE,
        ServerConfigurationContext.PASSIVE_OUTGOING_RESPONSE_STAGE,
        ServerConfigurationContext.PASSIVE_REPLICATION_ACK_STAGE
    );
  }

  private void flushLocalPipeline(EntityID eid, FetchID fetch, ServerEntityAction action) {
    switch(action) {
      case CREATE_ENTITY:
      case DESTROY_ENTITY:
      case FETCH_ENTITY:
      case RECONFIGURE_ENTITY:
      case RELEASE_ENTITY:
        logger.info("completed lifecycle " + action + " on " + eid + ":" +fetch);
        break;
      case FAILOVER_FLUSH:
        //  this is a failover flush, nothing more is needed
        return;
      default:
      //  not lifecycle, ignore
        logger.debug("completed mgmt " + action + " on " + eid);
    }
    boolean forDestroy = (action == ServerEntityAction.DESTROY_ENTITY);
    if (!this.l2Coordinator.getStateManager().isActiveCoordinator()) {
      try {
        this.seda.getStageManager()
            .getStage(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE, ReplicationMessage.class)
            .getSink().addToSink(ReplicationMessage.createLocalContainer(SyncReplicationActivity.createFlushLocalPipelineMessage(fetch, (action.isReplicated()) ? action.replicationType() : FLUSH_LOCAL_PIPELINE)));
        return;
      } catch (IllegalStateException state) {
//  ignore, could have transitioned to active before message got added
      }
    }
//  must be active, noop the ProcessTransactionHandler

    this.seda.getStageManager()
        .getStage(ServerConfigurationContext.SINGLE_THREADED_FAST_PATH, VoltronEntityMessage.class)
        .getSink().addToSink(new LocalPipelineFlushMessage(EntityDescriptor.createDescriptorForInvoke(fetch, ClientInstanceID.NULL_ID), forDestroy));
  }

  private StageController createStageController(ProcessTransactionHandler pth) {
    StageController control = new StageController(this::getContext);
//  PASSIVE-UNINITIALIZED handle replicate messages right away.
    // NOTE:  PASSIVE_OUTGOING_RESPONSE_STAGE must be active whenever PASSIVE_REPLICATION_STAGE is.
    control.addStageToState(ServerMode.UNINITIALIZED.getState(), ServerConfigurationContext.PASSIVE_REPLICATION_STAGE);
    control.addStageToState(ServerMode.UNINITIALIZED.getState(), ServerConfigurationContext.PASSIVE_OUTGOING_RESPONSE_STAGE);
//  REPLICATION needs to continue in STANDBY so include that stage here.  SYNC also needs to be handled.
    control.addStageToState(ServerMode.SYNCING.getState(), ServerConfigurationContext.PASSIVE_REPLICATION_STAGE);
    control.addStageToState(ServerMode.SYNCING.getState(), ServerConfigurationContext.PASSIVE_OUTGOING_RESPONSE_STAGE);
//  REPLICATION needs to continue in STANDBY so include that stage here. SYNC goes away
    control.addStageToState(ServerMode.PASSIVE.getState(), ServerConfigurationContext.PASSIVE_REPLICATION_STAGE);
    control.addStageToState(ServerMode.PASSIVE.getState(), ServerConfigurationContext.PASSIVE_OUTGOING_RESPONSE_STAGE);
//  turn on the process transaction handler, the active to passive driver, and the replication ack handler, replication handler needs to be shutdown and empty for
//  active to start
    control.addStageToState(ServerMode.ACTIVE.getState(), ServerConfigurationContext.SINGLE_THREADED_FAST_PATH);
    control.addStageToState(ServerMode.ACTIVE.getState(), ServerConfigurationContext.REQUEST_PROCESSOR_DURING_SYNC_STAGE);
    control.addStageToState(ServerMode.ACTIVE.getState(), ServerConfigurationContext.HYDRATE_MESSAGE_STAGE);
    control.addStageToState(ServerMode.ACTIVE.getState(), ServerConfigurationContext.VOLTRON_MESSAGE_STAGE);
    control.addStageToState(ServerMode.ACTIVE.getState(), ServerConfigurationContext.RESPOND_TO_REQUEST_STAGE);
    control.addTriggerToState(ServerMode.ACTIVE.getState(),s->{
      //  this is shimmed in to add permanent entities or load existing entities before replication and clients are active
      //  but after the active machinery is up and running
      startActiveMode(pth, StateManager.convert(s) == ServerMode.PASSIVE);
      server.updateActivateTime();
    });
    // these need to be started after startActiveMode is called since replication is only active after permanement
    // entities are create or existing entities are reloaded
    control.addStageToState(ServerMode.ACTIVE.getState(), ServerConfigurationContext.PASSIVE_REPLICATION_ACK_STAGE);
    control.addStageToState(ServerMode.ACTIVE.getState(), ServerConfigurationContext.ACTIVE_TO_PASSIVE_DRIVER_STAGE);
    return control;
  }

  private GroupEventsListener connectPassiveEvents(Sink<PlatformInfoRequest> infoHandler, LocalMonitoringProducer monitoringShimService) {
    return new GroupEventsListener() {

      @Override
      public void nodeJoined(NodeID nodeID) {
        if (l2Coordinator.getStateManager().isActiveCoordinator()) {
          // Note that this passive may have joined in the time between when we decided to enter the active state and
          // when we ran the event to initialize LocalMonitoringProducer to receive events, as an active.
          // In those cases, we should avoid sending the request to this passive as we will send it to all of them, when
          // that happens.
          if (monitoringShimService.isReadyToReceiveRemoteEvents()) {
            PlatformInfoRequest req = PlatformInfoRequest.createEmptyRequest();
            try {
              groupCommManager.sendTo(nodeID, req);
   // monitor will be updated when the remote server responds with it's info
            } catch (GroupException g) {
              // This is unexpected but the rest of the system should be able to recover so just log it.
              logger.error("Failed to send PlatformInfoRequest to new passive", g);
            }
          } else {
            logger.warn("Deferring PlatformInfoRequest to new passive: " + nodeID);
          }
        }
      }

      @Override
      public void nodeLeft(NodeID nodeID) {
        if (l2Coordinator.getStateManager().isActiveCoordinator()) {
          PlatformInfoRequest fake = PlatformInfoRequest.createServerInfoRemoveMessage((ServerID)nodeID);
          fake.setMessageOrginator((ServerID)nodeID);
          infoHandler.addToSink(fake);
        }
      }
    };
  }

  private void connectServerStateToReplicatedState(LocalMonitoringProducer monitoringShimService, StateManager mgr, ClientEntityStateManager clients, ReplicatedClusterStateManager rcs) {
    mgr.registerForStateChangeEvents(new StateChangeListener() {
      private boolean diagnosticsStarted = false;
      
      @Override
      public void l2StateChanged(StateChangedEvent sce) {
        rcs.setCurrentState(sce.getCurrentState());
        if (sce.movedToActive()) {
    // transition the local monitoring producer to active so the tree is rebuilt as a new active of the stripe
          monitoringShimService.serverIsActive();
          PlatformInfoRequest req = PlatformInfoRequest.createEmptyRequest();
      //  due to the broadcast nature of this call, it is possible to get multiple
      //  responses from the same server.  The underlying collector must tolerate this
          groupCommManager.sendAll(req);  //  request info from all the other servers
          final Set<ClientID> existingClients = new HashSet<>(persistor.getClientStatePersistor().loadAllClientIDs());
//  must do this because the replicated state when it comes to clients, may not include all the references
//  to clients that were in the midst of cleaning up after disconnection.
          existingClients.addAll(clients.clearClientReferences());
          Set<ConnectionID> existingConnections = existingClients.stream()
              .map(cid->new ConnectionID(ConnectionID.NULL_JVM_ID, cid.toLong(), stripeIDStateManager.getStripeID().getName())).collect(Collectors.toSet());
          getContext().getClientHandshakeManager().setStarting(existingClients);
          l2Coordinator.getReplicatedClusterStateManager().goActiveAndSyncState();
          startL1Listener(existingConnections);
        } else if (!diagnosticsStarted && StateManager.convert(sce.getCurrentState()) != ServerMode.STOP) {
          startDiagnosticListener();
          diagnosticsStarted = true;
        }
      }
    });
  }

  public void startGroupManagers() {
    try {
        final NodeID myNodeId = this.groupCommManager.join(this.configSetupManager.getGroupConfiguration());
        logger.info("This L2 Node ID = " + myNodeId);
    } catch (final GroupException e) {
      logger.error("Caught Exception :", e);
      throw new RuntimeException(e);
    }
  }

  private HashMap<TCMessageType, Class<? extends TCMessage>> getMessageTypeClassMappings() {
    HashMap<TCMessageType, Class<? extends TCMessage>> messageTypeClassMapping = new HashMap<>();
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

  protected Logger getLogger() {
    return logger;
  }

  private ServerID makeServerNodeID(ServerConfiguration l2DSOConfig) {
    String host = l2DSOConfig.getTsaPort().getHostName();
    if (TCSocketAddress.isWildcardAddress(host)) {
      host = l2DSOConfig.getHost();
    }
    final ServerID aNodeID = new ServerID(TCSocketAddress.getStringForm(InetSocketAddress.createUnresolved(host, l2DSOConfig.getTsaPort().getPort())), UUID.getUUID().toString().getBytes());
    logger.info("Creating server nodeID: " + aNodeID);
    return aNodeID;
  }

  public ServerID getServerNodeID() {
    return this.thisServerNodeID;
  }

  private void startActiveMode(ProcessTransactionHandler pth, boolean wasStandby) {
    if (!wasStandby) {
      if (persistor.getClusterStatePersistor().getInitialState() == null) {
  //  no reconnects on a new server
        pth.reconnectComplete();
        Sink<VoltronEntityMessage> msgSink = this.seda.getStageManager().getStage(ServerConfigurationContext.SINGLE_THREADED_FAST_PATH, VoltronEntityMessage.class).getSink();
        Map<EntityID, VoltronEntityMessage> checkdups = new HashMap<>();
  //  find annotated permanent entities
        List<VoltronEntityMessage> annotated = entityManager.getEntityLoader().getAnnotatedEntities();
        for (VoltronEntityMessage vem : annotated) {
  //  map them to weed out duplicates
          checkdups.put(vem.getEntityDescriptor().getEntityID(), vem);
        }
        for (VoltronEntityMessage vem : checkdups.values()) {
          msgSink.addToSink(vem);
        }
        EntityPersistor ep = this.persistor.getEntityPersistor();
        for (VoltronEntityMessage vem : checkdups.values()) {
          try {
            ep.waitForPermanentEntityCreation(vem.getEntityDescriptor().getEntityID());
          } catch (RuntimeException e) {
            this.persistor.getClusterStatePersistor().setDBClean(false);
            throw new TCServerRestartException("error creating permanent entities", e);
          } catch (Exception e) {
            this.persistor.getClusterStatePersistor().setDBClean(false);
            throw new TCServerRestartException("error creating permanent entities", e);
          }
        }
      } else {
        pth.loadExistingEntities();
      }
    }
  }

  public void startL1Listener(Set<ConnectionID> existingConnections) {
    while (!server.isStopped()) {
      try {
        this.l1Diagnostics.stop(1000L);
      } catch (TCTimeoutException to) {
        logger.warn("unable to stop diagnostics listener");
        continue;
      }

      try {
        this.l1Listener.start(existingConnections);
        break;
      } catch (BindException bind) {
  // this seems to happen on windows but should not.  we just gave up the port.
  // loop forever as a hack.
        logger.warn("client server port not available for binding:", bind);
        try {
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ie) {
          L2Utils.handleInterrupted(logger, ie);
          throw new RuntimeException(bind);
        }
      } catch (IOException ioe) {
        if (!stopping.isSet()) {
          consoleLogger.info("Unable to start Terracotta Server instance diagnostic listening on {}", this.l1Diagnostics, ioe);
          throw new RuntimeException(ioe);
        } else {
          logger.debug("cannot start listeners, server shutting down");
        }
      }
    }
    consoleLogger.info("Terracotta Server instance has started up as ACTIVE node on {}", this.l1Listener
                       + " successfully, and is now ready for work.");
  }

  public void startDiagnosticListener() {
    try {
      this.l1Diagnostics.start(Collections.emptySet());
      consoleLogger.info("Terracotta Server instance has started diagnostic listening on  {}", this.l1Diagnostics);
    } catch (IOException ioe) {
      if (!stopping.isSet()) {
        consoleLogger.info("Unable to start Terracotta Server instance diagnostic listening on {}", this.l1Diagnostics, ioe);
        throw new RuntimeException(ioe);
      } else {
        logger.debug("cannot start listeners, server shutting down");
      }
    }
  }

  /**
   * Since this is accessed via JMX and l1Listener isn't initialed when a secondary is waiting on the lock file, use the
   * config value unless the special value 0 is specified for use in the tests to get a random port.
   * @return
   */
  public int getListenPort() {
    final ServerConfiguration l2DSOConfig = this.configSetupManager.getServerConfiguration();
    final int configValue = l2DSOConfig.getTsaPort().getPort();
    if (configValue != 0) { return configValue; }
    if (this.l1Diagnostics != null) {
      try {
        return this.l1Diagnostics.getBindPort();
      } catch (final IllegalStateException ise) {/**/
      }
    }
    if (this.l1Listener != null) {
      try {
        return this.l1Listener.getBindPort();
      } catch (final IllegalStateException ise) {/**/
      }
    }
    return -1;
  }

  public int getGroupPort() {
    final ServerConfiguration l2DSOConfig = this.configSetupManager.getServerConfiguration();
    final int configValue = l2DSOConfig.getGroupPort().getPort();
    if (configValue != 0) { return configValue; }
    return -1;
  }

  public ConnectionIDFactory getConnectionIdFactory() {
    return this.connectionIdFactory;
  }

  public ServerConfigurationContext getContext() {
    return this.context;
  }

  public ServerManagementContext getManagementContext() {
    return this.managementContext;
  }

  public GroupManager<AbstractGroupMessage> getGroupManager() {
    return this.groupCommManager;
  }

  public ServerConfigurationManager getConfigSetupManager() {
    return configSetupManager;
  }

  protected ClientHandshakeHandler createHandShakeHandler(EntityManager entities, ProcessTransactionHandler processTransactionHandler) {
    return new ClientHandshakeHandler(entities, processTransactionHandler);
  }

  // for tests only
  public CommunicationsManager getCommunicationsManager() {
    return communicationsManager;
  }

  public Persistor getPersistor() {
    return persistor;
  }
}
