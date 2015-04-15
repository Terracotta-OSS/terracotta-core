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
package com.tc.objectserver.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.EvictionListener;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.persistence.EvictionRemoveContext;
import com.tc.objectserver.persistence.EvictionTransactionPersistor;
import com.tc.objectserver.persistence.PersistentCollectionsUtil;
import com.tc.objectserver.tx.AbstractServerTransactionListener;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TransactionBatchContext;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrinter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The main class that performs server side eviction for ConcurrentDistributedServerMap and other similar
 * data-structures in future.
 * <p>
 * The Algorithm for server side eviction could be described as follows : <br>
 * 1. If tti/ttl are not set (ie == 0), or the cache is set to eternal (which means tti/ttl = 0), then any entry that is
 * not present in the L1's heap becomes a candidate for eviction.<br>
 * 2. If the property ehcache.storageStrategy.dcv2.perElementTTITTL.enable is set to true, then even if tti/ttl are not
 * set at the cache level, each element from a sampled set of overshoot entries are faulted to check element level
 * tti/ttl and access-time/creation-time to make sure that either expired elements are evicted or sooner rather than
 * later to be expired elements are evicted. Here tti/ttl of 0 is eternal so they take least precedence.
 *
 * @author Saravanan Subbiah
 */
public class ServerMapEvictionEngine extends AbstractServerTransactionListener {

  private static final TCLogger               logger                          = TCLogging.getLogger(ServerMapEvictionEngine.class);

  private static final boolean                EVICTOR_LOGGING                 = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getBoolean(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED);

  private final static boolean                PERIODIC_EVICTOR_ENABLED        = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getBoolean(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED);

  // 15 Minutes
  public static final long                    DEFAULT_SLEEP_TIME              = 15 * 60000;

  private final boolean                       persistent;
  private final ObjectManager                 objectManager;
  private final ServerTransactionFactory      serverTransactionFactory;
  private final Set<ObjectID>                 currentlyEvicting               = new HashSet<ObjectID>();
  private final Set<EvictionListener>         listeners                       = new HashSet<EvictionListener>();
  private final AtomicBoolean                 isStarted                       = new AtomicBoolean(false);
  private final Map<ServerTransactionID, ObjectID> inflightEvictions          = new ConcurrentHashMap<ServerTransactionID, ObjectID>();

  private TransactionBatchManager             transactionBatchManager;
  private EvictionTransactionPersistor        evictionTransactionPersistor;

  public ServerMapEvictionEngine(final ObjectManager objectManager,
                                 final ServerTransactionFactory serverTransactionFactory,
                                 final EvictionTransactionPersistor evictionTransactionPersistor, final boolean persistent) {
    this.objectManager = objectManager;
    this.serverTransactionFactory = serverTransactionFactory;
    this.evictionTransactionPersistor = evictionTransactionPersistor;
    this.persistent = persistent;
  }

  public void initializeContext(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionBatchManager = scc.getTransactionBatchManager();

    // if running in persistence mode, we need to save each in-flight transaction to FRS and delete it when complete
    scc.getTransactionManager().addTransactionListener(this);
  }

  public void startEvictor() {
    logger.info(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED + " : " + EVICTOR_LOGGING);
    logger.info(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED + " : "
                + PERIODIC_EVICTOR_ENABLED);

    logger.info("Recovering any in-flight eviction transactions from the previous session.");
    for (ServerTransactionID recoveredTxnID : evictionTransactionPersistor.getPersistedTransactions()) {
      EvictionRemoveContext eviction = evictionTransactionPersistor.getEviction(recoveredTxnID);
      ObjectStringSerializer objectStringSerializer = new ObjectStringSerializerImpl();
      ServerTransaction removeAllTransaction = serverTransactionFactory.createRemoveAllTransaction(recoveredTxnID, eviction
          .getObjectID(), eviction.getCacheName(), eviction.getSamples(), objectStringSerializer);
      TransactionBatchContext context = new ServerTransactionBatchContext(recoveredTxnID.getSourceID(), removeAllTransaction, objectStringSerializer);
      transactionBatchManager.processTransactions(context);
    }
  }

