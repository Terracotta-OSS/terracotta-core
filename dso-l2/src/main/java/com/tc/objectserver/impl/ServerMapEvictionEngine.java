/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.persistence.EvictionTransactionPersistor;
import com.tc.objectserver.persistence.EvictionTransactionPersistorImpl;
import com.tc.objectserver.persistence.PersistentCollectionsUtil;
import com.tc.objectserver.tx.AbstractServerTransactionListener;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManagerImpl;
import com.tc.objectserver.tx.TransactionBatchContext;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrinter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
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

  private final ObjectManager                 objectManager;
  private final ServerTransactionFactory      serverTransactionFactory;
  private final Set<ObjectID>                 currentlyEvicting               = Collections
                                                                                  .synchronizedSet(new HashSet());
  private final AtomicBoolean                 isStarted                       = new AtomicBoolean(false);

  private GroupManager                        groupManager;
  private TransactionBatchManager             transactionBatchManager;
  private EvictionTransactionPersistor        evictionTransactionPersistor;
  private ServerTransactionManagerImpl        serverTransactionManager;

  public ServerMapEvictionEngine(final ObjectManager objectManager,
                                 final ServerTransactionFactory serverTransactionFactory,
                                 final EvictionTransactionPersistor evictionTransactionPersistor,
                                 final ServerTransactionManagerImpl serverTransactionManager) {
    this.objectManager = objectManager;
    this.serverTransactionFactory = serverTransactionFactory;
    this.evictionTransactionPersistor = evictionTransactionPersistor;
    this.serverTransactionManager = serverTransactionManager;
  }

  public void initializeContext(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.groupManager = scc.getL2Coordinator().getGroupManager();
    this.transactionBatchManager = scc.getTransactionBatchManager();

    // if running in persistence mode, we need to save each in-flight transaction to FRS and delete it when complete
    if(evictionTransactionPersistor.getClass() == EvictionTransactionPersistorImpl.class) {
      scc.getTransactionManager().addTransactionListener(this);
    }
  }

  public void startEvictor() {
    logger.info(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED + " : " + EVICTOR_LOGGING);
    logger.info(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED + " : "
                + PERIODIC_EVICTOR_ENABLED);

    logger.info("Recovering any in-flight eviction transactions from the previous session.");
    Collection<TransactionBatchContext> transactionBatchContexts = evictionTransactionPersistor.getAllTransactionBatches();
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    if (transactionBatchContexts.size() > 0) {
      serverTransactionManager.addTransactionListener(this);
      logger.info("Found " + evictionTransactionPersistor.getAllTransactionBatches().size() + " incomplete transactions from previous session, recovering them.");
      for(TransactionBatchContext transactionBatchContext : transactionBatchContexts) {
        transactionBatchManager.processTransactions(transactionBatchContext);
      }
      serverTransactionManager.callBackOnTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
        @Override
        public void onCompletion() {
          countDownLatch.countDown();
        }
      });
      try {
        countDownLatch.await();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      evictionTransactionPersistor.removeAllTransactions();
      logger.info("Recovered all former in-flight eviction transactions. Starting normal evictor operation.");
    } else {
      logger.info("No in-flight eviction transactions found from previous session. Starting normal evictor operation.");
    }
    if (evictionTransactionPersistor.getAllTransactionBatches().size() > 0) {
      logger.warn("Not all recovered in-flight transactions were deleted from persistent storage. This could lead to data corruption in future restarts. Number of txns remaining = " + evictionTransactionPersistor.getAllTransactionBatches().size() );
    }
  }

  boolean isLogging() {
      return EVICTOR_LOGGING;
  }

  boolean markEvictionInProgress(final ObjectID oid) {
    return this.currentlyEvicting.add(oid);
  }

  void markEvictionDone(final ObjectID oid) {
    this.currentlyEvicting.remove(oid);
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
    } finally {
      this.objectManager.releaseReadOnly(mo);
    }
  }

  void evictFrom(final ObjectID oid, final Map<Object, EvictableEntry> candidates, final String className, final String cacheName) {
    if ( candidates.isEmpty() ) {
      notifyEvictionCompletedFor(oid);
      return;
    }

    if (EVICTOR_LOGGING) {
      logger.debug("Server Map Eviction  : Evicting " + oid + " [" + cacheName + "] Candidates : " + candidates.size());
    }

    NodeID localNodeID = groupManager.getLocalNodeID();
    ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    ServerTransaction serverTransaction = serverTransactionFactory.createServerMapEvictionTransactionFor(localNodeID, oid,
        className, candidates, serializer, cacheName);

    TransactionBatchContext batchContext = new ServerMapEvictionTransactionBatchContext(localNodeID, serverTransaction,
        serializer);

    evictionTransactionPersistor.saveTransactionBatch(serverTransaction.getServerTransactionID(), batchContext);
    transactionBatchManager.processTransactions(batchContext);
    if (EVICTOR_LOGGING) {
      logger.debug("Server Map Eviction  : Evicted " + candidates.size() + " from " + oid + " [" + cacheName + "]");
    }
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.indent().print("isStarted:" + this.isStarted).flush();
    out.indent().print("currentlyEvicting:" + this.currentlyEvicting).flush();
    return out;
  }

  @Override
  public void transactionCompleted(ServerTransactionID stxID) {
    evictionTransactionPersistor.removeTransaction(stxID);
  }

}
