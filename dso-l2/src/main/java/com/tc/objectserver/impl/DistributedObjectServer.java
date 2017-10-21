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
import com.tc.async.api.EventHandlerException;

import com.tc.logging.TCLogging;
import com.tc.objectserver.api.EntityManager;
import com.tc.services.MappedStateCollector;
import com.tc.services.PlatformConfigurationImpl;
import com.tc.services.PlatformServiceProvider;
import com.tc.services.SingleThreadedTimer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
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
import com.tc.config.HaConfig;
import com.tc.config.HaConfigImpl;
import com.tc.config.NodesStore;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.entity.DiagnosticMessageImpl;
import com.tc.entity.DiagnosticResponseImpl;
import com.tc.entity.NetworkVoltronEntityMessageImpl;
import com.tc.entity.ServerEntityMessageImpl;
import com.tc.entity.ServerEntityResponseMessage;
import com.tc.entity.ServerEntityResponseMessageImpl;
import com.tc.entity.VoltronEntityAppliedResponseImpl;
import com.tc.entity.VoltronEntityMessage;
import com.tc.entity.VoltronEntityMultiResponseImpl;
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
import com.tc.l2.ha.ChannelWeightGenerator;
import com.tc.l2.ha.ConnectionIDWeightGenerator;
import com.tc.l2.ha.HASettingsChecker;
import com.tc.l2.ha.InitialStateWeightGenerator;
import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.l2.ha.ServerUptimeWeightGenerator;
import com.tc.l2.ha.StripeIDStateManagerImpl;
import com.tc.l2.ha.TransactionCountWeightGenerator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.handler.GroupEvent;
import com.tc.l2.handler.GroupEventsDispatchHandler;
import com.tc.l2.handler.L2StateChangeHandler;
import com.tc.l2.handler.L2StateMessageHandler;
import com.tc.l2.handler.PlatformInfoRequestHandler;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.msg.PlatformInfoRequest;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.ThreadDumpHandler;
import com.tc.management.TerracottaManagement;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCDumper;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.net.AddressChecker;
import com.tc.net.ClientID;
import com.tc.net.NIOWorkarounds;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.Node;
import com.tc.net.protocol.HttpConnectionContext;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageHydrateSink;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
import com.tc.net.protocol.transport.TransportHandshakeErrorNullHandler;
import com.tc.net.utils.L2Utils;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.config.schema.L2Config;
import com.tc.object.msg.ClientHandshakeAckMessageImpl;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.msg.ClientHandshakeRefusedMessageImpl;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerImpl;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.core.api.GlobalServerStatsImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.entity.ActiveToPassiveReplication;
import com.tc.objectserver.handler.ChannelLifeCycleHandler;
import com.tc.objectserver.handler.ClientHandshakeHandler;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.persistence.ClientStatePersistor;
import com.tc.objectserver.persistence.Persistor;
import com.tc.objectserver.persistence.NullPlatformStorageServiceProvider;
import com.tc.objectserver.persistence.NullPlatformStorageProviderConfiguration;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.ServerConnectionValidator;
import com.tc.server.TCServer;
import com.tc.server.TCServerMain;
import com.tc.services.CommunicatorService;
import com.tc.services.EntityMessengerProvider;
import com.tc.services.LocalMonitoringProducer;
import com.tc.services.TerracottaServiceProviderRegistryImpl;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.text.PrettyPrintable;
import com.tc.util.Assert;
import com.tc.util.CommonShutDownHook;
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
import com.tc.objectserver.entity.RequestProcessorHandler;
import com.tc.objectserver.entity.VoltronMessageSink;
import com.tc.objectserver.handler.CommunicatorResponseHandler;
import com.tc.objectserver.handler.GenericHandler;
import com.tc.objectserver.handler.ReplicatedTransactionHandler;
import com.tc.objectserver.handler.VoltronMessageHandler;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.text.MapListPrettyPrint;
import com.tc.util.ProductCapabilities;
import com.tc.text.PrettyPrinter;
import com.tc.util.ProductID;
import java.lang.management.ManagementFactory;
import java.net.BindException;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.BasicServiceConfiguration;


/**
 * Startup and shutdown point. Builds and starts the server
 */
public class DistributedObjectServer implements TCDumper, ServerConnectionValidator {
  private final ConnectionPolicy                 connectionPolicy;
  private final TCServer                         server;
  private final ServerBuilder                    serverBuilder;
  protected final L2ConfigurationSetupManager    configSetupManager;
  protected final HaConfigImpl                   haConfig;

  private static final Logger logger = LoggerFactory.getLogger(DistributedObjectServer.class);
  private static final Logger consoleLogger = TCLogging.getConsoleLogger();

  private ServerID                               thisServerNodeID = ServerID.NULL_ID;
  protected NetworkListener                      l1Listener;
  protected NetworkListener                      l1Diagnostics;
  private CommunicationsManager                  communicationsManager;
  private ServerConfigurationContext             context;
  private CounterManager                         sampledCounterManager;
  private ServerManagementContext                managementContext;
  private Persistor                              persistor;

  private L2Coordinator                          l2Coordinator;

  private TCProperties                           tcProperties;

  private ConnectionIDFactoryImpl                connectionIdFactory;

  private final TCThreadGroup                    threadGroup;
  private final SEDA<HttpConnectionContext>                             seda;

  private ReconnectConfig                        l1ReconnectConfig;

