/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.tx.BatchedTransactionProcessor;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;

/**
 * App specific configuration context
 * 
 * @author steve
 */
public class ServerConfigurationContextImpl extends ConfigurationContextImpl implements ServerConfigurationContext {

  private final ObjectManager                 objectManager;
  private final LockManager                   lockManager;
  private final DSOChannelManager             channelManager;
  private final ClientStateManager            clientStateManager;
  private final ServerTransactionManager      transactionManager;
  private final ManagedObjectStore            objectStore;
  private final ServerClientHandshakeManager  clientHandshakeManager;
  private final ChannelStats                  channelStats;
  private final TransactionBatchReaderFactory transactionBatchReaderFactory;
  private final ObjectRequestManager          objectRequestManager;
  private final BatchedTransactionProcessor   txnProcessor;

  public ServerConfigurationContextImpl(StageManager stageManager, ObjectManager objectManager,
                                        ObjectRequestManager objectRequestManager, ManagedObjectStore objectStore,
                                        LockManager lockManager, DSOChannelManager channelManager,
                                        ClientStateManager clientStateManager,
                                        ServerTransactionManager transactionManager,
                                        BatchedTransactionProcessor txnProcessor,
                                        ServerClientHandshakeManager clientHandshakeManager, ChannelStats channelStats,
                                        TransactionBatchReaderFactory transactionBatchReaderFactory) {
    super(stageManager);
    this.objectManager = objectManager;
    this.objectRequestManager = objectRequestManager;
    this.objectStore = objectStore;
    this.lockManager = lockManager;
    this.channelManager = channelManager;
    this.clientStateManager = clientStateManager;
    this.transactionManager = transactionManager;
    this.txnProcessor = txnProcessor;
    this.clientHandshakeManager = clientHandshakeManager;
    this.channelStats = channelStats;
    this.transactionBatchReaderFactory = transactionBatchReaderFactory;
  }

  public ObjectManager getObjectManager() {
    return objectManager;
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
  
  public BatchedTransactionProcessor getBatchedTransactionProcessor() {
    return txnProcessor;
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

  public ObjectRequestManager getObjectRequestManager() {
    return this.objectRequestManager;
  }
}