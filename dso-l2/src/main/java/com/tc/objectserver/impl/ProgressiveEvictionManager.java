/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.EvictionTrigger;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ResourceManager;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.api.ShutdownError;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.persistence.PersistentCollectionsUtil;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.MemoryUsage;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounterImpl;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author mscott
 */
public class ProgressiveEvictionManager implements ServerMapEvictionManager {

  private static final TCLogger                   logger                              = TCLogging
                                                                                          .getLogger(ProgressiveEvictionManager.class);
  private static final long                       L2_EVICTION_RESOURCEPOLLINGINTERVAL = TCPropertiesImpl
                                                                                          .getProperties()
                                                                                          .getLong(TCPropertiesConsts.L2_EVICTION_RESOURCEPOLLINGINTERVAL,
                                                                                                   -1);
  private static final int                        L2_EVICTION_CRITICALTHRESHOLD       = TCPropertiesImpl
                                                                                          .getProperties()
                                                                                          .getInt(TCPropertiesConsts.L2_EVICTION_CRITICALTHRESHOLD,
                                                                                                  -1);
  private static final int                        L2_EVICTION_HALTTHRESHOLD           = TCPropertiesImpl
                                                                                          .getProperties()
                                                                                          .getInt(TCPropertiesConsts.L2_EVICTION_HALTTHRESHOLD,
                                                                                                  -1);
  private final static boolean                    PERIODIC_EVICTOR_ENABLED            = TCPropertiesImpl
                                                                                          .getProperties()
                                                                                          .getBoolean(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED,
                                                                                                      true);
  private final ServerMapEvictionEngine           evictor;
  private final ResourceMonitor                   trigger;
  private final PersistentManagedObjectStore      store;
  private final ObjectManager                     objectManager;
  private final ClientObjectReferenceSet          clientObjectReferenceSet;
  private Sink                                    evictorSink;
  private final ExecutorService                   agent;
  private ThreadGroup                             evictionGrp;
  private final Responder                         responder                           = new Responder();
  private final SampledCounter                    expirationStats;
  private final SampledCounter                    evictionStats;
  private final ResourceManager                   resourceManager;
  private final EvictionThreshold                 threshold;

  private final static Future<SampledRateCounter> completedFuture                     = new Future<SampledRateCounter>() {

                                                                                        private final SampledRateCounter zeroStats = new SampledRateCounterImpl(
                                                                                                                                                                new SampledRateCounterConfig(
                                                                                                                                                                                             5,
                                                                                                                                                                                             100,
                                                                                                                                                                                             false));

                                                                                        @Override
                                                                                        public boolean cancel(boolean bln) {
                                                                                          return true;
                                                                                        }

                                                                                        @Override
                                                                                        public boolean isCancelled() {
                                                                                          return true;
                                                                                        }

                                                                                        @Override
                                                                                        public boolean isDone() {
                                                                                          return true;
                                                                                        }

                                                                                        @Override
                                                                                        public SampledRateCounter get() {
                                                                                          return zeroStats;
                                                                                        }

                                                                                        @Override
                                                                                        public SampledRateCounter get(long l,
                                                                                                                      TimeUnit tu) {
                                                                                          return zeroStats;
                                                                                        }

                                                                                      };

  @Override
  public SampledCounter getExpirationStatistics() {
    return expirationStats;
  }

  @Override
  public SampledCounter getEvictionStatistics() {
    return evictionStats;
  }

