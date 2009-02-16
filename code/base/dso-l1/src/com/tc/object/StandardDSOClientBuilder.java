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
import com.tc.object.loaders.ClassProvider;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.impl.ClientLockManagerConfigImpl;
import com.tc.object.lockmanager.impl.RemoteLockManagerImpl;
import com.tc.object.lockmanager.impl.StripedClientLockManagerImpl;
import com.tc.object.logging.RuntimeLogger;
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
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.ToggleableReferenceManager;
import com.tc.util.sequence.BatchSequence;

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

  public ClientLockManager createLockManager(final ClientIDLogger clientIDLogger,
                                             final RemoteLockManagerImpl remoteLockManagerImpl,
                                             final SessionManager sessionManager,
                                             final ClientLockStatManager lockStatManager,
                                             final ClientLockManagerConfigImpl clientLockManagerConfigImpl) {
    return new StripedClientLockManagerImpl(clientIDLogger, remoteLockManagerImpl, sessionManager, lockStatManager,
                                            clientLockManagerConfigImpl);
  }

  public RemoteTransactionManager createRemoteTransactionManager(final ClientIDProvider cidProvider,
                                                                 final DNAEncoding encoding,
                                                                 final FoldingConfig foldingConfig,
                                                                 final TransactionIDGenerator transactionIDGenerator,
                                                                 final SessionManager sessionManager,
                                                                 final DSOClientMessageChannel dsoChannel,
                                                                 final Counter outstandingBatchesCounter,
                                                                 final SampledCounter numTransactionCounter,
                                                                 final SampledCounter numBatchesCounter,
                                                                 final SampledCounter batchSizeCounter,
                                                                 final Counter pendingBatchesSize) {
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
                                            numTransactionCounter,
                                            numBatchesCounter,
                                            batchSizeCounter,
                                            pendingBatchesSize,
                                            TCPropertiesImpl.getProperties()
                                                .getLong(TCPropertiesConsts.L1_TRANSACTIONMANAGER_TIMEOUTFORACK_ONEXIT) * 1000);
  }

  public ObjectIDClientHandshakeRequester getObjectIDClientHandshakeRequester(final BatchSequence sequence) {
    return new ObjectIDClientHandshakeRequester(sequence);
  }

}
