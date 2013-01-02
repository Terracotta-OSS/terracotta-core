/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.l2.api.L2Coordinator;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.clustermetadata.ServerClusterMetaDataManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.impl.PersistentManagedObjectStore;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.metadata.MetaDataManager;
import com.tc.objectserver.search.IndexManager;
import com.tc.objectserver.search.SearchRequestManager;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;
import com.tc.objectserver.tx.TransactionalObjectManager;

/**
 * App specific configuration context
 * 
 * @author steve
 */
public class ServerConfigurationContextImpl extends ConfigurationContextImpl implements ServerConfigurationContext {

  private final ObjectManager                  objectManager;
  private final ObjectRequestManager           objectRequestManager;
  private final ServerMapRequestManager        serverMapRequestManager;
  private final LockManager                    lockManager;
  private final DSOChannelManager              channelManager;
  private final ClientStateManager             clientStateManager;
  private final ServerTransactionManager       transactionManager;
  private final PersistentManagedObjectStore             persistor;
  private final ServerClientHandshakeManager   clientHandshakeManager;
  private final ChannelStats                   channelStats;
  private final TransactionBatchReaderFactory  transactionBatchReaderFactory;
  private final TransactionalObjectManager     txnObjectManager;
  private final L2Coordinator                  l2Coordinator;
  private final TransactionBatchManager        transactionBatchManager;
  private final ServerGlobalTransactionManager serverGlobalTransactionManager;
  private final ServerClusterMetaDataManager   serverClusterMetaDataManager;
  private final MetaDataManager                metaDataManager;
  private final IndexManager                   indexManager;
  private final SearchRequestManager           searchRequestManager;
  private final GarbageCollectionManager       garbageCollectionManager;

  public ServerConfigurationContextImpl(final StageManager stageManager, final ObjectManager objectManager,
                                        final ObjectRequestManager objectRequestManager,
                                        final ServerMapRequestManager serverTCMapRequestManager,
                                        final PersistentManagedObjectStore objectStore, final LockManager lockManager,
                                        final DSOChannelManager channelManager,
                                        final ClientStateManager clientStateManager,
                                        final ServerTransactionManager transactionManager,
                                        final TransactionalObjectManager txnObjectManager,
                                        final ServerClientHandshakeManager clientHandshakeManager,
                                        final ChannelStats channelStats, final L2Coordinator l2Coordinator,
                                        final TransactionBatchReaderFactory transactionBatchReaderFactory,
                                        final TransactionBatchManager transactionBatchManager,
                                        final ServerGlobalTransactionManager serverGlobalTransactionManager,
                                        final ServerClusterMetaDataManager serverClusterMetaDataManager,
                                        final MetaDataManager metaDataManager, final IndexManager indexManager,
                                        final SearchRequestManager searchRequestManager,
                                        final GarbageCollectionManager garbageCollectionManager) {
    super(stageManager);
    this.objectManager = objectManager;
    this.objectRequestManager = objectRequestManager;
    this.serverMapRequestManager = serverTCMapRequestManager;
    this.persistor = objectStore;
    this.lockManager = lockManager;
    this.channelManager = channelManager;
    this.clientStateManager = clientStateManager;
    this.transactionManager = transactionManager;
    this.txnObjectManager = txnObjectManager;
    this.clientHandshakeManager = clientHandshakeManager;
    this.channelStats = channelStats;
    this.l2Coordinator = l2Coordinator;
    this.transactionBatchReaderFactory = transactionBatchReaderFactory;
    this.transactionBatchManager = transactionBatchManager;
    this.serverGlobalTransactionManager = serverGlobalTransactionManager;
    this.serverClusterMetaDataManager = serverClusterMetaDataManager;
    this.metaDataManager = metaDataManager;
    this.indexManager = indexManager;
    this.searchRequestManager = searchRequestManager;
    this.garbageCollectionManager = garbageCollectionManager;
  }

  @Override
  public L2Coordinator getL2Coordinator() {
    return l2Coordinator;
  }

  @Override
  public ObjectManager getObjectManager() {
    return objectManager;
  }

  @Override
  public ObjectRequestManager getObjectRequestManager() {
    return objectRequestManager;
  }

  @Override
  public ServerMapRequestManager getServerMapRequestManager() {
    return serverMapRequestManager;
  }

  @Override
  public LockManager getLockManager() {
    return lockManager;
  }

  @Override
  public DSOChannelManager getChannelManager() {
    return channelManager;
  }

  @Override
  public ClientStateManager getClientStateManager() {
    return clientStateManager;
  }

  @Override
  public ServerTransactionManager getTransactionManager() {
    return transactionManager;
  }

  @Override
  public TransactionalObjectManager getTransactionalObjectManager() {
    return txnObjectManager;
  }

  @Override
  public PersistentManagedObjectStore getObjectStore() {
    return this.persistor;
  }

  @Override
  public ServerClientHandshakeManager getClientHandshakeManager() {
    return clientHandshakeManager;
  }

  @Override
  public ChannelStats getChannelStats() {
    return this.channelStats;
  }

  @Override
  public TransactionBatchReaderFactory getTransactionBatchReaderFactory() {
    return this.transactionBatchReaderFactory;
  }

  @Override
  public TransactionBatchManager getTransactionBatchManager() {
    return this.transactionBatchManager;
  }

  @Override
  public ServerGlobalTransactionManager getServerGlobalTransactionManager() {
    return this.serverGlobalTransactionManager;
  }

  @Override
  public ServerClusterMetaDataManager getClusterMetaDataManager() {
    return serverClusterMetaDataManager;
  }

  @Override
  public MetaDataManager getMetaDataManager() {
    return metaDataManager;
  }

  @Override
  public IndexManager getIndexManager() {
    return indexManager;
  }

  @Override
  public SearchRequestManager getSearchRequestManager() {
    return searchRequestManager;
  }

  @Override
  public GarbageCollectionManager getGarbageCollectionManager() {
    return garbageCollectionManager;
  }
}