  boolean isLogging() {
    return EVICTOR_LOGGING;
  }

  synchronized boolean markEvictionInProgress(final ObjectID oid) {
    boolean starting = this.currentlyEvicting.add(oid);
    if ( starting ) {
      if ( EVICTOR_LOGGING ) {
        logger.debug("starting eviction " + oid);
      }
      Iterator<EvictionListener> list = listeners.iterator();
      while ( list.hasNext() ) {
        if ( list.next().evictionStarted(oid) ) {
          list.remove();
        }
      }
    }
    return starting;
  }

  synchronized void markEvictionDone(final ObjectID oid) {
    if ( !this.currentlyEvicting.remove(oid) ) {
      throw new AssertionError("not evicting");
    } else {
      if ( EVICTOR_LOGGING ) {
        logger.debug("ending eviction " + oid);
      }
      
      Iterator<EvictionListener> list = listeners.iterator();
      while ( list.hasNext() ) {
        EvictionListener evl = list.next();
        if ( evl.evictionCompleted(oid) ) {
          list.remove();
        }
      }
    }
  }

  private EvictableMap getEvictableMapFrom(final ObjectID id, final ManagedObjectState state) {
    if (!PersistentCollectionsUtil.isEvictableMapType(state.getType())) { throw new AssertionError(
                                                                                                   "Received wrong object thats not evictable : "
                                                                                                       + id + " : "
                                                                                                       + state); }
    return (EvictableMap) state;
  }

  private void notifyEvictionCompletedFor(ObjectID oid) {
    final ManagedObject mo = this.objectManager.getObjectByIDReadOnly(oid);
    if (mo == null) { return; }
    final ManagedObjectState state = mo.getManagedObjectState();
    try {
      final EvictableMap ev = getEvictableMapFrom(mo.getID(), state);
      ev.evictionCompleted();
      markEvictionDone(oid);
    } finally {
      this.objectManager.releaseReadOnly(mo);
    }
  }

  void evictFrom(final ObjectID oid, final Map<Object, EvictableEntry> candidates, final String cacheName) {
    if ( candidates.isEmpty() ) {
      notifyEvictionCompletedFor(oid);
      return;
    }

    if (EVICTOR_LOGGING) {
      logger.debug("Server Map Eviction  : Evicting " + oid + " [" + cacheName + "] Candidates : " + candidates.size());
    }

    ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    ServerTransaction serverTransaction = serverTransactionFactory.createServerMapEvictionTransactionFor(oid,
        candidates, serializer, cacheName);

    TransactionBatchContext batchContext = new ServerTransactionBatchContext(serverTransaction.getSourceID(),
        serverTransaction, serializer);
    
    inflightEvictions.put(serverTransaction.getServerTransactionID(), oid);

    if ( persistent ) {
      evictionTransactionPersistor.saveEviction(serverTransaction.getServerTransactionID(), oid, cacheName, candidates);
    }
    transactionBatchManager.processTransactions(batchContext);
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.indent().print("isStarted:" + this.isStarted).flush();
    out.indent().print("currentlyEvicting:" + currentlyEvicting()).flush();
    return out;
  }

  @Override
  public void transactionCompleted(ServerTransactionID stxID) {
    ObjectID targetID = inflightEvictions.remove(stxID);
    
    if ( targetID == null ) {
      return;
    }
    
    markEvictionDone(targetID);

    if (EVICTOR_LOGGING) {
      logger.debug("Server Map Eviction  : Evicted  from " + targetID);
    }
    
    if ( persistent ) {
      evictionTransactionPersistor.removeEviction(stxID);
    }
  }
  
  public synchronized void addEvictionListener(EvictionListener listener) {
      listeners.add(listener);
  }
  
  public synchronized void removeEvictionListener(EvictionListener listener) {
      listeners.add(listener);
  }
  
  public synchronized Set<ObjectID> currentlyEvicting() {
    return new HashSet(currentlyEvicting);
  }

}
