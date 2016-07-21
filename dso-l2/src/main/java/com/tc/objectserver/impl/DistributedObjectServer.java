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

import com.tc.l2.handler.PassiveInfoMessageHandler;
import com.tc.l2.msg.PassiveInfoMessage;
import com.tc.objectserver.api.EntityManager;
import com.tc.services.LogBasedStateDumper;
import com.tc.services.PlatformServiceProvider;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.monitoring.IMonitoringProducer;

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
import com.tc.entity.NetworkVoltronEntityMessageImpl;
import com.tc.entity.ServerEntityMessageImpl;
import com.tc.entity.ServerEntityResponseMessage;
import com.tc.entity.ServerEntityResponseMessageImpl;
import com.tc.entity.VoltronEntityAppliedResponseImpl;
import com.tc.entity.VoltronEntityMessage;
import com.tc.entity.VoltronEntityReceivedResponseImpl;
import com.tc.entity.VoltronEntityRetiredResponseImpl;
import com.tc.exception.TCRuntimeException;
import com.tc.exception.TCServerRestartException;
import com.tc.exception.TCShutdownServerException;
import com.tc.exception.ZapDirtyDbServerNodeException;
import com.tc.exception.ZapServerNodeException;
import com.tc.handler.CallbackDumpAdapter;
import com.tc.handler.CallbackDumpHandler;
import com.tc.handler.CallbackGroupExceptionHandler;
import com.tc.handler.CallbackZapDirtyDbExceptionAdapter;
import com.tc.handler.CallbackZapServerNodeExceptionAdapter;
import com.tc.handler.LockInfoDumpHandler;
import com.tc.io.TCFile;
import com.tc.io.TCFileImpl;
import com.tc.io.TCRandomFileAccessImpl;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.ChannelWeightGenerator;
import com.tc.l2.ha.HASettingsChecker;
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
import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.l2.msg.PlatformInfoRequest;
import com.tc.l2.msg.ReplicationEnvelope;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.operatorevent.OperatorEventsPassiveServerConnectionListener;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerConfigImpl;
import com.tc.l2.state.StateManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.CustomerLogging;
import com.tc.logging.DumpHandlerStore;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.logging.ThreadDumpHandler;
import com.tc.management.RemoteManagement;
import com.tc.management.RemoteManagementImpl;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaManagement;
import com.tc.management.TerracottaRemoteManagement;
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
import com.tc.net.groups.MessageID;
import com.tc.net.groups.Node;
import com.tc.net.protocol.HttpConnectionContext;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.HydrateContext;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
import com.tc.net.protocol.transport.TransportHandshakeErrorNullHandler;
import com.tc.net.utils.L2Utils;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.config.schema.L2Config;
import com.tc.object.msg.ClientHandshakeAckMessageImpl;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.msg.ClientHandshakeRefusedMessageImpl;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.msg.InvokeRegisteredServiceMessage;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;
import com.tc.object.msg.ListRegisteredServicesMessage;
import com.tc.object.msg.ListRegisteredServicesResponseMessage;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.net.DSOChannelManagerImpl;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionManager;
import com.tc.objectserver.context.NodeStateEventContext;
import com.tc.objectserver.core.api.GlobalServerStatsImpl;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.entity.ActiveToPassiveReplication;
import com.tc.objectserver.handler.ChannelLifeCycleHandler;
import com.tc.objectserver.handler.ClientHandshakeHandler;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.handler.RequestLockUnLockHandler;
import com.tc.objectserver.handler.RespondToRequestLockHandler;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.locks.LockManagerImpl;
import com.tc.objectserver.locks.LockResponseContext;
import com.tc.objectserver.persistence.ClientStatePersistor;
import com.tc.objectserver.persistence.FlatFileStorageProviderConfiguration;
import com.tc.objectserver.persistence.FlatFileStorageServiceProvider;
import com.tc.objectserver.persistence.Persistor;
import com.tc.objectserver.persistence.NullPlatformStorageServiceProvider;
import com.tc.objectserver.persistence.NullPlatformStorageProviderConfiguration;
import com.tc.operatorevent.OperatorEventHistoryProviderImpl;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.TCMemoryManagerImpl;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.server.ServerConnectionValidator;
import com.tc.server.TCServer;
import com.tc.services.CommunicatorResponseHandler;
import com.tc.services.CommunicatorService;
import com.tc.services.EntityMessengerProvider;
import com.tc.services.TerracottaServiceProviderRegistry;
import com.tc.services.TerracottaServiceProviderRegistryImpl;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.util.Assert;
import com.tc.util.CommonShutDownHook;
import com.tc.util.ProductInfo;
import com.tc.util.StartupLock;
import com.tc.util.TCTimeoutException;
import com.tc.util.UUID;
import com.tc.util.concurrent.Runners;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.NullThreadIDMapImpl;
import com.tc.util.runtime.ThreadIDMap;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
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
import com.tc.objectserver.entity.NoopEntityMessage;
import com.tc.objectserver.entity.RequestProcessor;
import com.tc.objectserver.entity.RequestProcessorHandler;
import com.tc.objectserver.entity.ServerEntityFactory;
import com.tc.objectserver.handler.ReplicatedTransactionHandler;
import com.tc.objectserver.handler.ReplicationSender;
import com.tc.objectserver.handler.ServerManagementHandler;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventCallback;
import java.lang.management.ManagementFactory;
import java.util.Map;


/**
 * Startup and shutdown point. Builds and starts the server
 */