  public ProgressiveEvictionManager(ObjectManager mgr, MonitoredResource monitored, PersistentManagedObjectStore store,
                                    ClientObjectReferenceSet clients, ServerTransactionFactory trans,
                                    final TCThreadGroup grp, final ResourceManager resourceManager,
                                    final CounterManager counterManager) {
    this.objectManager = mgr;
    this.store = store;
    this.clientObjectReferenceSet = clients;
    this.resourceManager = resourceManager;
    this.evictor = new ServerMapEvictionEngine(mgr, trans);
    // assume 100 MB/sec fill rate and set 0% usage poll rate to the time it would take to fill up.
    this.evictionGrp = new ThreadGroup(grp, "Eviction Group") {

      @Override
      public void uncaughtException(Thread thread, Throwable thrwbl) {
        getParent().uncaughtException(thread, thrwbl);
      }

    };
    long sleeptime = L2_EVICTION_RESOURCEPOLLINGINTERVAL;
    if (sleeptime < 0) {
      // 1GB a second
      sleeptime = (monitored.getTotal() * 1000) / (1024 * 1024 * 1024);
      if (sleeptime > 120 * 1000) {
        // max out at 2 min.
        sleeptime = 120 * 1000;
      }
    }
    this.threshold = EvictionThreshold.configure(monitored);
    log("Using threshold " + this.threshold + " for total size " + monitored.getTotal());
    log(this.threshold.log(L2_EVICTION_CRITICALTHRESHOLD, L2_EVICTION_HALTTHRESHOLD));

    this.trigger = new ResourceMonitor(monitored, sleeptime, evictionGrp);
    this.agent = new ThreadPoolExecutor(4, 64, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                                        new ThreadFactory() {
                                          private int count = 1;

                                          @Override
                                          public Thread newThread(Runnable r) {
                                            Thread t = new Thread(evictionGrp, r, "Expiration Thread - " + count++);
                                            return t;
                                          }
                                        }, new ThreadPoolExecutor.DiscardPolicy());
    try {
      Runnable rb = new Runnable() {

        @Override
        public void run() {
          log("Threshold crossed");
          resourceManager.setThrowException();
        }

      };
      if (monitored.getType() == MonitoredResource.Type.OFFHEAP) {
        monitored
            .addReservedThreshold(MonitoredResource.Direction.RISING, monitored.getTotal() - 32l * 1024 * 1024, rb);
      }
    } catch (UnsupportedOperationException uns) {
      logger.info("threshold monitor not registered", uns);
    }

    this.evictionStats = (SampledCounter) counterManager.createCounter(new SampledCounterConfig(1, 100, true, 0));
    this.expirationStats = (SampledCounter) counterManager.createCounter(new SampledCounterConfig(1, 100, true, 0));
  }

  @Override
  public void initializeContext(final ConfigurationContext context) {
    evictor.initializeContext(context);
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.evictorSink = scc.getStage(ServerConfigurationContext.SERVER_MAP_EVICTION_PROCESSOR_STAGE).getSink();
  }

  @Override
  public void startEvictor() {
    evictor.startEvictor();
    trigger.registerForResourceEvents(responder);

  }

  @Override
  public void runEvictor() {
    scheduleEvictionRun();
  }

  private Future<SampledRateCounter> scheduleEvictionRun() {
    try {
      clientObjectReferenceSet.size();
      final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
      return new FutureCallable<SampledRateCounter>(agent, new PeriodicCallable(this, objectManager, evictableObjects));
    } catch (ShutdownError err) {
      // is probably in shutdown, unregister
      trigger.unregisterForResourceEvents(responder);
    }
    agent.shutdown();
    return completedFuture;
  }

  @Override
  public void scheduleEvictionTrigger(final EvictionTrigger triggerParam) {
    final SampledRateCounter count = new AggregateSampleRateCounter();
    final Future<SampledRateCounter> run = agent.submit(new Runnable() {
      @Override
      public void run() {
        while (triggerParam.isValid()) {
          doEvictionOn(triggerParam);
          count.increment(triggerParam.getCount(), triggerParam.getRuntimeInMillis());
        }
      }
    }, count);
    print(triggerParam.getName(), run);
  }

  /*
   * return of false means the map is gone
   */

  @Override
  public boolean doEvictionOn(final EvictionTrigger triggerParam) {
    if (Thread.currentThread().getThreadGroup() != this.evictionGrp) {
      scheduleEvictionTrigger(triggerParam);
      return true;
    }

    ObjectID oid = triggerParam.getId();

    if (!evictor.markEvictionInProgress(oid)) { return true; }

    final ManagedObject mo = this.objectManager.getObjectByIDReadOnly(oid);
    try {
      if (mo == null) {
        if (evictor.isLogging()) {
          log("Managed object gone : " + oid);
        }
        return false;
      }

      final ManagedObjectState state = mo.getManagedObjectState();
      final String className = state.getClassName();

      EvictableMap ev = getEvictableMapFrom(mo.getID(), state);
      // if size is zero or cache is pinned or already evicting, exit false
      if (!triggerParam.startEviction(ev)) {
        this.objectManager.releaseReadOnly(mo);
        return true;
      }

      ServerMapEvictionContext context = doEviction(triggerParam, ev, className);

      // Reason for releasing the checked-out object before adding the context to the sink is that we can block on add
      // to the sink because the sink reached max capacity and blocking
      // with a checked-out object will result in a deadlock. @see DEV-5207
      triggerParam.completeEviction(ev);
      this.objectManager.releaseReadOnly(mo);

      if (context != null) {
        int size = context.getRandomSamples().size();
        if (triggerParam instanceof PeriodicEvictionTrigger
            && ((PeriodicEvictionTrigger) triggerParam).isExpirationOnly()) {
          expirationStats.increment(size);
        } else {
          evictionStats.increment(size);
        }
        this.evictorSink.add(context);
      }
    } finally {
      evictor.markEvictionDone(oid);
      if (evictor.isLogging() && logger.isDebugEnabled()) {
        logger.debug(triggerParam);
      }
    }

    return true;
  }

