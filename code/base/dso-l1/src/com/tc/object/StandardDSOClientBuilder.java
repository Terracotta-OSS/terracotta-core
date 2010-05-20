/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.async.api.Sink;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L1Management;
import com.tc.management.TCClient;
import com.tc.management.lock.stats.ClientLockStatisticsManagerImpl;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.cache.CachedItem;
import com.tc.object.cache.ClockEvictionPolicy;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.MBeanSpec;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.gtx.ClientGlobalTransactionManagerImpl;
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
import com.tc.object.locks.LockID;
import com.tc.object.locks.RemoteLockManager;
import com.tc.object.locks.RemoteLockManagerImpl;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.KeysForOrphanedValuesMessageFactory;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.NodeMetaDataMessageFactory;
import com.tc.object.msg.NodesWithObjectsMessageFactory;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.object.tx.RemoteTransactionManagerImpl;
import com.tc.object.tx.TransactionBatchFactory;
import com.tc.object.tx.TransactionBatchWriterFactory;
import com.tc.object.tx.TransactionIDGenerator;
import com.tc.object.tx.TransactionBatchWriter.FoldingConfig;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.Assert;
import com.tc.util.ToggleableReferenceManager;
import com.tc.util.UUID;
import com.tc.util.runtime.ThreadIDManager;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.BatchSequenceReceiver;
import com.tcclient.cluster.DsoClusterInternal;