  private GroupManager<AbstractGroupMessage> groupCommManager;
  private StripeIDStateManagerImpl               stripeIDStateManager;

  private final SingleThreadedTimer timer;
  private final TerracottaServiceProviderRegistryImpl serviceRegistry;
  private WeightGeneratorFactory globalWeightGeneratorFactory;
  private EntityManagerImpl entityManager;

  // used by a test
  public DistributedObjectServer(L2ConfigurationSetupManager configSetupManager, TCThreadGroup threadGroup,
                                 ConnectionPolicy connectionPolicy, TCServerInfoMBean tcServerInfoMBean) {
    this(configSetupManager, threadGroup, connectionPolicy, new SEDA<HttpConnectionContext>(threadGroup), null);

  }

  public DistributedObjectServer(L2ConfigurationSetupManager configSetupManager, TCThreadGroup threadGroup,
                                 ConnectionPolicy connectionPolicy,
                                 SEDA<HttpConnectionContext> seda,
                                 TCServer server) {
    // This assertion is here because we want to assume that all threads spawned by the server (including any created in
    // 3rd party libs) inherit their thread group from the current thread . Consider this before removing the assertion.
    // Even in tests, we probably don't want different thread group configurations
    Assert.assertEquals(threadGroup, Thread.currentThread().getThreadGroup());

    this.configSetupManager = configSetupManager;
    this.haConfig = new HaConfigImpl(this.configSetupManager);
    this.connectionPolicy = connectionPolicy;
    this.threadGroup = threadGroup;
    this.seda = seda;
    this.server = server;
    this.serverBuilder = createServerBuilder(this.haConfig, logger, server, configSetupManager.dsoL2Config());
    this.timer = new SingleThreadedTimer(null);
    this.timer.start();
    this.serviceRegistry = new TerracottaServiceProviderRegistryImpl();
  }

  protected ServerBuilder createServerBuilder(HaConfig config, Logger tcLogger, TCServer server,
                                                 L2Config l2dsoConfig) {
    return new StandardServerBuilder(config, tcLogger);
  }

  protected ServerBuilder getServerBuilder() {
    return this.serverBuilder;
  }
  
  public byte[] getClusterState(Charset set) {
    PrettyPrinter pp = null;
    try {
      pp = this.serviceRegistry.subRegistry(0).getService(new BasicServiceConfiguration<>(PrettyPrinter.class));
    } catch (ServiceException se) {
      logger.warn("error getting printer for cluster state", se);
    }
    if (pp == null) {
      pp = new MapListPrettyPrint();
    }
    collectState(this.seda.getStageManager(), pp);
    collectState(this.persistor, pp);
    collectState(this.groupCommManager, pp);
    collectState(this.l2Coordinator, pp);
    collectState(this.entityManager, pp);
    collectState(this.serviceRegistry, pp);
    addExtendedConfigState(pp);
    return pp.toString().getBytes(set);
  }

  private static void collectState(PrettyPrintable prettyPrintable, PrettyPrinter prettyPrinter) {
    try {
      prettyPrintable.prettyPrint(prettyPrinter);
    } catch (Throwable t) {
      prettyPrinter.println("unable to collect cluster state for " + prettyPrintable.getClass().getName() + " : " + t.getLocalizedMessage());
    }
  }

  @Override
  public void dump() {
    TCLogging.getDumpLogger().info(new String(getClusterState(Charset.defaultCharset()), Charset.defaultCharset()));
  }

  private void addExtendedConfigState(PrettyPrinter prettyPrinter) {
    try {
      MappedStateCollector mappedStateCollector = new MappedStateCollector("collector");
      this.configSetupManager.commonl2Config().getBean().addStateTo(mappedStateCollector);
      Map<String, Object> state = new HashMap<>();
      state.put("ExtendedConfigs", mappedStateCollector.getMap());
      prettyPrinter.println(state);
    } catch (Throwable t) {
      prettyPrinter.println("unable to collect cluster state for ExtendedConfigs" + " : " + t.getLocalizedMessage());
    }
  }

  public synchronized void start() throws IOException, LocationNotCreatedException, FileNotCreatedException {

    threadGroup.addCallbackOnExitDefaultHandler(new ThreadDumpHandler());
    threadGroup.addCallbackOnExitDefaultHandler((state) -> dump());
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

    this.thisServerNodeID = makeServerNodeID(this.configSetupManager.dsoL2Config());
    ThisServerNodeId.setThisServerNodeId(thisServerNodeID);


    final List<PostInit> toInit = new ArrayList<>();

    // perform the DSO network config verification
    final L2Config l2DSOConfig = this.configSetupManager.dsoL2Config();
    // verify user input host name, DEV-2293
    final String host = l2DSOConfig.host();
    final InetAddress ip = InetAddress.getByName(host);
    if (!ip.isLoopbackAddress() && (NetworkInterface.getByInetAddress(ip) == null)) {
      final String msg = "Unable to find local network interface for " + host;
      consoleLogger.error(msg);
      logger.error(msg, new TCRuntimeException(msg));
      System.exit(-1);
    }

    String bindAddress = this.configSetupManager.commonl2Config().tsaPort().getBind();
    if (bindAddress == null) {
      // workaround for CDV-584
      bindAddress = TCSocketAddress.WILDCARD_IP;
    }

    final InetAddress jmxBind = InetAddress.getByName(bindAddress);
    final AddressChecker addressChecker = new AddressChecker();
    if (!addressChecker.isLegalBindAddress(jmxBind)) { throw new IOException("Invalid bind address [" + jmxBind
                                                                             + "]. Local addresses are "
                                                                             + addressChecker.getAllLocalAddresses()); }

    NIOWorkarounds.solaris10Workaround();
    this.tcProperties = TCPropertiesImpl.getProperties();
    this.l1ReconnectConfig = new L1ReconnectConfigImpl();
    
    String serverName = this.configSetupManager.dsoL2Config().serverName();
//  this is character replacement for windows platform file names.  This is probably a bogus way to 
//  handle this.  Re-evaluate the way data directories are managed for 5.0 and fix this when a plan is
//  devised
    serverName = serverName.replace('.', '-');
    serverName = serverName.replace(':', '$');

    final int maxStageSize = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY);
    final StageManager stageManager = this.seda.getStageManager();