  private ServerMapEvictionContext doEviction(final EvictionTrigger triggerParam, final EvictableMap ev,
                                              final String className) {
    int max = ev.getMaxTotalCount();

    if (max < 0) {
      // cache has no count capacity max is MAX_VALUE;
      max = Integer.MAX_VALUE;
    }

    return triggerParam.collectEvictionCandidates(max, className, ev, clientObjectReferenceSet);
  }

  Future<SampledRateCounter> emergencyEviction(final boolean pre, final int blowout) {
    final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
    List<Future<SampledRateCounter>> push = new ArrayList<Future<SampledRateCounter>>(evictableObjects.size());
    Random r = new Random();
    List<ObjectID> list = new ArrayList<ObjectID>(evictableObjects);

    clientObjectReferenceSet.refreshClientObjectReferencesNow();
    final AggregateSampleRateCounter rate = new AggregateSampleRateCounter();
    while (!list.isEmpty()) {
      final ObjectID mapID = list.remove(r.nextInt(list.size()));
      push.add(agent.submit(new Callable<SampledRateCounter>() {
        @Override
        public SampledRateCounter call() throws Exception {
          EvictionTrigger triggerLocal = (pre) ? new BrakingEvictionTrigger(mapID, blowout)
              : new EmergencyEvictionTrigger(objectManager, mapID, blowout);
          doEvictionOn(triggerLocal);
          rate.increment(triggerLocal.getCount(), triggerLocal.getRuntimeInMillis());
          return rate;
        }
      }));
    }
    return new GroupFuture<SampledRateCounter>(push);
  }

  private EvictableMap getEvictableMapFrom(final ObjectID id, final ManagedObjectState state) {
    if (!PersistentCollectionsUtil.isEvictableMapType(state.getType())) { throw new AssertionError(
                                                                                                   "Received wrong object thats not evictable : "
                                                                                                       + id + " : "
                                                                                                       + state); }
    return (EvictableMap) state;
  }

  private void log(String msg) {
    logger.info(msg);
  }

  @Override
  public void evict(ObjectID oid, Map samples, String className, String cacheName) {
    evictor.evictFrom(oid, samples, className, cacheName);
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return evictor.prettyPrint(out);
  }

  private void print(final String name, final Future<SampledRateCounter> counter) {
    agent.submit(new Runnable() {
      @Override
      public void run() {
        try {
          SampledRateCounter rate = counter.get();
          if (rate == null) { return; }
          if ( evictor.isLogging() ) {
              if (rate.getValue() == 0 ) {
                 log("Eviction Run:" + name + " " + rate + " client references=" + clientObjectReferenceSet.size());
              } else {
                 log("Eviction Run:" + name + " " + rate);
              }
          }
        } catch (ExecutionException exp) {
          logger.warn("eviction run", exp);
          evictionGrp.uncaughtException(Thread.currentThread(), exp);
        } catch (InterruptedException it) {
          logger.warn("eviction run", it);
        }
      }
    });
  }

  class Responder implements ResourceEventListener {

    private long               last        = System.currentTimeMillis();
    private long               epoc        = System.currentTimeMillis();
    private long               size        = 0;
    private boolean            isEmergency = false;
    private float              throttle    = 0f;
    private boolean            isStopped   = false;
    // private long brake = 0;
    private int                turnCount   = 1;

    Future<SampledRateCounter> currentRun  = completedFuture;

