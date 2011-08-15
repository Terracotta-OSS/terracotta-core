/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.async.api.Stage;
import com.tc.exception.ImplementMe;
import com.tc.l2.api.L2Coordinator;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
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

import java.util.HashMap;
import java.util.Map;

public class TestServerConfigurationContext implements ServerConfigurationContext {

  public ObjectManager                  objectManager;
  public ObjectRequestManager           objectRequestManager;
  public LockManager                    lockManager;
  public DSOChannelManager              channelManager;
  public ClientStateManager             clientStateManager;
  public ServerTransactionManager       transactionManager;
  public ManagedObjectStore             objectStore;
  public ServerClientHandshakeManager   clientHandshakeManager;
  public Map                            stages = new HashMap();
  public ChannelStats                   channelStats;
  public TransactionalObjectManager     txnObjectManager;
  public L2Coordinator                  l2Coordinator;
  public TransactionBatchManager        transactionBatchManager;
  public ServerGlobalTransactionManager serverGlobalTransactionManager;
  public ServerClusterMetaDataManager   clusterMetaDataManager;

  public void addStage(final String name, final Stage stage) {
    stages.put(name, stage);
  }

  public ObjectManager getObjectManager() {
    return this.objectManager;
  }

  public ObjectRequestManager getObjectRequestManager() {
    return this.objectRequestManager;
  }

  public void setObjectRequestManager(final ObjectRequestManager objectRequestManager) {
    this.objectRequestManager = objectRequestManager;
  }

  public LockManager getLockManager() {
    return this.lockManager;
  }

  public DSOChannelManager getChannelManager() {
    return this.channelManager;
  }

  public ClientStateManager getClientStateManager() {
    return this.clientStateManager;
  }

  public ServerTransactionManager getTransactionManager() {
    return this.transactionManager;
  }

  public ManagedObjectStore getObjectStore() {
    return this.objectStore;
  }

  public ServerClientHandshakeManager getClientHandshakeManager() {
    return this.clientHandshakeManager;
  }

  public Stage getStage(final String name) {
    return (Stage) this.stages.get(name);
  }

  public TCLogger getLogger(final Class clazz) {
    return TCLogging.getLogger(getClass());
  }

  public ChannelStats getChannelStats() {
    return this.channelStats;
  }

  public TransactionBatchReaderFactory getTransactionBatchReaderFactory() {
    throw new ImplementMe();
  }

  public TransactionalObjectManager getTransactionalObjectManager() {
    return txnObjectManager;
  }

  public L2Coordinator getL2Coordinator() {
    return l2Coordinator;
  }

  public TransactionBatchManager getTransactionBatchManager() {
    return transactionBatchManager;
  }

  public ServerGlobalTransactionManager getServerGlobalTransactionManager() {
    return serverGlobalTransactionManager;
  }

  public ServerClusterMetaDataManager getClusterMetaDataManager() {
    return clusterMetaDataManager;
  }

  public ServerMapRequestManager getServerMapRequestManager() {
    throw new ImplementMe();
  }

  public IndexManager getIndexManager() {
    throw new ImplementMe();
  }

  public MetaDataManager getMetaDataManager() {
    throw new ImplementMe();
  }

  public SearchRequestManager getSearchRequestManager() {
    throw new ImplementMe();
  }

  public GarbageCollectionManager getGarbageCollectionManager() {
    throw new ImplementMe();
  }
}