    this.sampledCounterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, true, 0L);

    // Set up the ServiceRegistry.
    TcConfiguration base = this.configSetupManager.commonl2Config().getBean();
    PlatformConfiguration platformConfiguration = new PlatformConfigurationImpl(this.configSetupManager.getL2Identifier(), base);
    serviceRegistry.initialize(platformConfiguration, base, Thread.currentThread().getContextClassLoader());
    serviceRegistry.registerImplementationProvided(new PlatformServiceProvider(this));

    final EntityMessengerProvider messengerProvider = new EntityMessengerProvider(this.timer);
    this.serviceRegistry.registerImplementationProvided(messengerProvider);
    
    final CommunicatorService communicatorService = new CommunicatorService();
    serviceRegistry.registerImplementationProvided(communicatorService);
    
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
    final int serverPort = l2DSOConfig.tsaPort().getValue();
    final ProductInfo pInfo = ProductInfo.getInstance();
    PlatformServer thisServer = new PlatformServer(server.getL2Identifier(), host, hostAddress, bindAddress, serverPort, l2DSOConfig.tsaGroupPort().getValue(), pInfo.buildVersion(), pInfo.buildID(), TCServerMain.getServer().getStartTime());
    
    final LocalMonitoringProducer monitoringShimService = new LocalMonitoringProducer(this.serviceRegistry, thisServer, this.timer);
    this.serviceRegistry.registerImplementationProvided(monitoringShimService);
    
    // ***** NOTE:  At this point, since we are about to create a subregistry for the platform, the serviceRegistry must be complete!
    
    // The platform gets the reserved consumerID 0.
    long platformConsumerID = 0;
    ServiceRegistry platformServiceRegistry = serviceRegistry.subRegistry(platformConsumerID);
    
    Set<ProductID> capablities = EnumSet.allOf(ProductID.class);
    
    if (serviceRegistry.hasUserProvidedServiceProvider(ProductCapabilities.class)) {
      try {
        capablities = platformServiceRegistry.getService(new BasicServiceConfiguration<>(ProductCapabilities.class)).supportedClients();
      } catch (ServiceException s) {
        logger.warn("multiple service providers for " + ProductCapabilities.class.getName());
      }
    }

    persistor = serverBuilder.createPersistor(platformServiceRegistry);

    while(!persistor.start(capablities.contains(ProductID.PERMANENT))) {
      // make sure peristor is not using any storage service
      persistor.close();
      // Log that that the state was not clean so we are going to clear all service provider state.
      logger.warn("DB state not clean!  Clearing all ServiceProvider state (ZAP request)");
      serviceRegistry.clearServiceProvidersState();
      // create the persistor once again as underlying storage service might have cleared its internal state
      persistor = serverBuilder.createPersistor(platformServiceRegistry);
    }

    new ServerPersistenceVersionChecker(persistor.getClusterStatePersistor()).checkAndSetVersion();

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
    final int stageWorkerThreadCount = L2Utils.getOptimalStageWorkerThreads();

    final NetworkStackHarnessFactory networkStackHarnessFactory;
    final boolean useOOOLayer = this.l1ReconnectConfig.getReconnectEnabled();
    if (useOOOLayer) {
      networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                     new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                     this.l1ReconnectConfig);
    } else {
      networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    }

    final MessageMonitor mm = MessageMonitorImpl.createMonitor(TCPropertiesImpl.getProperties(), logger);

    final TCMessageRouter messageRouter = new TCMessageRouterImpl();
    
    TCSecurityManager mgr = new PluggableSecurityManager(this.serviceRegistry.subRegistry(platformConsumerID));
    
    this.communicationsManager = new CommunicationsManagerImpl(CommunicationsManager.COMMSMGR_SERVER, mm,
                                                               messageRouter, networkStackHarnessFactory,
                                                               this.connectionPolicy, commWorkerThreadCount,
                                                               new HealthCheckerConfigImpl(tcProperties
                                                                   .getPropertiesFor(TCPropertiesConsts.L2_L1_HEALTH_CHECK_CATEGORY), "TSA Server"),
                                                               this.thisServerNodeID,
                                                               new TransportHandshakeErrorNullHandler(),
                                                               getMessageTypeClassMappings(), Collections.emptyMap(),
                                                               mgr);


    final SampledCumulativeCounterConfig sampledCumulativeCounterConfig = new SampledCumulativeCounterConfig(1, 300,
                                                                                                             true, 0L);

    ClientStatePersistor clientStateStore = this.persistor.getClientStatePersistor();
    this.connectionIdFactory = new ConnectionIDFactoryImpl(clientStateStore, capablities);

    final String dsoBind = l2DSOConfig.tsaPort().getBind();
    this.l1Listener = this.communicationsManager.createListener(new TCSocketAddress(dsoBind, serverPort), true,
                                                                this.connectionIdFactory);
    
    this.l1Diagnostics = this.communicationsManager.createListener(new TCSocketAddress(dsoBind, serverPort), true, () -> {
      StateManager stateMgr = l2Coordinator.getStateManager();
      // only provide an active name if this server is not active
      ServerID server1 = !stateMgr.isActiveCoordinator() ? (ServerID)stateMgr.getActiveNodeID() : ServerID.NULL_ID;
      if (!server1.isNull()) {
        return server1.getName();
      }
      return null;
    });
    
    this.stripeIDStateManager = new StripeIDStateManagerImpl(this.persistor.getClusterStatePersistor());

    final DSOChannelManager channelManager = new DSOChannelManagerImpl(this.l1Listener.getChannelManager(),
                                                                       this.communicationsManager
                                                                           .getConnectionManager(), pInfo.version());
    channelManager.addEventListener(this.connectionIdFactory);

    final WeightGeneratorFactory weightGeneratorFactory = new WeightGeneratorFactory();
    // At this point, we can create the weight generator factory we will use for elections and other inter-server consensus decisions.
    // Generators to produce:
    // 1)  TransactionCountWeightGenerator - needs the TransactionOrderPersistor.
    final TransactionCountWeightGenerator transactionCountWeightGenerator = new TransactionCountWeightGenerator(this.persistor.getTransactionOrderPersistor());
    weightGeneratorFactory.add(transactionCountWeightGenerator);
    // 2)  ChannelWeightGenerator - needs the DSOChannelManager.
    final ChannelWeightGenerator connectedClientCountWeightGenerator = new ChannelWeightGenerator(()->l2Coordinator.getStateManager(), channelManager);
    weightGeneratorFactory.add(connectedClientCountWeightGenerator);
    // 3)  ConnectionIDWeightGenerator - How many connection ids have been created.  Greater wins
    final ConnectionIDWeightGenerator connectionsMade = new ConnectionIDWeightGenerator(connectionIdFactory);
    weightGeneratorFactory.add(connectionsMade);
    // 4)  InitialStateWeightGenerator - If it gets down to here, give some weight to a persistent server that went down as active
    final InitialStateWeightGenerator initialState = new InitialStateWeightGenerator(persistor.getClusterStatePersistor());
    weightGeneratorFactory.add(initialState);
    // 5)  ServerUptimeWeightGenerator.
    final ServerUptimeWeightGenerator serverUptimeWeightGenerator = new ServerUptimeWeightGenerator();
    weightGeneratorFactory.add(serverUptimeWeightGenerator);
    // 6)  RandomWeightGenerator.
    final RandomWeightGenerator randomWeightGenerator = new RandomWeightGenerator(new SecureRandom());
    weightGeneratorFactory.add(randomWeightGenerator);
    // -We can now install the generator as it is built.
    this.globalWeightGeneratorFactory = weightGeneratorFactory;
    

    final ChannelStatsImpl channelStats = new ChannelStatsImpl(sampledCounterManager, channelManager);
    channelManager.addEventListener(channelStats);

    // Attach the communicator service to the channel manager.
    communicatorService.setChannelManager(channelManager);
    final Stage<ServerEntityResponseMessage> communicatorResponseStage = stageManager.createStage(ServerConfigurationContext.SERVER_ENTITY_MESSAGE_RESPONSE_STAGE, ServerEntityResponseMessage.class,  new CommunicatorResponseHandler(communicatorService), 1, maxStageSize);

    final ObjectInstanceMonitorImpl instanceMonitor = new ObjectInstanceMonitorImpl();

    final SampledCounter globalTxnCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);

    // DEV-8737. Count map mutation operations
    final SampledCounter globalOperationCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);

    final SampledCounter broadcastCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);

    final SampledCounter globalObjectFaultCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final SampledRateCounterConfig sampledRateCounterConfig = new SampledRateCounterConfig(1, 300, true);
    final SampledRateCounter changesPerBroadcast = (SampledRateCounter) this.sampledCounterManager
        .createCounter(sampledRateCounterConfig);
    final SampledRateCounter transactionSizeCounter = (SampledRateCounter) this.sampledCounterManager
        .createCounter(sampledRateCounterConfig);
    final SampledCumulativeCounter globalServerMapGetSizeRequestsCounter = (SampledCumulativeCounter) this.sampledCounterManager
        .createCounter(sampledCumulativeCounterConfig);
    final SampledCumulativeCounter globalServerMapGetValueRequestsCounter = (SampledCumulativeCounter) this.sampledCounterManager
        .createCounter(sampledCumulativeCounterConfig);
    final SampledCumulativeCounter globalServerMapGetSnapshotRequestsCounter = (SampledCumulativeCounter) this.sampledCounterManager
        .createCounter(sampledCumulativeCounterConfig);

    // Note that the monitoring service interface can be null if there is no monitoring support loaded into the server.
    IMonitoringProducer serviceInterface = null;
    try {
      serviceInterface = platformServiceRegistry.getService(new ServiceConfiguration<IMonitoringProducer>(){
        @Override
        public Class<IMonitoringProducer> getServiceType() {
          return IMonitoringProducer.class;
        }});
    } catch (ServiceException e) {
      Assert.fail("Multiple IMonitoringProducer implementations found!");
    }

    long reconnectTimeout = l2DSOConfig.clientReconnectWindow();
    logger.debug("Client Reconnect Window: " + reconnectTimeout + " seconds");
    reconnectTimeout *= 1000;

    RequestProcessor processor = new RequestProcessor(stageManager);
    
    ManagementTopologyEventCollector eventCollector = new ManagementTopologyEventCollector(serviceInterface);
    ClientEntityStateManager clientEntityStateManager = new ClientEntityStateManagerImpl();

    entityManager = new EntityManagerImpl(this.serviceRegistry, clientEntityStateManager, eventCollector, processor, this::flushLocalPipeline, this.configSetupManager.getServiceLocator());
    // We need to set up a stage to point at the ProcessTransactionHandler and we also need to register it for events, below.
    final ProcessTransactionHandler processTransactionHandler = new ProcessTransactionHandler(this.persistor, channelManager, entityManager, () -> l2Coordinator.getStateManager().cleanupKnownServers());
    final Stage<VoltronEntityMessage> processTransactionStage_voltron = stageManager.createStage(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE, VoltronEntityMessage.class, processTransactionHandler.getVoltronMessageHandler(), 1, maxStageSize, true);
    final Stage<TCMessage> multiRespond = stageManager.createStage(ServerConfigurationContext.RESPOND_TO_REQUEST_STAGE, TCMessage.class, processTransactionHandler.getMultiResponseSender(), 1, maxStageSize, true);
    final Sink<VoltronEntityMessage> voltronMessageSink = processTransactionStage_voltron.getSink();

    VoltronMessageHandler voltron = new VoltronMessageHandler(voltronMessageSink);
    // We need to connect the IInterEntityMessengerProvider to the voltronMessageSink.
    
    Stage<VoltronEntityMessage> fast = stageManager.createStage(ServerConfigurationContext.SINGLE_THREADED_FAST_PATH, VoltronEntityMessage.class, voltron, 1, maxStageSize);
    messengerProvider.setMessageSink(fast.getSink());
    entityManager.setMessageSink(fast.getSink());    
    // If we are running in a restartable mode, instantiate any entities in storage.
    processTransactionHandler.loadExistingEntities();
    
    final Stage<ClientHandshakeMessage> clientHandshake = stageManager.createStage(ServerConfigurationContext.CLIENT_HANDSHAKE_STAGE, ClientHandshakeMessage.class, createHandShakeHandler(entityManager, processTransactionHandler), 1, maxStageSize);
    
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, new TCMessageHydrateSink<>(clientHandshake.getSink()));
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_MESSAGE, new VoltronMessageSink(fast.getSink(), entityManager));
    messageRouter.routeMessageType(TCMessageType.SERVER_ENTITY_RESPONSE_MESSAGE, new TCMessageHydrateSink<>(communicatorResponseStage.getSink()));
    messageRouter.routeMessageType(TCMessageType.DIAGNOSTIC_REQUEST, new DiagnosticsHandler(this));    

    HASettingsChecker haChecker = new HASettingsChecker(configSetupManager, TCPropertiesImpl.getProperties());
    haChecker.validateHealthCheckSettingsForHighAvailability();

    this.groupCommManager = this.serverBuilder.createGroupCommManager(this.configSetupManager, stageManager,
                                                                      this.thisServerNodeID,
                                                                      this.stripeIDStateManager, mgr, this.globalWeightGeneratorFactory);

    L2StateChangeHandler stateHandler = new L2StateChangeHandler(createStageController(monitoringShimService), eventCollector);
    final Stage<StateChangedEvent> stateChange = stageManager.createStage(ServerConfigurationContext.L2_STATE_CHANGE_STAGE, StateChangedEvent.class, stateHandler, 1, maxStageSize);
    StateManager state = new StateManagerImpl(DistributedObjectServer.consoleLogger, this.groupCommManager, 
        stateChange.getSink(), stageManager, 
        configSetupManager.getActiveServerGroupForThisL2().getMembers().length,
        configSetupManager.getActiveServerGroupForThisL2().getElectionTimeInSecs(),
        weightGeneratorFactory, 
        this.persistor.getClusterStatePersistor());
    
    state.registerForStateChangeEvents(this.server);
    // And the stage for handling their response batching/serialization.
    Stage<Runnable> replicationResponseStage = stageManager.createStage(ServerConfigurationContext.PASSIVE_OUTGOING_RESPONSE_STAGE, Runnable.class, 
        new GenericHandler<>(), 1, maxStageSize);
