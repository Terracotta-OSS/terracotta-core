/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.logging.ClientIDLogger;
import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.cache.ClockEvictionPolicy;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.idprovider.impl.ObjectIDClientHandshakeRequester;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.impl.ClientLockManagerConfigImpl;
import com.tc.object.lockmanager.impl.RemoteLockManagerImpl;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.object.tx.TransactionIDGenerator;
import com.tc.object.tx.TransactionBatchWriter.FoldingConfig;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.ToggleableReferenceManager;
import com.tc.util.sequence.BatchSequence;

public interface DSOClientBuilder {

  DSOClientMessageChannel createDSOClientMessageChannel(final CommunicationsManager commMgr,
                                                        final PreparedComponentsFromL2Connection connComp,
                                                        final SessionProvider sessionProvider);

  CommunicationsManager createCommunicationsManager(final MessageMonitor monitor,
                                                    final NetworkStackHarnessFactory stackHarnessFactory,
                                                    final ConnectionPolicy connectionPolicy,
                                                    final HealthCheckerConfig hcConfig);

  TunnelingEventHandler createTunnelingEventHandler(final ClientMessageChannel ch);

  ClientGlobalTransactionManager createClientGlobalTransactionManager(final RemoteTransactionManager remoteTxnMgr);

  RemoteObjectManager createRemoteObjectManager(final TCLogger logger, final DSOClientMessageChannel dsoChannel,
                                                final ObjectRequestMonitor objectRequestMonitor, final int faultCount,
                                                final SessionManager sessionManager);

  ClientObjectManagerImpl createObjectManager(final RemoteObjectManager remoteObjectManager,
                                              final DSOClientConfigHelper dsoConfig, final ObjectIDProvider idProvider,
                                              final ClockEvictionPolicy clockEvictionPolicy,
                                              final RuntimeLogger rtLogger, final ClientIDProvider clientIDProvider,
                                              final ClassProvider classProviderLocal,
                                              final TCClassFactory classFactory, final TCObjectFactory objectFactory,
                                              final Portability portability, final DSOClientMessageChannel dsoChannel,
                                              final ToggleableReferenceManager toggleRefMgr);

  ClientLockManager createLockManager(final ClientIDLogger clientIDLogger,
                                      final RemoteLockManagerImpl remoteLockManagerImpl,
                                      final SessionManager sessionManager, final ClientLockStatManager lockStatManager,
                                      final ClientLockManagerConfigImpl clientLockManagerConfigImpl);

  RemoteTransactionManager createRemoteTransactionManager(final ClientIDProvider cidProvider,
                                                          final DNAEncoding encoding,
                                                          final FoldingConfig foldingConfig,
                                                          final TransactionIDGenerator transactionIDGenerator,
                                                          final SessionManager sessionManager,
                                                          final DSOClientMessageChannel dsoChannel,
                                                          final Counter outstandingBatchesCounter,
                                                          final Counter pendingBatchesSize,
                                                          SampledRateCounter transactionSizeCounter,
                                                          SampledRateCounter transactionPerBatchCounter);

  ObjectIDClientHandshakeRequester getObjectIDClientHandshakeRequester(final BatchSequence sequence);

}
