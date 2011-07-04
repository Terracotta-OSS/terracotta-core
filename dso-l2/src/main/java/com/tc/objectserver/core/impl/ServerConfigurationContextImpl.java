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
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.clustermetadata.ServerClusterMetaDataManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.metadata.MetaDataManager;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
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
  private final ManagedObjectStore             objectStore;
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

  public ServerConfigurationContextImpl(final StageManager stageManager, final ObjectManager objectManager,
                                        final ObjectRequestManager objectRequestManager,
                                        final ServerMapRequestManager serverTCMapRequestManager,
                                        final ManagedObjectStore objectStore, final LockManager lockManager,
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
                                        final MetaDataManager metaDataManager,
                                        final IndexManager indexManager,
                                        final SearchRequestManager searchRequestManager) {
    super(stageManager);
    this.objectManager = objectManager;
    this.objectRequestManager = objectRequestManager;
    this.serverMapRequestManager = serverTCMapRequestManager;
    this.objectStore = objectStore;
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
  }

  public L2Coordinator getL2Coordinator() {
    return l2Coordinator;
  }

  public ObjectManager getObjectManager() {
    return objectManager;
  }

  public ObjectRequestManager getObjectRequestManager() {
    return objectRequestManager;
  }

  public ServerMapRequestManager getServerMapRequestManager() {
    return serverMapRequestManager;
  }

  public LockManager getLockManager() {
    return lockManager;
  }

  public DSOChannelManager getChannelManager() {
    return channelManager;
  }

  public ClientStateManager getClientStateManager() {
    return clientStateManager;
  }

  public ServerTransactionManager getTransactionManager() {
    return transactionManager;
  }

  public TransactionalObjectManager getTransactionalObjectManager() {
    return txnObjectManager;
  }

  public ManagedObjectStore getObjectStore() {
    return this.objectStore;
  }

  public ServerClientHandshakeManager getClientHandshakeManager() {
    return clientHandshakeManager;
  }

  public ChannelStats getChannelStats() {
    return this.channelStats;
  }

  public TransactionBatchReaderFactory getTransactionBatchReaderFactory() {
    return this.transactionBatchReaderFactory;
  }

  public TransactionBatchManager getTransactionBatchManager() {
    return this.transactionBatchManager;
  }

  public ServerGlobalTransactionManager getServerGlobalTransactionManager() {
    return this.serverGlobalTransactionManager;
  }

  public ServerClusterMetaDataManager getClusterMetaDataManager() {
    return serverClusterMetaDataManager;
  }

  public MetaDataManager getMetaDataManager() {
    return metaDataManager;
  }

  public IndexManager getIndexManager() {
    return indexManager;
  }
  
  public SearchRequestManager getSearchRequestManager() {
    return searchRequestManager;
  }
  
  
}