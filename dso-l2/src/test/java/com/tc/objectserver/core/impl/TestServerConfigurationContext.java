/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

import java.util.HashMap;
import java.util.Map;

public class TestServerConfigurationContext implements ServerConfigurationContext {

  public ObjectManager                  objectManager;
  public ObjectRequestManager           objectRequestManager;
  public LockManager                    lockManager;
  public DSOChannelManager              channelManager;
  public ClientStateManager             clientStateManager;
  public ServerTransactionManager       transactionManager;
  public PersistentManagedObjectStore             objectStore;
  public ServerClientHandshakeManager   clientHandshakeManager;
  public Map                            stages = new HashMap();
  public ChannelStats                   channelStats;
  public TransactionalObjectManager     txnObjectManager;
  public L2Coordinator                  l2Coordinator;
  public TransactionBatchManager        transactionBatchManager;
  public ServerGlobalTransactionManager serverGlobalTransactionManager;
  public ServerClusterMetaDataManager   clusterMetaDataManager;
  public GarbageCollectionManager       garbageCollectionManager;

  public void addStage(final String name, final Stage stage) {
    stages.put(name, stage);
  }

  @Override
  public ObjectManager getObjectManager() {
    return this.objectManager;
  }

  @Override
  public ObjectRequestManager getObjectRequestManager() {
    return this.objectRequestManager;
  }

  public void setObjectRequestManager(final ObjectRequestManager objectRequestManager) {
    this.objectRequestManager = objectRequestManager;
  }

    @Override
    public PersistentManagedObjectStore getObjectStore() {
        return this.objectStore;
    }

  @Override
  public LockManager getLockManager() {
    return this.lockManager;
  }

  @Override
  public DSOChannelManager getChannelManager() {
    return this.channelManager;
  }

  @Override
  public ClientStateManager getClientStateManager() {
    return this.clientStateManager;
  }

  @Override
  public ServerTransactionManager getTransactionManager() {
    return this.transactionManager;
  }

  @Override
  public ServerClientHandshakeManager getClientHandshakeManager() {
    return this.clientHandshakeManager;
  }

  @Override
  public Stage getStage(final String name) {
    return (Stage) this.stages.get(name);
  }

  @Override
  public TCLogger getLogger(final Class clazz) {
    return TCLogging.getLogger(getClass());
  }

  @Override
  public ChannelStats getChannelStats() {
    return this.channelStats;
  }

  @Override
  public TransactionBatchReaderFactory getTransactionBatchReaderFactory() {
    throw new ImplementMe();
  }

  @Override
  public TransactionalObjectManager getTransactionalObjectManager() {
    return txnObjectManager;
  }

  @Override
  public L2Coordinator getL2Coordinator() {
    return l2Coordinator;
  }

  @Override
  public TransactionBatchManager getTransactionBatchManager() {
    return transactionBatchManager;
  }

  @Override
  public ServerGlobalTransactionManager getServerGlobalTransactionManager() {
    return serverGlobalTransactionManager;
  }

  @Override
  public ServerClusterMetaDataManager getClusterMetaDataManager() {
    return clusterMetaDataManager;
  }

  @Override
  public ServerMapRequestManager getServerMapRequestManager() {
    throw new ImplementMe();
  }

  @Override
  public IndexManager getIndexManager() {
    throw new ImplementMe();
  }

  @Override
  public MetaDataManager getMetaDataManager() {
    throw new ImplementMe();
  }

  @Override
  public SearchRequestManager getSearchRequestManager() {
    throw new ImplementMe();
  }

  @Override
  public GarbageCollectionManager getGarbageCollectionManager() {
    return garbageCollectionManager;
  }
}