public class DistributedObjectServer implements TCDumper, LockInfoDumpHandler, ServerConnectionValidator,
    DumpHandlerStore {
  private final ConnectionPolicy                 connectionPolicy;
  private final TCServer                         server;
  private final ServerBuilder                    serverBuilder;
  protected final L2ConfigurationSetupManager    configSetupManager;
  protected final HaConfigImpl                   haConfig;

  private static final TCLogger                  logger           = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger                  consoleLogger    = CustomerLogging.getConsoleLogger();

  private ServerID                               thisServerNodeID = ServerID.NULL_ID;
  protected NetworkListener                      l1Listener;
  private TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider;
  private CommunicationsManager                  communicationsManager;
  private ServerConfigurationContext             context;
  private CounterManager                         sampledCounterManager;
  private LockManagerImpl                        lockManager;
  private ServerManagementContext                managementContext;
  private StartupLock                            startupLock;
  private Persistor                              persistor;

  private L2Coordinator                          l2Coordinator;

  private TCProperties                           tcProperties;

  private ConnectionIDFactoryImpl                connectionIdFactory;

  private final TCThreadGroup                    threadGroup;
  private final SEDA<HttpConnectionContext>                             seda;

  private ReconnectConfig                        l1ReconnectConfig;

  private GroupManager<AbstractGroupMessage> groupCommManager;
  private Stage<HydrateContext>                                  hydrateStage;
  private StripeIDStateManagerImpl               stripeIDStateManager;

  private final CallbackDumpHandler              dumpHandler      = new CallbackDumpHandler();

  protected final TCSecurityManager              tcSecurityManager;

  private final TaskRunner                       taskRunner;
  private final TerracottaServiceProviderRegistry serviceRegistry;
  private WeightGeneratorFactory globalWeightGeneratorFactory;
  private EntityManager entityManager;

  // used by a test
  public DistributedObjectServer(L2ConfigurationSetupManager configSetupManager, TCThreadGroup threadGroup,
                                 ConnectionPolicy connectionPolicy, TCServerInfoMBean tcServerInfoMBean) {
    this(configSetupManager, threadGroup, connectionPolicy, new SEDA<HttpConnectionContext>(threadGroup), null, null);

  }

  public DistributedObjectServer(L2ConfigurationSetupManager configSetupManager, TCThreadGroup threadGroup,
                                 ConnectionPolicy connectionPolicy,
                                 SEDA<HttpConnectionContext> seda,
                                 TCServer server, TCSecurityManager securityManager) {
    // This assertion is here because we want to assume that all threads spawned by the server (including any created in
    // 3rd party libs) inherit their thread group from the current thread . Consider this before removing the assertion.
    // Even in tests, we probably don't want different thread group configurations
    Assert.assertEquals(threadGroup, Thread.currentThread().getThreadGroup());

    this.tcSecurityManager = securityManager;
    if (configSetupManager.isSecure()) {
      Assert.assertNotNull("Security is turned on, but TCSecurityManager", this.tcSecurityManager);
      consoleLogger.info("Security enabled, turning on SSL");
    }

    this.configSetupManager = configSetupManager;
    this.haConfig = new HaConfigImpl(this.configSetupManager);
    this.connectionPolicy = connectionPolicy;
    this.threadGroup = threadGroup;
    this.seda = seda;
    this.server = server;
    this.serverBuilder = createServerBuilder(this.haConfig, logger, server, configSetupManager.dsoL2Config());
    this.taskRunner = Runners.newDefaultCachedScheduledTaskRunner(this.threadGroup);
    this.serviceRegistry = new TerracottaServiceProviderRegistryImpl();
  }

  protected ServerBuilder createServerBuilder(HaConfig config, TCLogger tcLogger, TCServer server,
                                                 L2Config l2dsoConfig) {
    Assert.assertEquals(config.isActiveActive(), false);
    return new StandardServerBuilder(config, tcLogger, tcSecurityManager);
  }

  protected ServerBuilder getServerBuilder() {
    return this.serverBuilder;
  }

  public TaskRunner getTaskRunner() {
    return taskRunner;
  }

  @Override
  public void dump() {
    this.dumpHandler.dump();
    this.serverBuilder.dump();
    LogBasedStateDumper stateDumper = new LogBasedStateDumper("platform");
    this.entityManager.dumpStateTo(stateDumper.subStateDumper("entities"));
    this.serviceRegistry.dumpStateTo(stateDumper.subStateDumper("services"));
    stateDumper.logState();
  }

  public synchronized void start() throws IOException, LocationNotCreatedException, FileNotCreatedException {

    threadGroup.addCallbackOnExitDefaultHandler(new ThreadDumpHandler(this));
    threadGroup.addCallbackOnExitDefaultHandler(this.dumpHandler);
    threadGroup.addCallbackOnExitExceptionHandler(TCServerRestartException.class, new CallbackOnExitHandler() {
      @Override
      public void callbackOnExit(CallbackOnExitState state) {
        state.setRestartNeeded();
      }
    });
    threadGroup.addCallbackOnExitExceptionHandler(TCShutdownServerException.class, new CallbackOnExitHandler() {
      @Override
      public void callbackOnExit(CallbackOnExitState state) {
        Throwable t = state.getThrowable();
        while (t.getCause() != null) {
          t = t.getCause();
        }
        consoleLogger.error("Server exiting: " + t.getMessage());
      }
    });

    this.thisServerNodeID = makeServerNodeID(this.configSetupManager.dsoL2Config());
    ThisServerNodeId.setThisServerNodeId(thisServerNodeID);

    TerracottaOperatorEventLogging.setNodeNameProvider(new ServerNameProvider(this.configSetupManager.dsoL2Config()
        .serverName()));


    final List<PostInit> toInit = new ArrayList<>();

    // perform the DSO network config verification
    final L2Config l2DSOConfig = this.configSetupManager.dsoL2Config();

    TCLogging.setLogLocationAndType(configSetupManager.commonl2Config().logsPath().toURI(), TCLogging.ProcessType.SERVER);
    
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
    final boolean restartable = l2DSOConfig.getRestartable();
    
    TCFile dataLoc = new TCFileImpl(this.configSetupManager.commonl2Config().dataPath());
    String serverName = this.configSetupManager.dsoL2Config().serverName();
//  this is character replacement for windows platform file names.  This is probably a bogus way to 
//  handle this.  Re-evaluate the way data directories are managed for 5.0 and fix this when a plan is
//  devised
    serverName = serverName.replace('.', '-');
    serverName = serverName.replace(':', '$');

    final TCFile location = new TCFileImpl(dataLoc, serverName);
    boolean retries = tcProperties.getBoolean(TCPropertiesConsts.L2_STARTUPLOCK_RETRIES_ENABLED);
    this.startupLock = this.serverBuilder.createStartupLock(location, retries);

    if (!this.startupLock.canProceed(new TCRandomFileAccessImpl())) {
      consoleLogger.error("Another L2 process is using the directory " + location + " as data directory.");
      if (!restartable) {
        consoleLogger.error("This is not allowed with persistence mode set to temporary-swap-only.");
      }
      consoleLogger.error("Exiting...");
      System.exit(1);
    }

    final int maxStageSize = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY);
    final StageManager stageManager = this.seda.getStageManager();
    final SessionManager sessionManager = new NullSessionManager();

    this.dumpHandler.registerForDump(new CallbackDumpAdapter(stageManager));

    this.sampledCounterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, true, 0L);

    logger.debug("persistent: " + restartable);

    serviceRegistry.initialize(this.configSetupManager.getL2Identifier(), this.configSetupManager.commonl2Config().getBean(), Thread.currentThread().getContextClassLoader());

    if(restartable) {
      // For now, we will register com.tc.objectserver.persistence.FlatFileStorageServiceProvider here.  We are currently
      //  treating it as a core component of the platform but, in the future, it may move out and be loaded like user
      //  services or be discarded, entirely.
      FlatFileStorageServiceProvider flatFileService = new FlatFileStorageServiceProvider();
      if (!flatFileService.initialize(new FlatFileStorageProviderConfiguration(null, restartable))) {
        throw new AssertionError("bad flat file initialization");
      }
      serviceRegistry.registerExternal(flatFileService);
    } else {
      NullPlatformStorageServiceProvider nullPlatformStorageServiceProvider = new NullPlatformStorageServiceProvider();
      nullPlatformStorageServiceProvider.initialize(new NullPlatformStorageProviderConfiguration());
      serviceRegistry.registerExternal(nullPlatformStorageServiceProvider);
    }

    // The platform gets the reserved consumerID 0.
    long platformConsumerID = 0;
    ServiceRegistry platformServiceRegistry = serviceRegistry.subRegistry(platformConsumerID);
    
    persistor = serverBuilder.createPersistor(platformServiceRegistry);
    persistor.start();

    if(!persistor.wasDBClean()) {
      // make sure peristor is not using any storage service
      persistor.close();
      serviceRegistry.clearServiceProvidersState();
      // create the persistor once again as underlying storage service might have cleared its internal state
      persistor = serverBuilder.createPersistor(platformServiceRegistry);
      persistor.start();
    }

    dumpHandler.registerForDump(new CallbackDumpAdapter(persistor));
    new ServerPersistenceVersionChecker(persistor.getClusterStatePersistor()).checkAndSetVersion();

    // register the terracotta operator event logger
    this.operatorEventHistoryProvider = new OperatorEventHistoryProviderImpl();

    this.threadGroup
        .addCallbackOnExitExceptionHandler(ZapDirtyDbServerNodeException.class,
                                           new CallbackZapDirtyDbExceptionAdapter(logger, consoleLogger, this.persistor
                                               .getClusterStatePersistor()));
    this.threadGroup
        .addCallbackOnExitExceptionHandler(ZapServerNodeException.class,
                                           new CallbackZapServerNodeExceptionAdapter(logger, consoleLogger,
                                                                                     this.persistor
                                                                                         .getClusterStatePersistor()));

    ClientStatePersistor clientStateStore = this.persistor.getClientStatePersistor();

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
    this.communicationsManager = new CommunicationsManagerImpl(CommunicationsManager.COMMSMGR_SERVER, mm,
                                                               messageRouter, networkStackHarnessFactory,
                                                               this.connectionPolicy, commWorkerThreadCount,
                                                               new HealthCheckerConfigImpl(tcProperties
                                                                   .getPropertiesFor(TCPropertiesConsts.L2_L1_HEALTH_CHECK_CATEGORY), "TSA Server"),
                                                               this.thisServerNodeID,
                                                               new TransportHandshakeErrorNullHandler(),
                                                               getMessageTypeClassMappings(), Collections.emptyMap(),
                                                               tcSecurityManager);


    final SampledCumulativeCounterConfig sampledCumulativeCounterConfig = new SampledCumulativeCounterConfig(1, 300,
                                                                                                             true, 0L);

    final TCMemoryManagerImpl tcMemManager = new TCMemoryManagerImpl(this.threadGroup);
    final long timeOut = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.LOGGING_LONG_GC_THRESHOLD);
    final LongGCLogger gcLogger = this.serverBuilder.createLongGCLogger(timeOut);

    tcMemManager.registerForMemoryEvents(gcLogger);
    // CDV-1181 warn if using CMS
    tcMemManager.checkGarbageCollectors();

    this.connectionIdFactory = new ConnectionIDFactoryImpl(clientStateStore);

    final int serverPort = l2DSOConfig.tsaPort().getValue();

    final String dsoBind = l2DSOConfig.tsaPort().getBind();
    this.l1Listener = this.communicationsManager.createListener(sessionManager,
                                                                new TCSocketAddress(dsoBind, serverPort), true,
                                                                this.connectionIdFactory);

    this.stripeIDStateManager = new StripeIDStateManagerImpl(this.haConfig, this.persistor.getClusterStatePersistor());

    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.stripeIDStateManager));

    final ProductInfo pInfo = ProductInfo.getInstance();
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
    final ChannelWeightGenerator connectedClientCountWeightGenerator = new ChannelWeightGenerator(channelManager);
    weightGeneratorFactory.add(connectedClientCountWeightGenerator);
    // 3)  ServerUptimeWeightGenerator.
    final ServerUptimeWeightGenerator serverUptimeWeightGenerator = new ServerUptimeWeightGenerator();
    weightGeneratorFactory.add(serverUptimeWeightGenerator);
    // 4)  RandomWeightGenerator.
    final RandomWeightGenerator randomWeightGenerator = new RandomWeightGenerator(new SecureRandom());
    weightGeneratorFactory.add(randomWeightGenerator);
    // -We can now install the generator as it is built.
    this.globalWeightGeneratorFactory = weightGeneratorFactory;
    

    final ChannelStatsImpl channelStats = new ChannelStatsImpl(sampledCounterManager, channelManager);
    channelManager.addEventListener(channelStats);

    CommunicatorService communicatorService = new CommunicatorService(channelManager);
    serviceRegistry.registerBuiltin(communicatorService);
    final Stage<ServerEntityResponseMessage> communicatorResponseStage = stageManager.createStage(ServerConfigurationContext.SERVER_ENTITY_MESSAGE_RESPONSE_STAGE, ServerEntityResponseMessage.class,  new CommunicatorResponseHandler(communicatorService), 1, maxStageSize);

    PlatformServiceProvider platformServiceProvider = new PlatformServiceProvider(this);
    serviceRegistry.registerBuiltin(platformServiceProvider);

    // Creating a stage here so that the sink can be passed
    final Stage<LockResponseContext> respondToLockStage = stageManager.createStage(ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE, LockResponseContext.class, new RespondToRequestLockHandler(), 1, maxStageSize);
    this.lockManager = new LockManagerImpl(respondToLockStage.getSink(), channelManager);

    final CallbackDumpAdapter lockDumpAdapter = new CallbackDumpAdapter(this.lockManager);
    this.dumpHandler.registerForDump(lockDumpAdapter);
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
    final SampledCounter globalLockRecallCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final SampledRateCounterConfig sampledRateCounterConfig = new SampledRateCounterConfig(1, 300, true);
    final SampledRateCounter changesPerBroadcast = (SampledRateCounter) this.sampledCounterManager
        .createCounter(sampledRateCounterConfig);
    final SampledRateCounter transactionSizeCounter = (SampledRateCounter) this.sampledCounterManager
        .createCounter(sampledRateCounterConfig);
    final SampledCounter globalLockCount = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final SampledCumulativeCounter globalServerMapGetSizeRequestsCounter = (SampledCumulativeCounter) this.sampledCounterManager
        .createCounter(sampledCumulativeCounterConfig);
    final SampledCumulativeCounter globalServerMapGetValueRequestsCounter = (SampledCumulativeCounter) this.sampledCounterManager
        .createCounter(sampledCumulativeCounterConfig);
    final SampledCumulativeCounter globalServerMapGetSnapshotRequestsCounter = (SampledCumulativeCounter) this.sampledCounterManager
        .createCounter(sampledCumulativeCounterConfig);

    // We need to set up a stage to point at the ProcessTransactionHandler and we also need to register it for events, below.
    final ProcessTransactionHandler processTransactionHandler = new ProcessTransactionHandler(this.persistor.getEntityPersistor(), this.persistor.getTransactionOrderPersistor(), stageManager);
    final Stage<Runnable> requestProcessorStage = stageManager.createStage(ServerConfigurationContext.REQUEST_PROCESSOR_STAGE, Runnable.class, new RequestProcessorHandler(), L2Utils.getOptimalApplyStageWorkerThreads(true), maxStageSize);
    final Stage<VoltronEntityMessage> processTransactionStage_voltron = stageManager.createStage(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE, VoltronEntityMessage.class, processTransactionHandler.getVoltronMessageHandler(), 1, maxStageSize);
    final Sink<VoltronEntityMessage> voltronMessageSink = processTransactionStage_voltron.getSink();
    
    // We can now initialize the internal managers used by the processTransactionHandler.
    final Sink<Runnable> requestProcessorSink = requestProcessorStage.getSink();
    ClientEntityStateManager clientEntityStateManager = new ClientEntityStateManagerImpl(voltronMessageSink);

    // Note that the monitoring service interface can be null if there is no monitoring support loaded into the server.
    IMonitoringProducer serviceInterface = platformServiceRegistry.getService(new ServiceConfiguration<IMonitoringProducer>(){
      @Override
      public Class<IMonitoringProducer> getServiceType() {
        return IMonitoringProducer.class;
      }});
    ManagementTopologyEventCollector eventCollector = new ManagementTopologyEventCollector(this.getServerNodeID(), serviceInterface);
