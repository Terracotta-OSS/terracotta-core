/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ServerMapEvictionBroadcastContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.persistence.PersistentCollectionsUtil;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TransactionBatchContext;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrinter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
public class ServerMapEvictionEngine {

  private static final TCLogger               logger                          = TCLogging
                                                                                  .getLogger(ServerMapEvictionEngine.class);

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

  private Sink                                evictionBroadcastSink;
  private GroupManager                        groupManager;
  private TransactionBatchManager             transactionBatchManager;
//  private final ServerMapEvictionStatsManager evictionStats;

  public ServerMapEvictionEngine(final ObjectManager objectManager,
                                      final ServerTransactionFactory serverTransactionFactory) {
    this.objectManager = objectManager;
    this.serverTransactionFactory = serverTransactionFactory;
//    this.evictionStats = new ServerMapEvictionStatsManager();
  }

  public void initializeContext(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.evictionBroadcastSink = scc.getStage(ServerConfigurationContext.SERVER_MAP_EVICTION_BROADCAST_STAGE).getSink();
    this.groupManager = scc.getL2Coordinator().getGroupManager();
    this.transactionBatchManager = scc.getTransactionBatchManager();
  }

  public void startEvictor() {
    logger.info(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED + " : " + EVICTOR_LOGGING);
    logger.info(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED + " : "
                + PERIODIC_EVICTOR_ENABLED);

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
  
  private void broadcastEvictedEntries(final ObjectID oid, final Map candidates) {
    // maybe we can batch up the broadcasts
    this.evictionBroadcastSink.add(new ServerMapEvictionBroadcastContext(oid, Collections.unmodifiableSet(candidates
        .keySet())));
  }

  void evictFrom(final ObjectID oid, final Map candidates, final String className, final String cacheName) {
    if ( candidates.isEmpty() ) {
      notifyEvictionCompletedFor(oid);
      return;
    }

    if (EVICTOR_LOGGING) {
      logger.debug("Server Map Eviction  : Evicting " + oid + " [" + cacheName + "] Candidates : " + candidates.size());
    }

    final NodeID localNodeID = this.groupManager.getLocalNodeID();
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ServerTransaction txn = this.serverTransactionFactory.createServerMapEvictionTransactionFor(localNodeID, oid,
                                                                                                      className,
                                                                                                      candidates,
                                                                                                      serializer,
                                                                                                      cacheName);
    final TransactionBatchContext batchContext = new ServerMapEvictionTransactionBatchContext(localNodeID, txn,
                                                                                              serializer);
    this.transactionBatchManager.processTransactions(batchContext);

    if (EVICTOR_LOGGING) {
      logger.debug("Server Map Eviction  : Evicted " + candidates.size() + " from " + oid + " [" + cacheName + "]");
    }
    broadcastEvictedEntries(oid, candidates);
  }
  
  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.indent().print("isStarted:" + this.isStarted).flush();
    out.indent().print("currentlyEvicting:" + this.currentlyEvicting).flush();
    return out;
  }
}