//  routing for passive to receive replication    
    ReplicatedTransactionHandler replicatedTransactionHandler = new ReplicatedTransactionHandler(state, replicationResponseStage, this.persistor, entityManager, groupCommManager);
    // This requires both the stage for handling the replication/sync messages.
    Stage<ReplicationMessage> replicationStage = stageManager.createStage(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE, ReplicationMessage.class, 
        replicatedTransactionHandler.getEventHandler(), 1, maxStageSize);
    
    final ChannelLifeCycleHandler channelLifeCycleHandler = new ChannelLifeCycleHandler(this.communicationsManager,
                                                                                        stageManager, channelManager,
                                                                                        clientEntityStateManager, state, processTransactionHandler, eventCollector);
    channelManager.addEventListener(channelLifeCycleHandler);
    
    this.l2Coordinator = this.serverBuilder.createL2HACoordinator(consoleLogger, this, 
                                                                  stageManager, state,
                                                                  this.groupCommManager,
                                                                  this.persistor,
                                                                  this.globalWeightGeneratorFactory,
                                                                  this.configSetupManager,
                                                                  this.stripeIDStateManager,
                                                                  channelLifeCycleHandler);

    connectServerStateToReplicatedState(state, clientEntityStateManager, l2Coordinator.getReplicatedClusterStateManager());
