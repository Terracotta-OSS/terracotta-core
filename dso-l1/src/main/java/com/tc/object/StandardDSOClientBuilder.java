/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.async.api.Sink;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L1Management;
import com.tc.management.TCClient;
import com.tc.management.lock.stats.ClientLockStatisticsManagerImpl;
import com.tc.management.remote.protocol.terracotta.TunneledDomainManager;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.net.GroupID;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.GeneratedMessageFactory;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandlerForL1;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.ConnectionInfoConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOMBeanConfig;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.gtx.ClientGlobalTransactionManagerImpl;
import com.tc.object.gtx.PreTransactionFlushCallback;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.handshakemanager.ClientHandshakeManagerImpl;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.idprovider.impl.ObjectIDClientHandshakeRequester;
import com.tc.object.idprovider.impl.ObjectIDProviderImpl;
import com.tc.object.idprovider.impl.RemoteObjectIDBatchSequenceProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.ClientLockManagerConfig;
import com.tc.object.locks.ClientLockManagerImpl;
import com.tc.object.locks.RemoteLockManager;
import com.tc.object.locks.RemoteLockManagerImpl;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.KeysForOrphanedValuesMessageFactory;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.NodeMetaDataMessageFactory;
import com.tc.object.msg.NodesWithKeysMessageFactory;
import com.tc.object.msg.NodesWithObjectsMessageFactory;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.ClientTransactionBatchWriter.FoldingConfig;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.object.tx.RemoteTransactionManagerImpl;
import com.tc.object.tx.TransactionBatchFactory;
import com.tc.object.tx.TransactionBatchWriterFactory;
import com.tc.object.tx.TransactionIDGenerator;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.Assert;
import com.tc.util.ToggleableReferenceManager;
import com.tc.util.runtime.ThreadIDManager;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.BatchSequenceReceiver;
import com.tcclient.cluster.DsoClusterInternalEventsGun;

import java.util.Collection;
import java.util.Map;

public class StandardDSOClientBuilder implements DSOClientBuilder {

  public DSOClientMessageChannel createDSOClientMessageChannel(final CommunicationsManager commMgr,
                                                               final PreparedComponentsFromL2Connection connComp,
                                                               final SessionProvider sessionProvider,
                                                               final int maxReconnectTries,
                                                               final int socketConnectTimeout, final TCClient client) {
    final ConnectionAddressProvider cap = createConnectionAddressProvider(connComp);
    final ClientMessageChannel cmc = commMgr.createClientChannel(sessionProvider, maxReconnectTries, null, 0,
                                                                 socketConnectTimeout, cap);
    return new DSOClientMessageChannelImpl(cmc, new GroupID[] { new GroupID(cap.getGroupId()) });
  }

  protected ConnectionAddressProvider createConnectionAddressProvider(final PreparedComponentsFromL2Connection connComp) {
    final ConnectionInfoConfig connectionInfoItem = connComp.createConnectionInfoConfigItem();
    final ConnectionInfo[] connectionInfo = connectionInfoItem.getConnectionInfos();
    final ConnectionAddressProvider cap = new ConnectionAddressProvider(connectionInfo);
    return cap;
  }

  public CommunicationsManager createCommunicationsManager(final MessageMonitor monitor,
                                                           final TCMessageRouter messageRouter,
                                                           final NetworkStackHarnessFactory stackHarnessFactory,
                                                           final ConnectionPolicy connectionPolicy,
                                                           final int commThread,
                                                           final HealthCheckerConfig aConfig,
                                                           Map<TCMessageType, Class> messageTypeClassMapping,
                                                           Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping,
                                                           ReconnectionRejectedHandler reconnectionRejectediHandler) {
    return new CommunicationsManagerImpl(CommunicationsManager.COMMSMGR_CLIENT, monitor, messageRouter,
                                         stackHarnessFactory, null, connectionPolicy, 0, aConfig,
                                         new TransportHandshakeErrorHandlerForL1(), messageTypeClassMapping,
                                         messageTypeFactoryMapping, reconnectionRejectediHandler);
  }

  public TunnelingEventHandler createTunnelingEventHandler(final ClientMessageChannel ch, final DSOMBeanConfig config) {
    return new TunnelingEventHandler(ch, config);
  }

  public TunneledDomainManager createTunneledDomainManager(final ClientMessageChannel ch, final DSOMBeanConfig config,
                                                           final TunnelingEventHandler teh) {
    return new TunneledDomainManager(ch, config, teh);
  }

