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
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.context.ServerMapEvictionBroadcastContext;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TransactionBatchContext;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
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
public class ServerMapEvictionManagerImpl implements ServerMapEvictionManager {

  private static final TCLogger               logger                          = TCLogging
                                                                                  .getLogger(ServerMapEvictionManagerImpl.class);

  private static final boolean                EVICTOR_LOGGING                 = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getBoolean(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED);
  private static final boolean                ELEMENT_BASED_TTI_TTL_ENABLED   = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getBoolean(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERELEMENT_TTI_TTL_ENABLED);
  private static final boolean                EVICT_UNEXPIRED_ENTRIES_ENABLED = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getBoolean(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_EVICT_UNEXPIRED_ENTRIES_ENABLED);

  private final static boolean                PERIODIC_EVICTOR_ENABLED        = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getBoolean(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED);

  // 15 Minutes
  public static final long                    DEFAULT_SLEEP_TIME              = 15 * 60000;

  private final ObjectManager                 objectManager;
  private final ManagedObjectStore            objectStore;
  private final ClientStateManager            clientStateManager;
  private final ServerTransactionFactory      serverTransactionFactory;
  private final long                          evictionSleepTime;
  private final Set<ObjectID>                 currentlyEvicting               = Collections
                                                                                  .synchronizedSet(new HashSet());
  private final AtomicBoolean                 isStarted                       = new AtomicBoolean(false);
  private final Timer                         evictor                         = new Timer(
                                                                                          "Server Map Periodic Evictor",
                                                                                          true);

  private Sink                                evictorSink;
  private Sink                                evictionBroadcastSink;
  private GroupManager                        groupManager;
  private TransactionBatchManager             transactionBatchManager;
  private final ServerMapEvictionStatsManager evictionStats;

  public ServerMapEvictionManagerImpl(final ObjectManager objectManager, final ManagedObjectStore objectStore,
                                      final ClientStateManager clientStateManager,
                                      final ServerTransactionFactory serverTransactionFactory,
                                      final long evictionSleepTime) {
    this.objectManager = objectManager;
    this.objectStore = objectStore;
    this.clientStateManager = clientStateManager;
    this.serverTransactionFactory = serverTransactionFactory;
    this.evictionSleepTime = evictionSleepTime;
    this.evictionStats = new ServerMapEvictionStatsManager();
  }

  public void initializeContext(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.evictorSink = scc.getStage(ServerConfigurationContext.SERVER_MAP_EVICTION_PROCESSOR_STAGE).getSink();
    this.evictionBroadcastSink = scc.getStage(ServerConfigurationContext.SERVER_MAP_EVICTION_BROADCAST_STAGE).getSink();
    this.groupManager = scc.getL2Coordinator().getGroupManager();
    this.transactionBatchManager = scc.getTransactionBatchManager();
  }

  public void startEvictor() {
    if (!this.isStarted.getAndSet(true) && PERIODIC_EVICTOR_ENABLED) {
      logger.info("Server Map Eviction : Evictor will run every " + this.evictionSleepTime + " ms");
      this.evictor.schedule(new EvictorTask(this), this.evictionSleepTime, this.evictionSleepTime);
    }
    logger.info(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED + " : " + EVICTOR_LOGGING);
    logger.info(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED + " : "
                + PERIODIC_EVICTOR_ENABLED);
    logger.info(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERELEMENT_TTI_TTL_ENABLED + " : "
                + ELEMENT_BASED_TTI_TTL_ENABLED);

  }

  public void runEvictor() {
    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Periodic Evictor Started ");
    }
    evictionStats.periodicEvictionStarted();

