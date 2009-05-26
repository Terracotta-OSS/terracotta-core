/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.ClientLockStatManager;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.net.GroupID;
import com.tc.net.OrderedGroupIDs;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.cache.ClockEvictionPolicy;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.gtx.ClientGlobalTransactionManagerImpl;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.idprovider.impl.ObjectIDClientHandshakeRequester;
import com.tc.object.idprovider.impl.ObjectIDProviderImpl;
import com.tc.object.idprovider.impl.RemoteObjectIDBatchSequenceProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.RemoteLockManager;
import com.tc.object.lockmanager.impl.ClientLockManagerConfigImpl;
import com.tc.object.lockmanager.impl.LockDistributionStrategy;
import com.tc.object.lockmanager.impl.RemoteLockManagerImpl;
import com.tc.object.lockmanager.impl.StandardLockDistributionStrategy;
import com.tc.object.lockmanager.impl.StripedClientLockManagerImpl;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.msg.KeysForOrphanedValuesMessageFactory;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.NodeMetaDataMessageFactory;
import com.tc.object.msg.NodesWithObjectsMessageFactory;
import com.tc.object.net.DSOClientMessageChannel;
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
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.Assert;
import com.tc.util.ToggleableReferenceManager;
import com.tc.util.runtime.ThreadIDManager;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.BatchSequenceReceiver;

public class StandardDSOClientBuilder implements DSOClientBuilder {

  public DSOClientMessageChannel createDSOClientMessageChannel(final CommunicationsManager commMgr,
                                                               final PreparedComponentsFromL2Connection connComp,
                                                               final SessionProvider sessionProvider) {
    ClientMessageChannel cmc;
    ConfigItem connectionInfoItem = connComp.createConnectionInfoConfigItem();
    ConnectionInfo[] connectionInfo = (ConnectionInfo[]) connectionInfoItem.getObject();
    ConnectionAddressProvider cap = new ConnectionAddressProvider(connectionInfo);
    cmc = commMgr.createClientChannel(sessionProvider, -1, null, 0, 10000, cap);
    return new DSOClientMessageChannelImpl(cmc, new GroupID[] { new GroupID(cap.getGroupId()) });
  }

  public CommunicationsManager createCommunicationsManager(final MessageMonitor monitor,
                                                           final NetworkStackHarnessFactory stackHarnessFactory,
                                                           final ConnectionPolicy connectionPolicy,
                                                           final HealthCheckerConfig aConfig) {
    return new CommunicationsManagerImpl(monitor, stackHarnessFactory, connectionPolicy, aConfig);
  }

  public TunnelingEventHandler createTunnelingEventHandler(final ClientMessageChannel ch) {
    return new TunnelingEventHandler(ch);
  }

  public ClientGlobalTransactionManager createClientGlobalTransactionManager(final RemoteTransactionManager remoteTxnMgr) {
    return new ClientGlobalTransactionManagerImpl(remoteTxnMgr);
  }

  public RemoteObjectManager createRemoteObjectManager(final TCLogger logger, final DSOClientMessageChannel dsoChannel,
                                                       final ObjectRequestMonitor objectRequestMonitor,
                                                       final int faultCount, final SessionManager sessionManager) {
    GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    assert defaultGroups != null && defaultGroups.length == 1;
    return new RemoteObjectManagerImpl(defaultGroups[0], logger, dsoChannel.getClientIDProvider(), dsoChannel
        .getRequestRootMessageFactory(), dsoChannel.getRequestManagedObjectMessageFactory(), objectRequestMonitor,
                                       faultCount, sessionManager);
  }

  public ClusterMetaDataManager createClusterMetaDataManager(final DSOClientMessageChannel dsoChannel,
                                                             final DNAEncoding encoding,
                                                             final ThreadIDManager threadIDManager,
                                                             final NodesWithObjectsMessageFactory nwoFactory,
                                                             final KeysForOrphanedValuesMessageFactory kfovFactory,
                                                             final NodeMetaDataMessageFactory nmdmFactory) {
    GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    assert defaultGroups != null && defaultGroups.length == 1;
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
                                             final ClientIDLogger clientIDLogger,
                                             final RemoteLockManager remoteLockManager,
                                             final SessionManager sessionManager,
                                             final ClientLockStatManager lockStatManager,
                                             final ClientLockManagerConfigImpl clientLockManagerConfigImpl) {
    GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    assert defaultGroups != null && defaultGroups.length == 1;
    LockDistributionStrategy strategy = new StandardLockDistributionStrategy(defaultGroups[0]);
    return new StripedClientLockManagerImpl(strategy, new OrderedGroupIDs(defaultGroups), clientIDLogger,
                                            remoteLockManager, sessionManager, lockStatManager,
                                            clientLockManagerConfigImpl);
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
    GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    assert defaultGroups != null && defaultGroups.length == 1;
    TransactionBatchFactory txBatchFactory = new TransactionBatchWriterFactory(dsoChannel
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

  public RemoteLockManager createRemoteLockManager(final DSOClientMessageChannel dsoChannel,
                                                   final LockRequestMessageFactory lockRequestMessageFactory,
                                                   final ClientGlobalTransactionManager gtxManager) {
    GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    assert defaultGroups != null && defaultGroups.length == 1;
    return new RemoteLockManagerImpl(defaultGroups[0], lockRequestMessageFactory, gtxManager);
  }

}