  public ClientGlobalTransactionManager createClientGlobalTransactionManager(final RemoteTransactionManager remoteTxnMgr,
                                                                             final PreTransactionFlushCallback preTransactionFlushCallback) {
    return new ClientGlobalTransactionManagerImpl(remoteTxnMgr, preTransactionFlushCallback);
  }

  public RemoteObjectManagerImpl createRemoteObjectManager(final TCLogger logger,
                                                           final DSOClientMessageChannel dsoChannel,
                                                           final int faultCount, final SessionManager sessionManager) {
    final GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    Assert.assertNotNull(defaultGroups);
    Assert.assertEquals(1, defaultGroups.length);
    return new RemoteObjectManagerImpl(defaultGroups[0], logger, dsoChannel.getRequestRootMessageFactory(),
                                       dsoChannel.getRequestManagedObjectMessageFactory(), faultCount, sessionManager);
  }

  public ClusterMetaDataManager createClusterMetaDataManager(final DSOClientMessageChannel dsoChannel,
                                                             final DNAEncoding encoding,
                                                             final ThreadIDManager threadIDManager,
                                                             final NodesWithObjectsMessageFactory nwoFactory,
                                                             final KeysForOrphanedValuesMessageFactory kfovFactory,
                                                             final NodeMetaDataMessageFactory nmdmFactory,
                                                             final NodesWithKeysMessageFactory nwkmFactory) {
    final GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    Assert.assertNotNull(defaultGroups);
    Assert.assertEquals(1, defaultGroups.length);

    return new ClusterMetaDataManagerImpl(defaultGroups[0], encoding, threadIDManager, nwoFactory, kfovFactory,
                                          nmdmFactory, nwkmFactory);
  }

  public ClientObjectManagerImpl createObjectManager(final RemoteObjectManager remoteObjectManager,
                                                     final DSOClientConfigHelper dsoConfig,
                                                     final ObjectIDProvider idProvider, final RuntimeLogger rtLogger,
                                                     final ClientIDProvider clientIDProvider,
                                                     final ClassProvider classProviderLocal,
                                                     final TCClassFactory classFactory,
                                                     final TCObjectFactory objectFactory,
                                                     final Portability portability,
                                                     final DSOClientMessageChannel dsoChannel,
                                                     final ToggleableReferenceManager toggleRefMgr,
                                                     TCObjectSelfStore tcObjectSelfStore) {
    return new ClientObjectManagerImpl(remoteObjectManager, dsoConfig, idProvider, rtLogger, clientIDProvider,
                                       classProviderLocal, classFactory, objectFactory, portability, dsoChannel,
                                       toggleRefMgr, tcObjectSelfStore);
  }

  public ClientLockManager createLockManager(final DSOClientMessageChannel dsoChannel,
                                             final ClientIDLogger clientIDLogger, final SessionManager sessionManager,
                                             final ClientLockStatManager lockStatManager,
                                             final LockRequestMessageFactory lockRequestMessageFactory,
                                             final ThreadIDManager threadManager,
                                             final ClientGlobalTransactionManager gtxManager,
                                             final ClientLockManagerConfig config) {
    final GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    Assert.assertNotNull(defaultGroups);
    Assert.assertEquals(1, defaultGroups.length);
    final RemoteLockManager remoteManager = new RemoteLockManagerImpl(dsoChannel.getClientIDProvider(),
                                                                      defaultGroups[0], lockRequestMessageFactory,
                                                                      gtxManager, lockStatManager);
    return new ClientLockManagerImpl(clientIDLogger, sessionManager, remoteManager, threadManager, config,
                                     lockStatManager);
  }

  @Deprecated
  public ClientLockStatManager createLockStatsManager() {
    return new ClientLockStatisticsManagerImpl(null);
  }

