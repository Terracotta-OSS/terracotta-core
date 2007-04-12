/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.l2.api.L2Coordinator;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;
import com.tc.objectserver.tx.TransactionalObjectManager;

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
  private final TransactionalObjectManager    txnObjectManager;
  private final L2Coordinator                 l2Coordinator;

  public ServerConfigurationContextImpl(StageManager stageManager, ObjectManager objectManager,
                                        ManagedObjectStore objectStore, LockManager lockManager,
                                        DSOChannelManager channelManager, ClientStateManager clientStateManager,
                                        ServerTransactionManager transactionManager,
                                        TransactionalObjectManager txnObjectManager,
                                        ServerClientHandshakeManager clientHandshakeManager, ChannelStats channelStats,
                                        L2Coordinator l2Coordinator,
                                        TransactionBatchReaderFactory transactionBatchReaderFactory) {
    super(stageManager);
    this.objectManager = objectManager;
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
  }

  public L2Coordinator getL2Coordinator() {
    return l2Coordinator;
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

}