    final ObjectIDSet evictableObjects = this.objectStore.getAllEvictableObjectIDs();
    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Number of Evictable : " + evictableObjects.size());
    }

    final ObjectIDSet faultedInClients = new ObjectIDSet();
    this.clientStateManager.addAllReferencedIdsTo(faultedInClients);
    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Number of Objects faulted in L1 : " + faultedInClients.size());
    }

    for (final ObjectID mapID : evictableObjects) {
      doEvictionOn(mapID, faultedInClients, true);
    }

    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Ended ");
    }
    evictionStats.periodicEvictionFinished();
  }

  public void doEvictionOn(final ObjectID oid, final SortedSet<ObjectID> faultedInClients, final boolean periodicEvictor) {
    if (!this.isStarted.get()) { throw new AssertionError("Evictor is not started yet"); }
    if (!markEvictionInProgress(oid)) {
      logger.info("Ignoring eviction request as its already in progress : " + oid);
      return;
    }

    if (EVICTOR_LOGGING && !periodicEvictor) {
      logger.info("Server Map Eviction  : Capacity Evictor Started for " + oid);
    }

    try {
      basicDoEviction(oid, faultedInClients, periodicEvictor);
    } finally {
      markEvictionDone(oid);
    }

    if (EVICTOR_LOGGING && !periodicEvictor) {
      logger.info("Server Map Eviction  : Capacity Evictor Ended for " + oid);
    }

  }

  private void basicDoEviction(final ObjectID oid, final SortedSet<ObjectID> faultedInClients,
                               final boolean periodicEvictor) {
    final ManagedObject mo = this.objectManager.getObjectByIDOrNull(oid);
    ServerMapEvictionContext context = null;
    if (mo == null) { return; }
    final ManagedObjectState state = mo.getManagedObjectState();
    final String className = state.getClassName();
    final String loaderDesc = state.getLoaderDescription();

    EvictableMap ev = null;
    try {
      ev = getEvictableMapFrom(mo.getID(), state);
      context = doEviction(oid, ev, faultedInClients, className, loaderDesc, periodicEvictor, ev.getCacheName());
    } finally {
      if (context == null) {
        ev.evictionCompleted();
        this.objectManager.releaseReadOnly(mo);
      } else {
        // Reason for releasing the checked-out object before adding the context to the sink is that we can block on add
        // to the sink because the sink reached max capacity and blocking
        // with a checked-out object will result in a deadlock. @see DEV-5207
        this.objectManager.releaseReadOnly(mo);
        this.evictorSink.add(context);
      }
    }
  }

  private boolean markEvictionInProgress(final ObjectID oid) {
    return this.currentlyEvicting.add(oid);
  }

  private void markEvictionDone(final ObjectID oid) {
    this.currentlyEvicting.remove(oid);
  }

  private EvictableMap getEvictableMapFrom(final ObjectID id, final ManagedObjectState state) {
    if (!PersistentCollectionsUtil.isEvictableMapType(state.getType())) { throw new AssertionError(
                                                                                                   "Received wrong object thats not evictable : "
                                                                                                       + id + " : "
                                                                                                       + state); }
    return (EvictableMap) state;
  }

  /**
   * Collects random samples and initiates eviction
   * 
   * @return true, if eviction is initiated, false otherwise
   */
  private ServerMapEvictionContext doEviction(final ObjectID oid, final EvictableMap ev,
                                              final SortedSet<ObjectID> faultedInClients, final String className,
                                              final String loaderDesc, final boolean periodicEvictor,
                                              final String cacheName) {
    final int targetMaxTotalCount = ev.getMaxTotalCount();
    final int currentSize = ev.getSize();
    if (targetMaxTotalCount <= 0 || currentSize <= targetMaxTotalCount) {
      if (EVICTOR_LOGGING) {
        logger.info("Server Map Eviction  : Eviction not required for " + oid + " [" + cacheName + "]; currentSize: "
                    + currentSize + " Vs targetMaxTotalCount: " + targetMaxTotalCount);
      }
      if (periodicEvictor) {
        evictionStats.evictionNotRequired(oid, ev, targetMaxTotalCount, currentSize);
      }
      return null;
    }
    final int overshoot = currentSize - targetMaxTotalCount;
    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Trying to evict : " + oid + " [" + cacheName + "] overshoot : " + overshoot
                  + " : current Size : " + currentSize + " : target max : " + targetMaxTotalCount);
    }

    final int ttl = ev.getTTLSeconds();
    final int tti = ev.getTTISeconds();
    final int requested = isInterestedInTTIOrTTL(tti, ttl) ? (int) (overshoot * 1.5) : overshoot;
    final Map samples = ev.getRandomSamples(requested, faultedInClients);

    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Got Random samples to evict : " + oid + " [" + cacheName
                  + "] : Random Samples : " + samples.size() + " overshoot : " + overshoot);
    }
    if (periodicEvictor) {
      evictionStats.evictionRequested(oid, ev, targetMaxTotalCount, overshoot, samples.size());
    }

    if (samples.isEmpty()) {
      return null;
    } else {
      return new ServerMapEvictionContext(oid, targetMaxTotalCount, tti, ttl, samples, overshoot, className,
                                          loaderDesc, cacheName);
    }
  }

  private boolean isInterestedInTTIOrTTL(final int tti, final int ttl) {
    return (tti > 0 || ttl > 0 || ELEMENT_BASED_TTI_TTL_ENABLED);
  }

  public void evict(final ObjectID oid, final Map samples, final int targetMaxTotalCount, final int ttiSeconds,
                    final int ttlSeconds, final int overshoot, final String className, final String loaderDesc,
                    final String cacheName) {
    final HashMap candidates = new HashMap(samples.size());
    final ArrayList likelyCandidates = new ArrayList(samples.size());
    int expired = 0;
    final int now = (int) (System.currentTimeMillis() / 1000);
    for (final Iterator iterator = samples.entrySet().iterator(); candidates.size() < overshoot && iterator.hasNext();) {
      final Entry e = (Entry) iterator.next();
      int expiresIn = expiresIn(now, e.getValue(), ttiSeconds, ttlSeconds);
      if (expiresIn <= 0) {
        // Element already expired
        candidates.put(e.getKey(), e.getValue());
        expired++;
      } else if (EVICT_UNEXPIRED_ENTRIES_ENABLED) {
        likelyCandidates.add(new ExpiryKey(expiresIn, e));
      }
    }
    if (candidates.size() < overshoot) {
      Collections.sort(likelyCandidates); // Is this costly ?
      int lastExpiresIn = Integer.MIN_VALUE;
      for (final Iterator i = likelyCandidates.iterator(); candidates.size() < overshoot && i.hasNext();) {
        ExpiryKey ek = (ExpiryKey) i.next();
        if (lastExpiresIn > ek.expiresIn()) { throw new AssertionError("Likely candidates is not sorted correctly : "
                                                                       + likelyCandidates); }
        lastExpiresIn = ek.expiresIn();
        Entry e = ek.getEntry();
        candidates.put(e.getKey(), e.getValue());
      }
    }
    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction : " + oid + " [" + cacheName + "] : Evicting " + candidates.size()
                  + " of which expired or eternal : " + expired + " alive : " + (candidates.size() - expired)
                  + " Samples : " + samples.size());
    }
    if (candidates.size() > 0) {
      evictFrom(oid, Collections.unmodifiableMap(candidates), className, loaderDesc, cacheName);
      broadcastEvictedEntries(oid, candidates);
    } else {
      notifyEvictionCompletedFor(oid);
    }
    evictionStats.entriesEvicted(oid, overshoot, samples.size(), candidates.size());
  }

  private void notifyEvictionCompletedFor(ObjectID oid) {
    final ManagedObject mo = this.objectManager.getObjectByIDOrNull(oid);
    if (mo == null) { return; }
    final ManagedObjectState state = mo.getManagedObjectState();
    try {
      final EvictableMap ev = getEvictableMapFrom(mo.getID(), state);
      ev.evictionCompleted();
    } finally {
      this.objectManager.releaseReadOnly(mo);
    }
  }

  private void broadcastEvictedEntries(final ObjectID oid, final HashMap candidates) {
    // maybe we can batch up the broadcasts
    this.evictionBroadcastSink.add(new ServerMapEvictionBroadcastContext(oid, Collections.unmodifiableSet(candidates
        .keySet())));
  }

  private void evictFrom(final ObjectID oid, final Map candidates, final String className, final String loaderDesc,
                         final String cacheName) {
    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Evicting " + oid + " [" + cacheName + "] Candidates : " + candidates.size());
    }
    final NodeID localNodeID = this.groupManager.getLocalNodeID();
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ServerTransaction txn = this.serverTransactionFactory.createServerMapEvictionTransactionFor(localNodeID, oid,
                                                                                                      className,
                                                                                                      loaderDesc,
                                                                                                      candidates,
                                                                                                      serializer,
                                                                                                      cacheName);
    final TransactionBatchContext batchContext = new ServerMapEvictionTransactionBatchContext(localNodeID, txn,
                                                                                              serializer);
    this.transactionBatchManager.processTransactions(batchContext);

    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Evicted " + candidates.size() + " from " + oid + " [" + cacheName + "]");
    }
  }

  /**
   * This method tries to compute when an entry will expire relative to "now". If the value is not an EvictableEntry or
   * if tti/ttl is 0 and the property ehcache.storageStrategy.dcv2.perElementTTITTL.enable is not set to true, then it
   * always returns 0, ie. Expire Now.
   * 
   * @return when values is going to expire relative to "now", a negative number or zero indicates the value is expired.
   */
  private int expiresIn(final int now, final Object value, final int ttiSeconds, final int ttlSeconds) {
    if ((!(value instanceof ObjectID)) || !isInterestedInTTIOrTTL(ttiSeconds, ttlSeconds)) {
      // When tti/ttl == 0 here, then cache is set to eternal (by default or in config explicitly), so all entries can
      // be evicted if maxInDisk size is reached.
      return 0;
    }
    final ObjectID oid = (ObjectID) value;
    final ManagedObject mo = this.objectManager.getObjectByIDOrNull(oid);
    if (mo == null) { return 0; }
    try {
      final EvictableEntry ev = getEvictableEntryFrom(mo);
      if (ev != null) {
        return ev.expiresIn(now, ttiSeconds, ttlSeconds);
      } else {
        return 0;
      }
    } finally {
      this.objectManager.releaseReadOnly(mo);
    }
  }

  private EvictableEntry getEvictableEntryFrom(final ManagedObject mo) {
    final ManagedObjectState state = mo.getManagedObjectState();
    if (state instanceof EvictableEntry) { return (EvictableEntry) state; }
    // TODO:: Custom mode support
    return null;
  }

  private static class EvictorTask extends TimerTask {
    private final ServerMapEvictionManager serverMapEvictionMgr;

    public EvictorTask(final ServerMapEvictionManager serverMapEvictionMgr) {
      this.serverMapEvictionMgr = serverMapEvictionMgr;
    }

    @Override
    public void run() {
      this.serverMapEvictionMgr.runEvictor();
    }
  }

  private static final class ExpiryKey implements Comparable {

    private final int   expiresIn;
    private final Entry e;

    public ExpiryKey(int expiresIn, Entry e) {
      this.expiresIn = expiresIn;
      this.e = e;
    }

    public int expiresIn() {
      return expiresIn;
    }

    public Entry getEntry() {
      return e;
    }

    public int compareTo(Object o) {
      return expiresIn - ((ExpiryKey) o).expiresIn;
    }

  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.indent().print("isStarted:" + this.isStarted).flush();
    out.indent().print("currentlyEvicting:" + this.currentlyEvicting).flush();
    return out;
  }
}