// setup replication    
    final Stage<Runnable> replicationSenderStage = stageManager.createStage(ServerConfigurationContext.ACTIVE_TO_PASSIVE_DRIVER_STAGE, Runnable.class, new GenericHandler<>(), 1, maxStageSize);
    ReplicationSender replicationSender = new ReplicationSender(replicationSenderStage, groupCommManager);
    
    final ActiveToPassiveReplication passives = new ActiveToPassiveReplication(processTransactionHandler, l2Coordinator.getReplicatedClusterStateManager().getPassives(), this.persistor.getEntityPersistor(), replicationSender, this.getGroupManager());
    processor.setReplication(passives); 

    Stage<ReplicationMessageAck> replicationStageAck = stageManager.createStage(ServerConfigurationContext.PASSIVE_REPLICATION_ACK_STAGE, ReplicationMessageAck.class, 
        new AbstractEventHandler<ReplicationMessageAck>() {
          @Override
          public void handleEvent(ReplicationMessageAck context) throws EventHandlerException {
            switch (context.getType()) {
              case ReplicationMessageAck.BATCH:
                passives.batchAckReceived(context);
                break;
              case ReplicationMessageAck.START_SYNC:
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
    Sink<ReplicationMessage> replication = new OrderedSink<ReplicationMessage>(logger, replicationStage.getSink());
    this.groupCommManager.routeMessages(ReplicationMessage.class, replication);

    this.groupCommManager.routeMessages(ReplicationMessageAck.class, replicationStageAck.getSink());
    Sink<PlatformInfoRequest> info = createPlatformInformationStages(stageManager, maxStageSize, monitoringShimService);
    dispatchHandler.addListener(connectPassiveEvents(info, haConfig.getNodesStore(), monitoringShimService));
    
    final GlobalServerStatsImpl serverStats = new GlobalServerStatsImpl(globalObjectFaultCounter,
                                                                              globalTxnCounter,
                                                                              broadcastCounter,
                                                                              changesPerBroadcast,
                                                                              transactionSizeCounter,
        globalOperationCounter);

    serverStats.serverMapGetSizeRequestsCounter(globalServerMapGetSizeRequestsCounter)
        .serverMapGetValueRequestsCounter(globalServerMapGetValueRequestsCounter)
        .serverMapGetSnapshotRequestsCounter(globalServerMapGetSnapshotRequestsCounter);

    final ServerClientHandshakeManager clientHandshakeManager = new ServerClientHandshakeManager(
                                                                                                 LoggerFactory
                                                                                                     .getLogger(ServerClientHandshakeManager.class),
                                                                                                 channelManager,
                                                                                                 new Timer(
                                                                                                           "Reconnect timer",
                                                                                                           true),
                                                                                                 reconnectTimeout,
                                                                                                 fast.getSink(),
                                                                                                 consoleLogger);
    
    this.context = this.serverBuilder.createServerConfigurationContext(stageManager, channelManager,
                                                                       channelStats, this.l2Coordinator,
        clientHandshakeManager,
                                                                       serverStats, this.connectionIdFactory,
                                                                       maxStageSize,
                                                                       this.l1Listener.getChannelManager()
    );
    toInit.add(this.serverBuilder);

    startStages(stageManager, toInit);    

    // XXX: yucky casts
    this.managementContext = new ServerManagementContext((DSOChannelManagerMBean) channelManager,
                                                         serverStats, channelStats, instanceMonitor,
                                                         connectionPolicy);

    final CallbackOnExitHandler handler = new CallbackGroupExceptionHandler(logger, consoleLogger);
    this.threadGroup.addCallbackOnExitExceptionHandler(GroupException.class, handler);

    startGroupManagers();
    this.l2Coordinator.start();
    startDiagnosticListener();
    setLoggerOnExit();
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
      default:
      //  not lifecycle, ignore
        logger.debug("completed mgmt " + action + " on " + eid);
    }
    boolean forDestroy = (action == ServerEntityAction.DESTROY_ENTITY);
    if (!this.l2Coordinator.getStateManager().isActiveCoordinator()) {
      try {
        this.seda.getStageManager()
            .getStage(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE, ReplicationMessage.class)
            .getSink().addSingleThreaded(ReplicationMessage.createLocalContainer(SyncReplicationActivity.createFlushLocalPipelineMessage(fetch, forDestroy)));
        return;
      } catch (IllegalStateException state) {
//  ignore, could have transitioned to active before message got added
      }
    }
//  must be active, noop the ProcessTransactionHandler

    this.seda.getStageManager()
        .getStage(ServerConfigurationContext.SINGLE_THREADED_FAST_PATH, VoltronEntityMessage.class)
        .getSink().addSingleThreaded(new LocalPipelineFlushMessage(EntityDescriptor.createDescriptorForInvoke(fetch, ClientInstanceID.NULL_ID), forDestroy));
  }

  private StageController createStageController(LocalMonitoringProducer monitoringSupport) {
    StageController control = new StageController();
//  PASSIVE-UNINITIALIZED handle replicate messages right away. 
    // NOTE:  PASSIVE_OUTGOING_RESPONSE_STAGE must be active whenever PASSIVE_REPLICATION_STAGE is.
    control.addStageToState(StateManager.PASSIVE_UNINITIALIZED, ServerConfigurationContext.PASSIVE_REPLICATION_STAGE);
    control.addStageToState(StateManager.PASSIVE_UNINITIALIZED, ServerConfigurationContext.PASSIVE_OUTGOING_RESPONSE_STAGE);
//  REPLICATION needs to continue in STANDBY so include that stage here.  SYNC also needs to be handled.
    control.addStageToState(StateManager.PASSIVE_SYNCING, ServerConfigurationContext.PASSIVE_REPLICATION_STAGE);
    control.addStageToState(StateManager.PASSIVE_SYNCING, ServerConfigurationContext.PASSIVE_OUTGOING_RESPONSE_STAGE);
//  REPLICATION needs to continue in STANDBY so include that stage here. SYNC goes away
    control.addStageToState(StateManager.PASSIVE_STANDBY, ServerConfigurationContext.PASSIVE_REPLICATION_STAGE);
    control.addStageToState(StateManager.PASSIVE_STANDBY, ServerConfigurationContext.PASSIVE_OUTGOING_RESPONSE_STAGE);
//  turn on the process transaction handler, the active to passive driver, and the replication ack handler, replication handler needs to be shutdown and empty for 
//  active to start
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.SINGLE_THREADED_FAST_PATH);
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.REQUEST_PROCESSOR_DURING_SYNC_STAGE);
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.ACTIVE_TO_PASSIVE_DRIVER_STAGE);
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.VOLTRON_MESSAGE_STAGE);
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.RESPOND_TO_REQUEST_STAGE);
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.PASSIVE_REPLICATION_ACK_STAGE);
    control.addTriggerToState(StateManager.ACTIVE_COORDINATOR, () -> {
      server.updateActivateTime();
  // transition the local monitoring producer to active so the tree is rebuilt as a new active of the stripe
      monitoringSupport.serverIsActive();
      PlatformInfoRequest req = PlatformInfoRequest.createEmptyRequest();
  //  due to the broadcast nature of this call, it is possible to get multiple 
  //  responses from the same server.  The underlying collector must tolerate this
      groupCommManager.sendAll(req);  //  request info from all the other servers
    });
    return control;
  }
  
  private GroupEventsListener connectPassiveEvents(Sink<PlatformInfoRequest> infoHandler, NodesStore nodesStore, LocalMonitoringProducer monitoringShimService) {
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
          fake.setMessageOrginator(nodeID);
          infoHandler.addSingleThreaded(fake);
        }
      }
    };
  }
  
  private void connectServerStateToReplicatedState(StateManager mgr, ClientEntityStateManager clients, ReplicatedClusterStateManager rcs) {
    mgr.registerForStateChangeEvents(new StateChangeListener() {
      @Override
      public void l2StateChanged(StateChangedEvent sce) {
        rcs.setCurrentState(sce.getCurrentState());
        final Set<ClientID> existingConnections = new HashSet<>(persistor.getClientStatePersistor().loadAllClientIDs());
//  must do this because the replicated state when it comes to clients, may not include all the references 
//  to clients that were in the midst of cleaning up after disconnection.  
        existingConnections.addAll(clients.clearClientReferences());
        if (sce.movedToActive()) {
          getContext().getClientHandshakeManager().setStarting(existingConnections);
          startActiveMode(sce.getOldState().equals(StateManager.PASSIVE_STANDBY));
          try {
            startL1Listener(existingConnections);
          } catch (IOException ioe) {
            throw new RuntimeException(ioe);
          }
        }
      }
    });
  }
  
  public void startGroupManagers() {
    try {

      final NodeID myNodeId = this.groupCommManager.join(this.haConfig.getThisNode(), this.haConfig.getNodesStore());
      logger.info("This L2 Node ID = " + myNodeId);
    } catch (final GroupException e) {
      logger.error("Caught Exception :", e);
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unused")
  public void reloadConfiguration() throws ConfigurationSetupException {
    if (false) { throw new ConfigurationSetupException(); }
    throw new UnsupportedOperationException();
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
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_MULTI_RESPONSE, VoltronEntityMultiResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.SERVER_ENTITY_MESSAGE, ServerEntityMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SERVER_ENTITY_RESPONSE_MESSAGE, ServerEntityResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.DIAGNOSTIC_REQUEST, DiagnosticMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.DIAGNOSTIC_RESPONSE, DiagnosticResponseImpl.class);
    return messageTypeClassMapping;
  }

  protected Logger getLogger() {
    return logger;
  }

  private ServerID makeServerNodeID(L2Config l2DSOConfig) {
    String host = l2DSOConfig.tsaGroupPort().getBind();
    if (TCSocketAddress.WILDCARD_IP.equals(host)) {
      host = l2DSOConfig.host();
    }
    final Node node = new Node(host, l2DSOConfig.tsaPort().getValue());
    final ServerID aNodeID = new ServerID(node.getServerNodeName(), UUID.getUUID().toString().getBytes());
    logger.info("Creating server nodeID: " + aNodeID);
    return aNodeID;
  }

  public ServerID getServerNodeID() {
    return this.thisServerNodeID;
  }

  // for testing purpose only
  public ChannelManager getChannelManager() {
    return this.l1Listener.getChannelManager();
  }

  private void setLoggerOnExit() {
    CommonShutDownHook.addShutdownHook(new Runnable() {
      @Override
      public void run() {
        logger.info("L2 Exiting...");
      }
    });
  }

  private void startActiveMode(boolean wasStandby) {
    if (!wasStandby && persistor.getClusterStatePersistor().getInitialState() == null) {
      
      Sink<VoltronEntityMessage> msgSink = this.seda.getStageManager().getStage(ServerConfigurationContext.SINGLE_THREADED_FAST_PATH, VoltronEntityMessage.class).getSink();
      Map<EntityID, VoltronEntityMessage> checkdups = new HashMap<>();
//  find annotated permanent entities
      List<VoltronEntityMessage> annotated = entityManager.getEntityLoader().getAnnotatedEntities();
      for (VoltronEntityMessage vem : annotated) {
//  map them to weed out duplicates
        checkdups.put(vem.getEntityDescriptor().getEntityID(), vem);
      } 
//  first configured permanent entities
      List<VoltronEntityMessage> msgs = PermanentEntityParser.parseEntities(this.configSetupManager.commonl2Config().getBean().getPlatformConfiguration());
      for (VoltronEntityMessage vem : msgs) {
//  map them to weed out duplicates, opt for the configured version when there are duplicates
        checkdups.put(vem.getEntityDescriptor().getEntityID(), vem);
      }
      for (VoltronEntityMessage vem : checkdups.values()) {
        msgSink.addSingleThreaded(vem);
      }
      EntityPersistor ep = this.persistor.getEntityPersistor();
      for (VoltronEntityMessage vem : checkdups.values()) {
        try {
          ep.waitForPermanentEntityCreation(vem.getEntityDescriptor().getEntityID());
        } catch (RuntimeException e) {
          throw e;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  public void startL1Listener(Set<ClientID> existingConnections) throws IOException {
    try {
      this.l1Diagnostics.stop(0L);
    } catch (TCTimeoutException to) {
      throw Assert.failure("no timeout set!", to);
    }
    boolean clientBound = false;
    while (!clientBound) {
      try {
        this.l1Listener.start(existingConnections);
        clientBound = true;
      } catch (BindException bind) {
  // this seems to happen on windows but should not.  we just gave up the port.  
  // loop forever as a hack.
        logger.warn("client server port not available for binding:", bind);
        try {
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ie) {
          logger.warn("client server port binding interrupted:", ie);
          throw bind;
        }
      }
    }
    if (!existingConnections.isEmpty()) {
      this.context.getClientHandshakeManager().startReconnectWindow();
    }
    consoleLogger.info("Terracotta Server instance has started up as ACTIVE node on " + format(this.l1Listener)
                       + " successfully, and is now ready for work.");
  }

  public void startDiagnosticListener() throws IOException {
    this.l1Diagnostics.start(Collections.emptySet());
  }
  
  private static String format(NetworkListener listener) {
    final StringBuilder sb = new StringBuilder(listener.getBindAddress().getHostAddress());
    sb.append(':');
    sb.append(listener.getBindPort());
    return sb.toString();
  }

  /**
   * Since this is accessed via JMX and l1Listener isn't initialed when a secondary is waiting on the lock file, use the
   * config value unless the special value 0 is specified for use in the tests to get a random port.
   */
  public int getListenPort() {
    final L2Config l2DSOConfig = this.configSetupManager.dsoL2Config();
    final int configValue = l2DSOConfig.tsaPort().getValue();
    if (configValue != 0) { return configValue; }
    if (this.l1Listener != null) {
      try {
        return this.l1Listener.getBindPort();
      } catch (final IllegalStateException ise) {/**/
      }
    }
    return -1;
  }

  public InetAddress getListenAddr() {
    return this.l1Listener.getBindAddress();
  }

  public int getGroupPort() {
    final L2Config l2DSOConfig = this.configSetupManager.dsoL2Config();
    final int configValue = l2DSOConfig.tsaGroupPort().getValue();
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

  protected GroupManager<AbstractGroupMessage> getGroupManager() {
    return this.groupCommManager;
  }

  @Override
  public boolean isAlive(String name) {
    throw new UnsupportedOperationException();
  }

  protected ClientHandshakeHandler createHandShakeHandler(EntityManager entities, ProcessTransactionHandler processTransactionHandler) {
    return new ClientHandshakeHandler(this.configSetupManager.dsoL2Config().serverName(), entities, processTransactionHandler);
  }

  // for tests only
  public CommunicationsManager getCommunicationsManager() {
    return communicationsManager;
  }

  public void dumpClusterState() {
    try {
      L2DumperMBean mbean = (L2DumperMBean) TerracottaManagement.findMBean(L2MBeanNames.DUMPER, L2DumperMBean.class, ManagementFactory.getPlatformMBeanServer());
      mbean.dumpClusterState();
    } catch (Exception e) {
      logger.warn("Could not take Cluster dump, hence taking server dump only");
      dump();
    }
  }
}