// add this server to the tree of servers
    eventCollector.serverDidJoinGroup(this.getServerNodeID(), server.getL2Identifier(), host, 
        bindAddress, serverPort, l2DSOConfig.tsaGroupPort().getValue(),
        pInfo.buildVersion(), pInfo.buildID());

    RequestProcessor processor = new RequestProcessor(requestProcessorSink);
    entityManager = new EntityManagerImpl(this.serviceRegistry, clientEntityStateManager, eventCollector, processor, this::sendNoop);
    channelManager.addEventListener(clientEntityStateManager);
    processTransactionHandler.setLateBoundComponents(channelManager, entityManager);
    
    // We need to connect the IInterEntityMessengerProvider to the voltronMessageSink.
    final EntityMessengerProvider messengerProvider = new EntityMessengerProvider(voltronMessageSink);
    this.serviceRegistry.registerBuiltin(messengerProvider);
    
    // If we are running in a restartable mode, instantiate any entities in storage.
    if (restartable) {
      processTransactionHandler.loadExistingEntities();
    }

    final Stage<LockRequestMessage> requestLock = stageManager.createStage(ServerConfigurationContext.REQUEST_LOCK_STAGE, LockRequestMessage.class, new RequestLockUnLockHandler(), 1, maxStageSize);
    final ChannelLifeCycleHandler channelLifeCycleHandler = new ChannelLifeCycleHandler(this.communicationsManager, channelManager, this.haConfig);
    stageManager.createStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE, NodeStateEventContext.class, channelLifeCycleHandler, 1, maxStageSize);
    channelManager.addEventListener(channelLifeCycleHandler);

    final Stage<ClientHandshakeMessage> clientHandshake = stageManager.createStage(ServerConfigurationContext.CLIENT_HANDSHAKE_STAGE, ClientHandshakeMessage.class, createHandShakeHandler(), 1, maxStageSize);
    this.hydrateStage = stageManager.createStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK, HydrateContext.class, new HydrateHandler(), stageWorkerThreadCount, maxStageSize);
    
    final Sink<HydrateContext> hydrateSink = this.hydrateStage.getSink();
    messageRouter.routeMessageType(TCMessageType.LOCK_REQUEST_MESSAGE, requestLock.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, clientHandshake.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.VOLTRON_ENTITY_MESSAGE, voltronMessageSink, hydrateSink);
    messageRouter.routeMessageType(TCMessageType.SERVER_ENTITY_RESPONSE_MESSAGE, communicatorResponseStage.getSink(), hydrateSink);

    long reconnectTimeout = l2DSOConfig.clientReconnectWindow();

    HASettingsChecker haChecker = new HASettingsChecker(configSetupManager, TCPropertiesImpl.getProperties());
    haChecker.validateHealthCheckSettingsForHighAvailability();

    logger.debug("Client Reconnect Window: " + reconnectTimeout + " seconds");
    reconnectTimeout *= 1000;
    final ServerClientHandshakeManager clientHandshakeManager = new ServerClientHandshakeManager(
                                                                                                 TCLogging
                                                                                                     .getLogger(ServerClientHandshakeManager.class),
                                                                                                 channelManager,
                                                                                                 this.lockManager,
                                                                                                 entityManager,
                                                                                                 processTransactionHandler,
                                                                                                 processTransactionStage_voltron,
                                                                                                 new Timer(
                                                                                                           "Reconnect timer",
                                                                                                           true),
                                                                                                 reconnectTimeout,
                                                                                                 restartable,
                                                                                                 consoleLogger);
    channelManager.addEventListener(new DSOChannelManagerEventListener() {
      @Override
      public void channelCreated(MessageChannel channel) {
        if (l2Coordinator.getStateManager().isActiveCoordinator()) {
          eventCollector.clientDidConnect(channel, new ClientID(channel.getChannelID().toLong()));
        }
      }

      @Override
      public void channelRemoved(MessageChannel channel) {
        if (l2Coordinator.getStateManager().isActiveCoordinator() && clientHandshakeManager.isStarted()) {
          eventCollector.clientDidDisconnect(channel, new ClientID(channel.getChannelID().toLong()));
        }
      }
    });
    
    this.groupCommManager = this.serverBuilder.createGroupCommManager(this.configSetupManager, stageManager,
                                                                      this.thisServerNodeID,
                                                                      this.stripeIDStateManager, this.globalWeightGeneratorFactory);

    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.groupCommManager));

    final Stage<StateChangedEvent> stateChange = stageManager.createStage(ServerConfigurationContext.L2_STATE_CHANGE_STAGE, StateChangedEvent.class, new L2StateChangeHandler(this.getServerNodeID(), createStageController(), eventCollector), 1, maxStageSize);
    StateManager state = new StateManagerImpl(DistributedObjectServer.consoleLogger, this.groupCommManager, 
        stateChange.getSink(), stageManager, 
        new StateManagerConfigImpl(configSetupManager.getActiveServerGroupForThisL2().getElectionTimeInSecs()),
        weightGeneratorFactory, 
        this.persistor.getClusterStatePersistor());
    
    state.registerForStateChangeEvents(this.server);
    
    Stage<PassiveInfoMessage> passiveInfoMessageReceiveStage = stageManager.createStage(ServerConfigurationContext.PASSIVE_INFO_MESSAGE_RECEIVE_STAGE, PassiveInfoMessage.class, new PassiveInfoMessageHandler(), 1, maxStageSize);
    groupCommManager.routeMessages(PassiveInfoMessage.class, passiveInfoMessageReceiveStage.getSink());
    Stage<PassiveInfoMessage> passiveInfoMessageSendStage = stageManager.createStage
      (ServerConfigurationContext.PASSIVE_INFO_MESSAGE_SEND_STAGE, PassiveInfoMessage.class, new AbstractEventHandler<PassiveInfoMessage>() {
        @Override
        public void handleEvent(PassiveInfoMessage message) throws EventHandlerException {
          switch (message.getType()) {
            case PassiveInfoMessage.PASSIVE_JOIN: //fall-through 
            case PassiveInfoMessage.PASSIVE_LEFT: {
              groupCommManager.sendAll(message);
              break;
            }
            case PassiveInfoMessage.PASSIVE_CLEANUP: {
              Set<NodeID> passiveStandBys = state.getKnownPassiveStandByServers();
              for(NodeID nodeID : passiveStandBys) {
                if(!groupCommManager.isNodeConnected(nodeID)) {
                  groupCommManager.sendAll(PassiveInfoMessage.createPassiveLeftMessage(nodeID));
                }
              }
              break;
            }
            default: throw new IllegalArgumentException("got unexpected message type " + message.getType());
          }
        }
      }, 1, maxStageSize);

    this.l2Coordinator = this.serverBuilder.createL2HACoordinator(consoleLogger, this, 
                                                                  stageManager, state,
                                                                  this.groupCommManager,
                                                                  this.persistor.getClusterStatePersistor(),
                                                                  this.globalWeightGeneratorFactory,
                                                                  this.configSetupManager,
                                                                  this.stripeIDStateManager);

    connectServerStateToReplicatedState(state, l2Coordinator.getReplicatedClusterStateManager());