  public RemoteTransactionManager createRemoteTransactionManager(final ClientIDProvider cidProvider,
                                                                 final DNAEncodingInternal encoding,
                                                                 final FoldingConfig foldingConfig,
                                                                 final TransactionIDGenerator transactionIDGenerator,
                                                                 final SessionManager sessionManager,
                                                                 final DSOClientMessageChannel dsoChannel,
                                                                 final Counter outstandingBatchesCounter,
                                                                 final Counter pendingBatchesSize,
                                                                 final SampledRateCounter transactionSizeCounter,
                                                                 final SampledRateCounter transactionsPerBatchCounter) {
    final GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    Assert.assertNotNull(defaultGroups);
    Assert.assertEquals(1, defaultGroups.length);
    final TransactionBatchFactory txBatchFactory = new TransactionBatchWriterFactory(
                                                                                     dsoChannel
                                                                                         .getCommitTransactionMessageFactory(),
                                                                                     encoding, foldingConfig);
    return new RemoteTransactionManagerImpl(
                                            defaultGroups[0],
                                            new ClientIDLogger(cidProvider, TCLogging
                                                .getLogger(RemoteTransactionManagerImpl.class)),
                                            txBatchFactory,
                                            transactionIDGenerator,
                                            sessionManager,
                                            dsoChannel,
                                            outstandingBatchesCounter,
                                            pendingBatchesSize,
                                            transactionSizeCounter,
                                            transactionsPerBatchCounter,
                                            TCPropertiesImpl.getProperties()
                                                .getLong(TCPropertiesConsts.L1_TRANSACTIONMANAGER_TIMEOUTFORACK_ONEXIT) * 1000);
  }

  public ObjectIDClientHandshakeRequester getObjectIDClientHandshakeRequester(final BatchSequenceReceiver sequence) {
    return new ObjectIDClientHandshakeRequester(sequence);
  }

  public BatchSequence[] createSequences(final RemoteObjectIDBatchSequenceProvider remoteIDProvider,
                                         final int requestSize) {
    return new BatchSequence[] { new BatchSequence(remoteIDProvider, requestSize) };
  }

  public ObjectIDProvider createObjectIdProvider(final BatchSequence[] sequences, final ClientIDProvider cidProvider) {
    Assert.assertTrue(sequences.length == 1);

    return new ObjectIDProviderImpl(sequences[0]);
  }

  public BatchSequenceReceiver getBatchReceiver(final BatchSequence[] sequences) {
    Assert.assertTrue(sequences.length == 1);
    return sequences[0];
  }

  public ClientHandshakeManager createClientHandshakeManager(final TCLogger logger,
                                                             final DSOClientMessageChannel channel,
                                                             final ClientHandshakeMessageFactory chmf,
                                                             final Sink pauseSink, final SessionManager sessionManager,
                                                             final DsoClusterInternalEventsGun dsoClusterEventsGun,
                                                             final String clientVersion,
                                                             final Collection<ClientHandshakeCallback> callbacks) {
    return new ClientHandshakeManagerImpl(logger, channel, chmf, pauseSink, sessionManager, dsoClusterEventsGun,
                                          clientVersion, callbacks);
  }

  public L1Management createL1Management(final TunnelingEventHandler teh,
                                         final StatisticsAgentSubSystem statisticsAgentSubSystem,
                                         final RuntimeLogger runtimeLogger,
                                         final InstrumentationLogger instrumentationLogger, final String rawConfigText,
                                         final DistributedObjectClient distributedObjectClient) {
    return new L1Management(teh, statisticsAgentSubSystem, runtimeLogger, instrumentationLogger, rawConfigText,
                            distributedObjectClient);
  }

  public void registerForOperatorEvents(final L1Management management) {
    // NOP
  }

  public TCClassFactory createTCClassFactory(final DSOClientConfigHelper config, final ClassProvider classProvider,
                                             final DNAEncoding dnaEncoding, final Manager manager,
                                             final L1ServerMapLocalCacheManager localCacheManager,
                                             final RemoteServerMapManager remoteServerMapManager) {
    return new TCClassFactoryImpl(new TCFieldFactory(config), config, classProvider, dnaEncoding, manager,
                                  localCacheManager, remoteServerMapManager);
  }

  public RemoteServerMapManager createRemoteServerMapManager(final TCLogger logger,
                                                             final DSOClientMessageChannel dsoChannel,
                                                             final SessionManager sessionManager,
                                                             final L1ServerMapLocalCacheManager globalLocalCacheManager) {
    final GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    Assert.assertNotNull(defaultGroups);
    Assert.assertEquals(1, defaultGroups.length);
    return new RemoteServerMapManagerImpl(defaultGroups[0], logger, dsoChannel.getServerMapMessageFactory(),
                                          sessionManager, globalLocalCacheManager);
  }

  public RemoteSearchRequestManager createRemoteSearchRequestManager(final TCLogger logger,
                                                                     final DSOClientMessageChannel dsoChannel,
                                                                     final SessionManager sessionManager) {
    return new NullRemoteSearchRequestManager();
  }

  public LongGCLogger createLongGCLogger(long gcTimeOut) {
    return new LongGCLogger(gcTimeOut);
  }

}