import java.util.Collection;

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
    final ConfigItem connectionInfoItem = connComp.createConnectionInfoConfigItem();
    final ConnectionInfo[] connectionInfo = (ConnectionInfo[]) connectionInfoItem.getObject();
    final ConnectionAddressProvider cap = new ConnectionAddressProvider(connectionInfo);
    return cap;
  }

  public CommunicationsManager createCommunicationsManager(final MessageMonitor monitor,
                                                           final NetworkStackHarnessFactory stackHarnessFactory,
                                                           final ConnectionPolicy connectionPolicy,
                                                           final int commThread, final HealthCheckerConfig aConfig) {
    return new CommunicationsManagerImpl(CommunicationsManager.COMMSMGR_CLIENT, monitor, stackHarnessFactory,
                                         connectionPolicy, aConfig);
  }

  public TunnelingEventHandler createTunnelingEventHandler(final ClientMessageChannel ch, final UUID id) {
    return new TunnelingEventHandler(ch, id);
  }

  public ClientGlobalTransactionManager createClientGlobalTransactionManager(
                                                                             final RemoteTransactionManager remoteTxnMgr,
                                                                             final RemoteServerMapManager remoteServerMapManager) {
    return new ClientGlobalTransactionManagerImpl(remoteTxnMgr, remoteServerMapManager);
  }

  public RemoteObjectManagerImpl createRemoteObjectManager(final TCLogger logger,
                                                           final DSOClientMessageChannel dsoChannel,
                                                           final int faultCount, final SessionManager sessionManager) {
    final GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    Assert.assertNotNull(defaultGroups);
    Assert.assertEquals(1, defaultGroups.length);
    return new RemoteObjectManagerImpl(defaultGroups[0], logger, dsoChannel.getRequestRootMessageFactory(), dsoChannel
        .getRequestManagedObjectMessageFactory(), faultCount, sessionManager);
  }

  public ClusterMetaDataManager createClusterMetaDataManager(final DSOClientMessageChannel dsoChannel,
                                                             final DNAEncoding encoding,
                                                             final ThreadIDManager threadIDManager,
                                                             final NodesWithObjectsMessageFactory nwoFactory,
                                                             final KeysForOrphanedValuesMessageFactory kfovFactory,
                                                             final NodeMetaDataMessageFactory nmdmFactory) {
    final GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    Assert.assertNotNull(defaultGroups);
    Assert.assertEquals(1, defaultGroups.length);

    return new ClusterMetaDataManagerImpl(defaultGroups[0], encoding, threadIDManager, nwoFactory, kfovFactory,
                                          nmdmFactory);
  }

  public ClientObjectManagerImpl createObjectManager(final RemoteObjectManager remoteObjectManager,
                                                     final DSOClientConfigHelper dsoConfig,
                                                     final ObjectIDProvider idProvider,
                                                     final ClockEvictionPolicy clockEvictionPolicy,
                                                     final RuntimeLogger rtLogger,
                                                     final ClientIDProvider clientIDProvider,
                                                     final ClassProvider classProviderLocal,
                                                     final TCClassFactory classFactory,
                                                     final TCObjectFactory objectFactory,
                                                     final Portability portability,
                                                     final DSOClientMessageChannel dsoChannel,
                                                     final ToggleableReferenceManager toggleRefMgr) {
    return new ClientObjectManagerImpl(remoteObjectManager, dsoConfig, idProvider, clockEvictionPolicy, rtLogger,
                                       clientIDProvider, classProviderLocal, classFactory, objectFactory, portability,
                                       dsoChannel, toggleRefMgr);
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
                                                                 final DNAEncoding encoding,
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
    final TransactionBatchFactory txBatchFactory = new TransactionBatchWriterFactory(dsoChannel
        .getCommitTransactionMessageFactory(), encoding, foldingConfig);
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
                                                             final DsoClusterInternal dsoCluster,
                                                             final String clientVersion,
                                                             final Collection<ClientHandshakeCallback> callbacks) {
    return new ClientHandshakeManagerImpl(logger, channel, chmf, pauseSink, sessionManager, dsoCluster, clientVersion,
                                          callbacks);
  }

  public L1Management createL1Management(final TunnelingEventHandler teh,
                                         final StatisticsAgentSubSystem statisticsAgentSubSystem,
                                         final RuntimeLogger runtimeLogger,
                                         final InstrumentationLogger instrumentationLogger, final String rawConfigText,
                                         final DistributedObjectClient distributedObjectClient,
                                         final MBeanSpec[] mBeanSpecs) {
    return new L1Management(teh, statisticsAgentSubSystem, runtimeLogger, instrumentationLogger, rawConfigText,
                            distributedObjectClient, mBeanSpecs);
  }

  public TCClassFactory createTCClassFactory(final DSOClientConfigHelper config, final ClassProvider classProvider,
                                             final DNAEncoding dnaEncoding,
                                             final Manager manager,
                                             final RemoteServerMapManager remoteServerMapManager) {
    return new TCClassFactoryImpl(new TCFieldFactory(config), config, classProvider, dnaEncoding);
  }

  public RemoteServerMapManager createRemoteServerMapManager(final TCLogger logger,
                                                             final DSOClientMessageChannel dsoChannel,
                                                             final SessionManager sessionManager) {
    return new RemoteServerMapManager() {

      public void unpause(final NodeID remoteNode, final int disconnected) {
        //
      }

      public void shutdown() {
        //
      }

      public void pause(final NodeID remoteNode, final int disconnected) {
        //
      }

      public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                      final ClientHandshakeMessage handshakeMessage) {
        //
      }

      public void removeCachedItemForLock(final LockID lockID, final CachedItem item) {
        //
      }

      public int getSize(final ObjectID mapID) {
        return -1;
      }

      public Object getMappingForKey(final ObjectID oid, final Object portableKey) {
        return null;
      }

      public void flush(final LockID lockID) {
        //
      }

      public void addResponseForKeyValueMapping(final SessionID localSessionID, final ObjectID mapID,
                                                final Collection<ServerMapGetValueResponse> responses,
                                                final NodeID nodeID) {
        //
      }

      public void addResponseForGetSize(final SessionID localSessionID, final ObjectID mapID,
                                        final ServerMapRequestID requestID, final Integer size,
                                        final NodeID sourceNodeID) {
        //
      }

      public void addCachedItemForLock(final LockID lockID, final CachedItem item) {
        //
      }
    };
  }

}