// setup replication    
    final Stage<ReplicationEnvelope> replicationDriver = stageManager.createStage(ServerConfigurationContext.ACTIVE_TO_PASSIVE_DRIVER_STAGE, ReplicationEnvelope.class, new ReplicationSender(groupCommManager), 1, maxStageSize);
    
    
    final ActiveToPassiveReplication passives = new ActiveToPassiveReplication(l2Coordinator.getReplicatedClusterStateManager().getPassives(), processTransactionHandler.getEntityList(), replicationDriver
      .getSink(), passiveInfoMessageSendStage.getSink());
    processor.setReplication(passives); 
//  routing for passive to receive replication    
    Stage<ReplicationMessage> replicationStage = stageManager.createStage(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE, ReplicationMessage.class, 
        new ReplicatedTransactionHandler(this.l2Coordinator.getStateManager(), this.persistor.getTransactionOrderPersistor(), entityManager, 
            this.persistor.getEntityPersistor(), groupCommManager).getEventHandler(), 1, maxStageSize);
    Stage<ReplicationMessageAck> replicationStageAck = stageManager.createStage(ServerConfigurationContext.PASSIVE_REPLICATION_ACK_STAGE, ReplicationMessageAck.class, 
        new AbstractEventHandler<ReplicationMessageAck>() {
          @Override
          public void handleEvent(ReplicationMessageAck context) throws EventHandlerException {
            switch (context.getType()) {
              case ReplicationMessageAck.RECEIVED:
                passives.ackReceived(context);
                break;
              case ReplicationMessageAck.COMPLETED:
                passives.ackCompleted(context);
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
    dispatchHandler.addListener(connectPassiveOperatorEvents(haConfig.getNodesStore(), eventCollector));
    Stage<GroupEvent> groupEvents = stageManager.createStage(ServerConfigurationContext.GROUP_EVENTS_DISPATCH_STAGE, GroupEvent.class, dispatchHandler, 1, maxStageSize);
    this.groupCommManager.registerForGroupEvents(dispatchHandler.createDispatcher(groupEvents.getSink()));
  //  TODO:  These stages should probably be activated and destroyed dynamically    
//  Replicated messages need to be ordered
    Sink<ReplicationMessage> replication = new OrderedSink<ReplicationMessage>(logger, replicationStage.getSink());
    this.groupCommManager.routeMessages(ReplicationMessage.class, replication);
    this.groupCommManager.routeMessages(PassiveSyncMessage.class, replication);

    this.groupCommManager.routeMessages(ReplicationMessageAck.class, replicationStageAck.getSink());
    createPlatformInformationStages(stageManager, maxStageSize, eventCollector);
    
    // The ProcessTransactionHandler also needs the L2 state events since it needs to change entity types between active
    //  and passive.
    this.l2Coordinator.getStateManager().registerForStateChangeEvents(processor);
    
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.l2Coordinator));

    final GlobalServerStatsImpl serverStats = new GlobalServerStatsImpl(globalObjectFaultCounter,
                                                                              globalTxnCounter,
                                                                              broadcastCounter,
                                                                              globalLockRecallCounter,
                                                                              changesPerBroadcast,
                                                                              transactionSizeCounter, globalLockCount,
        globalOperationCounter);

    serverStats.serverMapGetSizeRequestsCounter(globalServerMapGetSizeRequestsCounter)
        .serverMapGetValueRequestsCounter(globalServerMapGetValueRequestsCounter)
        .serverMapGetSnapshotRequestsCounter(globalServerMapGetSnapshotRequestsCounter);

    this.context = this.serverBuilder.createServerConfigurationContext(stageManager,
        this.lockManager, channelManager,
                                                                       channelStats, this.l2Coordinator,
        clientHandshakeManager,
                                                                       serverStats, this.connectionIdFactory,
                                                                       maxStageSize,
                                                                       this.l1Listener.getChannelManager(), this
    );
    toInit.add(this.serverBuilder);

    startStages(stageManager, toInit);
    
    ServerManagementHandler serverManagementHandler = new ServerManagementHandler();

    final RemoteManagement remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, haConfig.getNodesStore().getServerNameFromNodeName(thisServerNodeID.getName()));
    TerracottaRemoteManagement.setRemoteManagementInstance(remoteManagement);
    TerracottaOperatorEventLogging.getEventLogger().registerEventCallback(new TerracottaOperatorEventCallback() {
      @Override
      public void logOperatorEvent(TerracottaOperatorEvent event) {
        TSAManagementEventPayload payload = new TSAManagementEventPayload("TSA.OPERATOR_EVENT." + event.getEventTypeAsString());

        payload.getAttributes().put("OperatorEvent.CollapseString", event.getCollapseString());
        payload.getAttributes().put("OperatorEvent.EventLevel", event.getEventLevelAsString());
        payload.getAttributes().put("OperatorEvent.EventMessage", event.getEventMessage());
        payload.getAttributes().put("OperatorEvent.EventSubsystem", event.getEventSubsystemAsString());
        payload.getAttributes().put("OperatorEvent.EventType", event.getEventTypeAsString());
        payload.getAttributes().put("OperatorEvent.EventTime", event.getEventTime().getTime());
        payload.getAttributes().put("OperatorEvent.NodeName", event.getNodeName());

        remoteManagement.sendEvent(payload.toManagementEvent());
      }
    });

    // XXX: yucky casts
    this.managementContext = new ServerManagementContext(
        this.lockManager, (DSOChannelManagerMBean) channelManager,
                                                         serverStats, channelStats, instanceMonitor,
                                                         connectionPolicy,
                                                         remoteManagement);

    final CallbackOnExitHandler handler = new CallbackGroupExceptionHandler(logger, consoleLogger);
    this.threadGroup.addCallbackOnExitExceptionHandler(GroupException.class, handler);

    startGroupManagers();
    this.l2Coordinator.start();
    setLoggerOnExit();
  }
  
  private void createPlatformInformationStages(StageManager stageManager, int maxStageSize, ITopologyEventCollector eventCollector) {
    Stage<PlatformInfoRequest> stage = stageManager.createStage(ServerConfigurationContext.PLATFORM_INFORMATION_REQUEST, 
        PlatformInfoRequest.class, new PlatformInfoRequestHandler(groupCommManager, eventCollector).getEventHandler(), 1, maxStageSize);
    groupCommManager.routeMessages(PlatformInfoRequest.class, stage.getSink());
//  publish state change events to everyone in the stripe
    this.l2Coordinator.getStateManager().registerForStateChangeEvents((StateChangedEvent sce) -> {
      if (sce.movedToActive()) {
        server.updateActivateTime();
        PlatformInfoRequest req = new PlatformInfoRequest();
        req.setRequestType(PlatformInfoRequest.RequestType.SERVER_INFO);
//  due to the broadcast nature of this call, it is possible to get multiple 
//  responses from the same server.  The underlying collector must tolerate this
        groupCommManager.sendAll(req);  //  request info from all the other servers
      } else {
        try {
          groupCommManager.sendTo(l2Coordinator.getStateManager().getActiveNodeID(), new PlatformInfoRequest(sce.getCurrentState().getName(), -1, MessageID.NULL_ID));
        } catch (GroupException ge) {
         //  IGNORE
        }
      }
    });
  }
  
  private void startStages(StageManager stageManager, List<PostInit> toInit) {
//  exclude from startup specific stages that are controlled by the stage controller. 
    stageManager.startAll(this.context, toInit, 
        ServerConfigurationContext.VOLTRON_MESSAGE_STAGE,
        ServerConfigurationContext.CLIENT_HANDSHAKE_STAGE,
        ServerConfigurationContext.ACTIVE_TO_PASSIVE_DRIVER_STAGE,
        ServerConfigurationContext.PASSIVE_REPLICATION_STAGE,
        ServerConfigurationContext.PASSIVE_REPLICATION_ACK_STAGE,
        ServerConfigurationContext.PASSIVE_INFO_MESSAGE_SEND_STAGE,
        ServerConfigurationContext.PASSIVE_INFO_MESSAGE_RECEIVE_STAGE
    );
  }
  
  private void sendNoop(EntityID eid, long version) {
    if (!this.l2Coordinator.getStateManager().isActiveCoordinator()) {
      try {
        this.seda.getStageManager()
            .getStage(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE, ReplicationMessage.class)
            .getSink().addSingleThreaded(ReplicationMessage.createNoOpMessage(eid, version));
        return;
      } catch (IllegalStateException state) {
//  ignore, could have transitioned to active before message got added
      }
    }
//  must be active, noop the ProcessTransactionHandler
    this.seda.getStageManager()
        .getStage(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE, VoltronEntityMessage.class)
        .getSink().addSingleThreaded(new NoopEntityMessage(new EntityDescriptor(eid, ClientInstanceID.NULL_ID, version)));
  }

  private StageController createStageController() {
    StageController control = new StageController();
//  PASSIVE-UNINITIALIZED handle replicate messages right away. 
    control.addStageToState(StateManager.PASSIVE_UNINITIALIZED, ServerConfigurationContext.PASSIVE_REPLICATION_STAGE);
//  REPLICATION needs to continue in STANDBY so include that stage here.  SYNC also needs to be handled.
    control.addStageToState(StateManager.PASSIVE_SYNCING, ServerConfigurationContext.PASSIVE_REPLICATION_STAGE);
//  REPLICATION needs to continue in STANDBY so include that stage here. SYNC goes away
    control.addStageToState(StateManager.PASSIVE_STANDBY, ServerConfigurationContext.PASSIVE_REPLICATION_STAGE);
    control.addStageToState(StateManager.PASSIVE_STANDBY, ServerConfigurationContext.PASSIVE_INFO_MESSAGE_RECEIVE_STAGE);
//  turn on the process transaction handler, the active to passive driver, and the replication ack handler, replication handler needs to be shutdown and empty for 
//  active to start
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.ACTIVE_TO_PASSIVE_DRIVER_STAGE);
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.VOLTRON_MESSAGE_STAGE);
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.PASSIVE_REPLICATION_ACK_STAGE);
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.CLIENT_HANDSHAKE_STAGE);
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.PASSIVE_INFO_MESSAGE_SEND_STAGE);
    return control;
  }
  
  private GroupEventsListener connectPassiveOperatorEvents(NodesStore nodesStore, ManagementTopologyEventCollector monitor) {
    OperatorEventsPassiveServerConnectionListener delegate = new OperatorEventsPassiveServerConnectionListener(nodesStore);
    return new GroupEventsListener() {

      @Override
      public void nodeJoined(NodeID nodeID) {
        if (l2Coordinator.getStateManager().isActiveCoordinator()) {
          delegate.passiveServerJoined((ServerID)nodeID);
          PlatformInfoRequest req = new PlatformInfoRequest(PlatformInfoRequest.REQUEST);
          req.setRequestType(PlatformInfoRequest.RequestType.SERVER_INFO);
          try {
            groupCommManager.sendTo(nodeID, req);
 // monitor will be updated when the remote server responds with it's info
          } catch (GroupException g) {
            
          }
        }
      }

      @Override
      public void nodeLeft(NodeID nodeID) {
        if (l2Coordinator.getStateManager().isActiveCoordinator()) {
          delegate.passiveServerLeft((ServerID)nodeID);
          monitor.serverDidLeaveGroup((ServerID)nodeID);
        }
      }
    };
  }
  
  private void connectServerStateToReplicatedState(StateManager mgr, ReplicatedClusterStateManager rcs) {
    mgr.registerForStateChangeEvents(new StateChangeListener() {
      @Override
      public void l2StateChanged(StateChangedEvent sce) {
        rcs.setCurrentState(sce.getCurrentState());
        if (sce.movedToActive()) {
          startActiveMode(sce.getOldState().equals(StateManager.PASSIVE_STANDBY));
          try {
            startL1Listener();
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
    messageTypeClassMapping.put(TCMessageType.LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE, ListRegisteredServicesResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE, InvokeRegisteredServiceMessage.class);
    messageTypeClassMapping.put(TCMessageType.INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE, InvokeRegisteredServiceResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_MESSAGE, NetworkVoltronEntityMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE, VoltronEntityReceivedResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_APPLIED_RESPONSE, VoltronEntityAppliedResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.VOLTRON_ENTITY_RETIRED_RESPONSE, VoltronEntityRetiredResponseImpl.class);
    messageTypeClassMapping.put(TCMessageType.SERVER_ENTITY_MESSAGE, ServerEntityMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SERVER_ENTITY_RESPONSE_MESSAGE, ServerEntityResponseMessageImpl.class);
    return messageTypeClassMapping;
  }

  protected TCLogger getLogger() {
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

  public boolean isBlocking() {
    return this.startupLock != null && this.startupLock.isBlocked();
  }

  public void startActiveMode(boolean wasStandby) {
    if (!wasStandby && persistor.getClusterStatePersistor().getInitialState() == null) {
      Sink<VoltronEntityMessage> msgSink = this.seda.getStageManager().getStage(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE, VoltronEntityMessage.class).getSink();
      Map<EntityID, VoltronEntityMessage> checkdups = new HashMap<>();
//  find annotated permanent entities
      List<VoltronEntityMessage> annotated = ServerEntityFactory.getAnnotatedEntities(entityManager.getEntityLoader());
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
    }
  }

  public void startL1Listener() throws IOException {
    final Set<ConnectionID> existingConnections = Collections.unmodifiableSet(this.connectionIdFactory.loadConnectionIDs());
    this.context.getClientHandshakeManager().setStarting(existingConnections);
    this.l1Listener.start(existingConnections);
    if (!existingConnections.isEmpty()) {
      this.context.getClientHandshakeManager().startReconnectWindow();
    }
    consoleLogger.info("Terracotta Server instance has started up as ACTIVE node on " + format(this.l1Listener)
                       + " successfully, and is now ready for work.");
  }

  private static String format(NetworkListener listener) {
    final StringBuilder sb = new StringBuilder(listener.getBindAddress().getHostAddress());
    sb.append(':');
    sb.append(listener.getBindPort());
    return sb.toString();
  }

  public boolean stopActiveMode() throws TCTimeoutException {
    // TODO:: Make this not take timeout and force stop
    consoleLogger.info("Stopping ACTIVE Terracotta Server instance on " + format(this.l1Listener) + ".");
    this.l1Listener.stop(10000);
    this.l1Listener.getChannelManager().closeAllChannels();
    return true;
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

  public synchronized void stop() {

    this.seda.getStageManager().stopAll();

    if (this.l1Listener != null) {
      try {
        this.l1Listener.stop(5000);
      } catch (final TCTimeoutException e) {
        logger.warn("timeout trying to stop listener: " + e.getMessage());
      }
    }

    if ((this.communicationsManager != null)) {
      this.communicationsManager.shutdown();
    }

    try {
      this.persistor.close();
    } catch (final Exception e) {
      logger.warn(e);
    }

    if (this.sampledCounterManager != null) {
      try {
        this.sampledCounterManager.shutdown();
      } catch (final Exception e) {
        logger.error(e);
      }
    }

    basicStop();
  }

  public void quickStop() {
    basicStop();
  }

  private void basicStop() {
    if (this.startupLock != null) {
      this.startupLock.release();
    }
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
  
  public TerracottaOperatorEventHistoryProvider getOperatorEventsHistoryProvider() {
    return this.operatorEventHistoryProvider;
  }

  @Override
  public void addAllLocksTo(LockInfoByThreadID lockInfo) {
    // this feature not implemented for server. DEV-1949
  }

  @Override
  public ThreadIDMap getThreadIDMap() {
    return new NullThreadIDMapImpl();
  }

  protected GroupManager<AbstractGroupMessage> getGroupManager() {
    return this.groupCommManager;
  }

  @Override
  public void registerForDump(CallbackDumpAdapter dumpAdapter) {
    this.dumpHandler.registerForDump(dumpAdapter);
  }

  @Override
  public boolean isAlive(String name) {
    throw new UnsupportedOperationException();
  }

  protected ClientHandshakeHandler createHandShakeHandler() {
    return new ClientHandshakeHandler(this.configSetupManager.dsoL2Config().serverName());
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
