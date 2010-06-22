/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The main class that performs server side eviction for ConcurrentDistributedServerMap and other similar
 * data-structures in future.
 * 
 * @author Saravanan Subbiah
 */
public class ServerMapEvictionManagerImpl implements ServerMapEvictionManager {

  private static final boolean                 EVICTOR_LOGGING               = TCPropertiesImpl
                                                                                 .getProperties()
                                                                                 .getBoolean(
                                                                                             TCPropertiesConsts.EHCAHCE_EVICTOR_LOGGING_ENABLED);
  private static final boolean                 ELEMENT_BASED_TTI_TTL_ENABLED = TCPropertiesImpl
                                                                                 .getProperties()
                                                                                 .getBoolean(
                                                                                             TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERELEMENT_TTI_TTL_ENABLED);

  private static final TCLogger                logger                        = TCLogging
                                                                                 .getLogger(ServerMapEvictionManagerImpl.class);

  // 15 Minutes
  public static final long                     DEFAULT_SLEEP_TIME            = 15 * 60000;

  private final ObjectManager                  objectManager;
  private final ManagedObjectStore             objectStore;
  private final ClientStateManager             clientStateManager;
  private final PersistenceTransactionProvider transactionStorePTP;
  private final long                           evictionSleepTime;
  private final AtomicBoolean                  isStarted                     = new AtomicBoolean(false);
  private final Timer                          evictor                       = new Timer("Server Map Evictor", true);

  private Sink                                 evictorSink;

  public ServerMapEvictionManagerImpl(final ObjectManager objectManager, final ManagedObjectStore objectStore,
                                      final ClientStateManager clientStateManager, final long evictionSleepTime,
                                      final PersistenceTransactionProvider transactionStorePTP) {
    this.objectManager = objectManager;
    this.objectStore = objectStore;
    this.clientStateManager = clientStateManager;
    this.evictionSleepTime = evictionSleepTime;
    this.transactionStorePTP = transactionStorePTP;
  }

  public void initializeContext(final ConfigurationContext context) {
    this.evictorSink = context.getStage(ServerConfigurationContext.SERVER_MAP_EVICTION_PROCESSOR_STAGE).getSink();
  }

  public void startEvictor() {
    if (!this.isStarted.getAndSet(true)) {
      logger.info("Server Map Eviction : Evictor will run every " + this.evictionSleepTime + " ms");
      this.evictor.schedule(new EvictorTask(this), this.evictionSleepTime, this.evictionSleepTime);
    }
  }

  public void runEvictor() {
    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Started ");
    }

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
      doEvictionOn(mapID, faultedInClients);
    }

    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Ended ");
    }
  }

  public void doEvictionOn(final ObjectID oid, final SortedSet<ObjectID> faultedInClients) {
    final ManagedObject mo = this.objectManager.getObjectByID(oid);
    try {
      final EvictableMap ev = getEvictableMapFrom(mo);
      doEviction(oid, ev, faultedInClients);
    } finally {
      this.objectManager.releaseReadOnly(mo);
    }
  }

  private EvictableMap getEvictableMapFrom(final ManagedObject mo) {
    final ManagedObjectState state = mo.getManagedObjectState();
    if (!PersistentCollectionsUtil.isEvictableMapType(state.getType())) { throw new AssertionError(
                                                                                                   "Received wrong object thats not evictable : "
                                                                                                       + mo.getID()
                                                                                                       + " : " + mo); }
    return (EvictableMap) state;
  }

  private void doEviction(final ObjectID oid, final EvictableMap ev, final SortedSet<ObjectID> faultedInClients) {
    final int targetMaxTotalCount = ev.getMaxTotalCount();
    final int currentSize = ev.getSize();
    if (targetMaxTotalCount <= 0 || currentSize <= targetMaxTotalCount) { return; }
    final int overshoot = currentSize - targetMaxTotalCount;
    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Trying to evict : " + oid + " overshoot : " + overshoot
                  + " : current Size : " + currentSize + " : target max : " + targetMaxTotalCount);
    }

    final int ttl = ev.getTTLSeconds();
    final int tti = ev.getTTISeconds();
    final int requested = isInterestedInTTIOrTTL(tti, ttl) ? (int) (overshoot * 1.5) : overshoot;
    final Map samples = ev.getRandomSamples(requested, faultedInClients);

    if ((samples.size() < overshoot * 0.5) || EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Got Random samples to evict : " + oid + " : Random Samples : "
                  + samples.size() + " overshoot : " + overshoot);
    }

    if (!samples.isEmpty()) {
      final ServerMapEvictionContext context = new ServerMapEvictionContext(oid, targetMaxTotalCount, tti, ttl,
                                                                            samples, overshoot);
      this.evictorSink.add(context);
    }
  }

  private boolean isInterestedInTTIOrTTL(final int tti, final int ttl) {
    return (tti > 0 || ttl > 0 || ELEMENT_BASED_TTI_TTL_ENABLED);
  }

  public void evict(final ObjectID oid, final Map samples, final int targetMaxTotalCount, final int ttiSeconds,
                    final int ttlSeconds, final int overshoot) {
    final HashMap candidates = new HashMap();
    int cantEvict = 0;
    for (final Iterator iterator = samples.entrySet().iterator(); candidates.size() < overshoot && iterator.hasNext();) {
      final Entry e = (Entry) iterator.next();
      if (canEvict(e.getValue(), ttiSeconds, ttlSeconds)) {
        candidates.put(e.getKey(), e.getValue());
      } else {
        if (++cantEvict % 1000 == 0) {
          if (EVICTOR_LOGGING) {
            logger.info("Server Map Eviction  : Can't Evict " + cantEvict + " Candidates so far : " + candidates.size()
                        + " Samples : " + samples.size());
          }
        }
      }
    }
    evictFrom(oid, candidates);
  }

  private void evictFrom(final ObjectID oid, final HashMap candidates) {
    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Evicting " + oid + " Candidates : " + candidates.size());
    }
    final ManagedObject mo = this.objectManager.getObjectByID(oid);
    try {
      final EvictableMap ev = getEvictableMapFrom(mo);
      ev.evict(candidates);
    } finally {
      releaseAndCommit(mo);
    }
    if (EVICTOR_LOGGING) {
      logger.info("Server Map Eviction  : Evicted " + candidates.size() + " from " + oid);
    }

  }

  private void releaseAndCommit(final ManagedObject mo) {
    final PersistenceTransaction txn = this.transactionStorePTP.newTransaction();
    // This call commits the transaction too.
    this.objectManager.releaseAndCommit(txn, mo);
  }

  private boolean canEvict(final Object value, final int ttiSeconds, final int ttlSeconds) {
    if ((!(value instanceof ObjectID)) || !isInterestedInTTIOrTTL(ttiSeconds, ttlSeconds)) { return true; }
    final ObjectID oid = (ObjectID) value;
    final ManagedObject mo = this.objectManager.getObjectByID(oid);
    try {
      final EvictableEntry ev = getEvictableEntryFrom(mo);
      if (ev != null) {
        return ev.canEvict(ttiSeconds, ttlSeconds);
      } else {
        return true;
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
}
