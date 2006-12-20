/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.async.api.Stage;
import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;
import com.tc.objectserver.tx.TransactionalObjectManager;

import java.util.HashMap;
import java.util.Map;

public class TestServerConfigurationContext implements ServerConfigurationContext {

  public ObjectManager                objectManager;
  public LockManager                  lockManager;
  public DSOChannelManager            channelManager;
  public ClientStateManager           clientStateManager;
  public ServerTransactionManager     transactionManager;
  public ManagedObjectStore           objectStore;
  public ServerClientHandshakeManager clientHandshakeManager;
  public ObjectRequestManager         objectRequestManager;
  public Map                          stages = new HashMap();
  public ChannelStats                 channelStats;
  public TransactionalObjectManager     txnObjectManager;

  public void addStage(String name, Stage stage) {
    stages.put(name, stage);
  }

  public ObjectManager getObjectManager() {
    return this.objectManager;
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

  public Stage getStage(String name) {
    return (Stage) this.stages.get(name);
  }

  public TCLogger getLogger(Class clazz) {
    return TCLogging.getLogger(getClass());
  }

  public ChannelStats getChannelStats() {
    return this.channelStats;
  }

  public TransactionBatchReaderFactory getTransactionBatchReaderFactory() {
    throw new ImplementMe();
  }

  public ObjectRequestManager getObjectRequestManager() {
    return objectRequestManager;
  }

  public TransactionalObjectManager getTransactionalObjectManager() {
    return txnObjectManager;
  }

}