    @Override
    public void resourcesUsed(DetailedMemoryUsage usage) {
      try {
        long current = System.currentTimeMillis();
        long max = usage.getMaxMemory();
        long reserve = usage.getReservedMemory();

        if (evictor.isLogging()) {
          if (max != 0) {
            log("Percent usage:" + (usage.getUsedMemory() * 100 / max) + " reserved:" + (reserve * 100 / max)
                + " time:" + (current - last) + " msec. threshold:" + threshold);
          }
        }

        throttleIfNeeded(usage);

        if (threshold.isAboveThreshold(usage, L2_EVICTION_CRITICALTHRESHOLD, L2_EVICTION_HALTTHRESHOLD)) {
          if (!isEmergency || currentRun.isDone()) {
            triggerEmergency(usage);
          }
        } else {
          if (isEmergency) {
            if (!threshold.isAboveThreshold(usage, L2_EVICTION_CRITICALTHRESHOLD, L2_EVICTION_HALTTHRESHOLD)) {
              stopEmergency(usage);
            } else if (currentRun.isDone()) {
              triggerEmergency(usage);
            }
            // wait for generational eviction to do this.
            // } else if ( currentRun.isDone() && brake < usage.getReservedMemory() &&
            // threshold.isInThresholdRegion(usage,L2_EVICTION_CRITICALTHRESHOLD,L2_EVICTION_HALTTHRESHOLD)) {
            // currentRun = emergencyEviction(true, 1);
            // brake = usage.getReservedMemory();
          } else if (PERIODIC_EVICTOR_ENABLED && currentRun.isDone()) {
            currentRun = scheduleEvictionRun();
            print("Periodic", currentRun);
          }
        }
        last = current;
        resetEpocIfNeeded(current, reserve, max);
      } catch (UnsupportedOperationException us) {
        if (currentRun.isDone()) {
          currentRun = scheduleEvictionRun();
        }
        log(us.toString());
      }
    }

    private void throttleIfNeeded(DetailedMemoryUsage usage) {
      // if we are this low, stop no matter what
      if (usage.getReservedMemory() >= usage.getMaxMemory() - (16l * 1024 * 1024)) {
        stop(usage);
      } else if (usage.getReservedMemory() >= usage.getMaxMemory() - (64l * 1024 * 1024)
                 && usage.getUsedMemory() >= usage.getMaxMemory() - (64l * 1024 * 1024)) {
        throttle(usage, 1f);
      }

      if (throttle == 0f && threshold.shouldThrottle(usage, L2_EVICTION_CRITICALTHRESHOLD, L2_EVICTION_HALTTHRESHOLD)) {
        throttle(usage, 0.5f);
      }
    }

    private void stopEmergency(DetailedMemoryUsage usage) {
      isEmergency = false;
      currentRun.cancel(false);
      log("Emergency Eviction Stopped - " + usage.getUsedPercentage());
      turnCount = 1;
      if (isStopped || throttle > 0f) {
        clear(usage);
      }
    }

    private void triggerEmergency(DetailedMemoryUsage usage) {
      log("Emergency Triggered - " + usage.getUsedPercentage() + "/"
          + (usage.getReservedMemory() * 100 / usage.getMaxMemory()) + " turns:" + turnCount);
      currentRun.cancel(false);

      if (turnCount > 6 && isEmergency && !isStopped) {
        if (turnCount > 100) {
          stop(usage);
        } else {
          throttle(usage, (usage.getReservedMemory() * 1f / usage.getMaxMemory()));
        }
      }

      currentRun = emergencyEviction(false, turnCount++);

      // if already in emergency situation, really try hard to remove items.
      print("Emergency", currentRun);
      isEmergency = true;
    }

    /*
     * if resource usage is going down or 5 min have passed, reset the epoc and base size to try and detect rapid growth
     * in the future
     */
    private void resetEpocIfNeeded(long currentTime, long currentSize, long maxSize) {
      if (size == 0 || currentSize < size - (maxSize * .10) || epoc + (5 * 60 * 1000) < currentTime) {
        resetEpoc(currentTime, currentSize);
      }
    }

    private void resetEpoc(long currentTime, long currentSize) {
      epoc = currentTime;
      size = currentSize;
    }

    private void throttle(MemoryUsage reserved, float level) {
      if (isStopped || level <= throttle) { return; }
      resourceManager.setThrottle(level);
      if (throttle == 0f) {
        TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createNearResourceCapacityEvent("pool", reserved
            .getUsedPercentage());
        TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(event);
        resetEpoc(System.currentTimeMillis(), reserved.getUsedMemory());
      }
      throttle = level;
    }

    private void stop(MemoryUsage reserved) {
      if (isStopped) { return; }
      isStopped = true;
      resourceManager.setThrowException();
      TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createFullResourceCapacityEvent("pool", reserved
          .getUsedPercentage());
      TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(event);
    }

    public void clear(MemoryUsage reserved) {
      if (throttle == 0f && !isStopped) { return; }
      isStopped = false;
      throttle = 0f;
      // brake = 0;
      resourceManager.clear();
      TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createNormalResourceCapacityEvent("pool", reserved
          .getUsedPercentage());
      TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(event);
      resetEpoc(System.currentTimeMillis(), reserved.getUsedMemory());
    }
  }
}
