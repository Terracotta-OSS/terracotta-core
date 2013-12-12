/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.persistence.EvictionTransactionPersistor;
import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.EvictionListener;
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
import com.tc.text.PrettyPrinter;
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author mscott
 */
public class ProgressiveEvictionManager implements ServerMapEvictionManager {
    
  private static final double                     DEFAULT_RATE = 10; // MB/sec

  private static final TCLogger                   logger                              = TCLogging
                                                                                          .getLogger(ProgressiveEvictionManager.class);
  private static final long                       L2_EVICTION_RESOURCEPOLLINGINTERVAL = TCPropertiesImpl
                                                                                          .getProperties()
                                                                                          .getLong(TCPropertiesConsts.L2_EVICTION_RESOURCEPOLLINGINTERVAL,
                                                                                                   -1);
  private final static boolean                    PERIODIC_EVICTOR_ENABLED            = TCPropertiesImpl
                                                                                           .getProperties()
                                                                                           .getBoolean(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED,
                                                                                                       true);
  private final ServerMapEvictionEngine           evictor;
  private final ResourceEventProducer             trigger; 
  private final Collection<MonitoredResource>     resources;
  private final PersistentManagedObjectStore      store;
  private final ObjectManager                     objectManager;
  private final ClientObjectReferenceSet          clientObjectReferenceSet;
  private Sink                                    evictorSink;
  private final ExecutorService                   agent;
  private final ThreadLocal<EvictionTrigger>      currentTrigger = new ThreadLocal<EvictionTrigger>();
  private ThreadGroup                             evictionGrp;
  private final Responder                         responder                           = new Responder();
  private final SampledCounter                    expirationStats;
  private final SampledCounter                    evictionStats;
  private final ResourceManager                   resourceManager;
  private final EvictionThreshold                 threshold;
  private final AggregateSampleRateCounter        pulse                               = new AggregateSampleRateCounter();
  private final AtomicInteger                     emergencyCount                      = new AtomicInteger();

  private final static Future<SampledRateCounter> completedFuture                     = new Future<SampledRateCounter>() {

                                                                                        private final AggregateSampleRateCounter zeroStats = new AggregateSampleRateCounter();

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

  public ProgressiveEvictionManager(final ObjectManager mgr, final Collection<MonitoredResource> monitorList, final PersistentManagedObjectStore store,
                                    final ClientObjectReferenceSet clients, final ServerTransactionFactory trans,
                                    final TCThreadGroup grp, final ResourceManager resourceManager,
                                    final CounterManager counterManager,
                                    final EvictionTransactionPersistor evictionTransactionPersistor, final boolean hybrid, final boolean persistent) {
    this.objectManager = mgr;
    this.store = store;
    this.clientObjectReferenceSet = clients;
    this.resourceManager = resourceManager;
    this.evictor = new ServerMapEvictionEngine(mgr, trans, evictionTransactionPersistor, persistent);

    final MonitoredResource monitored = getEvictionBasedResource(monitorList, hybrid);
    
    if ( !hybrid ) {
      this.resources = Collections.singletonList(monitored);
    } else {
      this.resources = monitorList;
    }
    
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
      sleeptime = (monitored.getTotal() * 1000) / (256 * 1024 * 1024);
      if (sleeptime > 120 * 1000) {
        // max out at 2 min.
        sleeptime = 120 * 1000;
      }
    }
    this.threshold = EvictionThreshold.configure(monitored);
    try {
        log("Using threshold " + this.threshold + " for total size " + Conversion.memoryBytesAsSize(monitored.getTotal()));
    } catch ( Conversion.MetricsFormatException m ) {
        log("Using threshold " + this.threshold + " for total size " + monitored.getTotal());
    }

//    if ( !hybrid ) {
//      this.trigger = new ResourceMonitor(monitored, sleeptime, evictionGrp);
//    } else {
      this.trigger = new MultiResourceMonitor(this.resources, evictionGrp, threshold, sleeptime, hybrid, persistent);
//    }
      
    this.evictionGrp = new ThreadGroup(evictionGrp, "Eviction Worker Group");

    this.agent = new ThreadPoolExecutor(4, 64, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                                        new ThreadFactory() {
                                          private int count = 1;

                                          @Override
                                          public Thread newThread(Runnable r) {
                                            Thread t = new Thread(evictionGrp, r, "Expiration Thread - " + count++);
                                            return t;
                                          }
                                        }, new ThreadPoolExecutor.AbortPolicy());
    try {
      Runnable rb = new Runnable() {

        @Override
        public void run() {
          log("Threshold crossed used:" + monitored.getVital() + " reserved:" + monitored.getReserved() + " total:" + monitored.getTotal());
          resourceManager.setRestricted();
        }

      };
      if (monitored.getType() == MonitoredResource.Type.OFFHEAP) {
        monitored
            .addReservedThreshold(MonitoredResource.Direction.RISING, monitored.getTotal() - 16l * 1024 * 1024, rb);
      }
    } catch (UnsupportedOperationException uns) {
      logger.info("threshold monitor not registered", uns);
    }

    this.evictionStats = (SampledCounter) counterManager.createCounter(new SampledCounterConfig(1, 100, true, 0));
    this.expirationStats = (SampledCounter) counterManager.createCounter(new SampledCounterConfig(1, 100, true, 0));
  }
  
  private static MonitoredResource getEvictionBasedResource(Collection<MonitoredResource> list, boolean hybrid) {
    MonitoredResource best = null;
    for (MonitoredResource rsrc : list) {
        if ( best == null ) {
            best = rsrc;
        } else if ( hybrid && rsrc.getType() == MonitoredResource.Type.DATA ) {
            best = rsrc;
        } else if ( rsrc.getType() == MonitoredResource.Type.OFFHEAP) {
            best = rsrc;
        }
    }
    return best;
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
    schedulePeriodicEvictionRun(null);
  }
  
    // used only for tests
  ServerMapEvictionEngine getEngine() {
    return evictor;
  }
 
  // used only for tests
  void shutdownEvictor() {
    agent.shutdown();
    try {
      agent.awaitTermination(10, TimeUnit.SECONDS);
    } catch ( InterruptedException ie ) {
      logger.debug("bad test termintation", ie);
    }
  }

  Future<SampledRateCounter> schedulePeriodicEvictionRun(Set<ObjectID> evictableObjects) {
    try {
      clientObjectReferenceSet.size();
      if ( evictableObjects == null ) {
        evictableObjects = store.getAllEvictableObjectIDs();
      }
      return new FutureCallable<SampledRateCounter>(agent, new PeriodicCallable(this, evictableObjects));
    } catch (ShutdownError err) {
      // is probably in shutdown, unregister
      trigger.unregisterForResourceEvents(responder);
    }
    agent.shutdown();
    return completedFuture;
  }
    
  @Override
  public boolean scheduleCapacityEviction(ObjectID oid) {
    if ( evictor.markEvictionInProgress(oid) ) {
      CapacityEvictionTrigger ceTrigger = new CapacityEvictionTrigger(this, oid);
      return doEvictionOn(ceTrigger);
    }
    return false;
  }
  
  PeriodicEvictionTrigger schedulePeriodicEviction(ObjectID oid) {
    if ( evictor.markEvictionInProgress(oid) ) {
      PeriodicEvictionTrigger periodic = new PeriodicEvictionTrigger(objectManager, oid);
      doEvictionOn(periodic);
      return periodic;
    }
    return null;
  }

  private boolean scheduleEvictionTrigger(final EvictionTrigger triggerParam) {
    try {
      if ( evictor.markEvictionInProgress(triggerParam.getId()) ) {
        throw new AssertionError("target should already be locked for eviction");
      }
      final SampledRateCounter count = new AggregateSampleRateCounter();
      final Future<SampledRateCounter> run = agent.submit(new Runnable() {
        @Override
        public void run() {
          doEvictionOn(triggerParam);
          count.increment(triggerParam.getCount(), triggerParam.getRuntimeInMillis());
        }
      }, count);
      print(triggerParam.getName(), run);
      return true;
    } catch ( RejectedExecutionException rejected ) {
      evictor.markEvictionDone(triggerParam.getId());
    }
    return false;
  }
  
  /*
   * return of false means the map is gone
   */

  @Override
  public boolean doEvictionOn(final EvictionTrigger triggerParam) {
    if (Thread.currentThread().getThreadGroup() != this.evictionGrp ||
          currentTrigger.get() != null ) {
      return scheduleEvictionTrigger(triggerParam);
    }

    ObjectID oid = triggerParam.getId();

    currentTrigger.set(triggerParam);
    
    boolean isDone = false;

    final ManagedObject mo = this.objectManager.getObjectByIDReadOnly(oid);
    try {
      if (mo == null) {
        if (evictor.isLogging()) {
          log("Managed object gone : " + oid);
        }
        isDone = true;
      } else {
        if ( evictor.markEvictionInProgress(oid) ) {
          throw new AssertionError("not evicting");
        }
        final ManagedObjectState state = mo.getManagedObjectState();
        final String className = state.getClassName();

        EvictableMap ev = getEvictableMapFrom(mo.getID(), state);
        // ignore start eviction status
        if ( !triggerParam.startEviction(ev) ) {
          this.objectManager.releaseReadOnly(mo);
          isDone = true;
        } else {
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
          } else {
            isDone = !triggerParam.isValid();
          }
        }
      }
            
      if ( isDone ) {
        evictor.markEvictionDone(oid);
      }
    } finally {
      if (evictor.isLogging() && logger.isDebugEnabled()) {
        logger.debug(triggerParam);
      }
      currentTrigger.remove();
    }
    return isDone;
  }

  private ServerMapEvictionContext doEviction(final EvictionTrigger triggerParam, final EvictableMap ev,
                                              final String className) {
    int max = ev.getMaxTotalCount();

    if (max < 0 || !ev.isEvictionEnabled() ) {  //  check again because the flag could have changed
      // cache has no count capacity max is MAX_VALUE;
      max = Integer.MAX_VALUE;
    }

    return triggerParam.collectEvictionCandidates(max, className, ev, clientObjectReferenceSet);
  }

  Future<SampledRateCounter> emergencyEviction(final int blowout) {
    final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
    List<Future<SampledRateCounter>> push = new ArrayList<Future<SampledRateCounter>>(evictableObjects.size());
    Random r = new Random();
    List<ObjectID> list = new ArrayList<ObjectID>(evictableObjects);

    clientObjectReferenceSet.refreshClientObjectReferencesNow();
    final AggregateSampleRateCounter rate = new AggregateSampleRateCounter();
    while (!list.isEmpty()) {
      final ObjectID mapID = list.remove(r.nextInt(list.size()));
      try {
          push.add(agent.submit(new Callable<SampledRateCounter>() {
            @Override
            public SampledRateCounter call() throws Exception {
              if ( evictor.markEvictionInProgress(mapID) ) {
                EvictionTrigger triggerLocal = new EmergencyEvictionTrigger(mapID, blowout);
                doEvictionOn(triggerLocal);
                emergencyCount.addAndGet(triggerLocal.getCount());
                rate.increment(triggerLocal.getCount(), triggerLocal.getRuntimeInMillis());
              }
              return rate;
            }
          }));
      } catch ( RejectedExecutionException rejected ) {

      }
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
  public void evict(ObjectID oid, Map<Object,EvictableEntry> samples, String className, String cacheName) {
    evictor.evictFrom(oid, samples, cacheName);
  }

  void addEvictionListener(EvictionListener evl) {
    evictor.addEvictionListener(evl);
  }

  void removeEvictionListener(EvictionListener evl) {
    evictor.removeEvictionListener(evl);
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return evictor.prettyPrint(out);
  }
  
  Set<ObjectID> getCurrentlyEvicting() {
    return evictor.currentlyEvicting();
  }

  private void print(final String name, final Future<SampledRateCounter> counter) {
    try {
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
            pulse.increment(rate.getValue());
          } catch (ExecutionException exp) {
            logger.warn("eviction run", exp);
            evictionGrp.uncaughtException(Thread.currentThread(), exp);
          } catch (InterruptedException it) {
            logger.warn("eviction run", it);
          } catch ( CancellationException cancelled ) {
            logger.debug("eviction run cancelled");
          }
        }
      });
    } catch ( RejectedExecutionException rejected ) {
      // ignore
    }
  }
  
  class Responder implements ResourceEventListener {

    private long               epoc        = System.currentTimeMillis();
    private long               size        = 0;
    private boolean            isEmergency = false;
    private float              throttle    = 0f;
    private long               throttlePoll = 0L;
    private boolean            isStopped   = false;
    private int                turnCount   = 1;
    private long               notified    = 0;
    private long               tick        = 0;
    
    private final Set<MonitoredResource.Type> evictions = EnumSet.noneOf(MonitoredResource.Type.class);
    private final Set<MonitoredResource.Type> throttles = EnumSet.noneOf(MonitoredResource.Type.class);
    private final Set<MonitoredResource.Type> stops = EnumSet.noneOf(MonitoredResource.Type.class);

    Future<SampledRateCounter> currentRun  = completedFuture;

    @Override
    public void resourcesUsed(MonitoredResource usage) {
        long max = usage.getTotal();

        if ((evictor.isLogging() && System.currentTimeMillis() - tick > (10*1000)) || System.currentTimeMillis() - tick > (60*1000) ) {
          if (max != 0) {
            try {
                for ( MonitoredResource rsrc : ProgressiveEvictionManager.this.resources ) {
                    log("Resource usage: " + rsrc.getType() + 
                        " - used:" + Conversion.memoryBytesAsSize(rsrc.getVital()) +
                        " reserved:" + Conversion.memoryBytesAsSize(rsrc.getReserved()) +
                        " max:" + Conversion.memoryBytesAsSize(rsrc.getTotal()) 
                    );
                }
            } catch ( Conversion.MetricsFormatException me ) {
                logger.warn("bad usage info", me);
            } catch ( NumberFormatException number ) {
                logger.warn("bad usage info", number);
            }
            tick = System.currentTimeMillis();
            long count = pulse.getAndReset();
            if ( count > 0 ) {
                log("Evicted and expired " + count + " total elements");
            }
          }
        }
        if (!isEmergency && PERIODIC_EVICTOR_ENABLED && currentRun.isDone()) {
          currentRun = schedulePeriodicEvictionRun(null);
          print("Periodic", currentRun);
        }
        throttleIfNeeded(usage);
    }
    

    @Override
    public void resourcesConstrained(final MonitoredResource usage) {
      try {
        log("RESOURCE CONSTRAINED - Performance may be degraded due to " + usage.getType()
            + " used:" + Conversion.memoryBytesAsSize(usage.getVital()) + " reserved:" + Conversion.memoryBytesAsSize(usage.getReserved())
            + " total:" + Conversion.memoryBytesAsSize(usage.getTotal()));
      } catch ( Conversion.MetricsFormatException format ) {
        logger.info("unable to print usage", format);
      }
      
      notifyThrottling(new MemoryUsage() {

          @Override
          public long getFreeMemory() {
            return usage.getTotal() - usage.getReserved();
          }

          @Override
          public String getDescription() {
            return usage.getType().toString();
          }

          @Override
          public long getMaxMemory() {
            return usage.getTotal();
          }

          @Override
          public long getUsedMemory() {
            return usage.getReserved();
          }

          @Override
          public int getUsedPercentage() {
            return (int)(100 * usage.getReserved() / usage.getTotal());
          }

          @Override
          public long getCollectionCount() {
            return 0;
          }

          @Override
          public long getCollectionTime() {
            return 0;
          }
        
        }
        );
    }    

    @Override
    public void resourcesFreed(final MonitoredResource usage) {
      try {
        log("Performance no longer degraded due to " + usage.getType()
            + " used:" + Conversion.memoryBytesAsSize(usage.getVital()) + " reserved:" + Conversion.memoryBytesAsSize(usage.getReserved())
            + " total:" + Conversion.memoryBytesAsSize(usage.getTotal()));
      } catch ( Conversion.MetricsFormatException format ) {
        logger.info("unable to print usage", format);
      }
            
      notifyAllClear(new MemoryUsage() {

          @Override
          public long getFreeMemory() {
            return usage.getTotal() - usage.getReserved();
          }

          @Override
          public String getDescription() {
            return usage.getType().toString();
          }

          @Override
          public long getMaxMemory() {
            return usage.getTotal();
          }

          @Override
          public long getUsedMemory() {
            return usage.getReserved();
          }

          @Override
          public int getUsedPercentage() {
            return (int)(100 * usage.getReserved() / usage.getTotal());
          }

          @Override
          public long getCollectionCount() {
            return 0;
          }

          @Override
          public long getCollectionTime() {
            return 0;
          }
      });
    }
    
    
 
    @Override
    public void requestEvictions(MonitoredResource usage) {
      evictions.add(usage.getType());
      if ( !isEmergency || currentRun.isDone() ) {
        triggerEmergency(usage);
      }
    }

    @Override
    public void cancelEvictions(MonitoredResource usage) {
      if ( evictions.remove(usage.getType()) && evictions.isEmpty() ) {
        stopEmergency(usage);
      } else if (PERIODIC_EVICTOR_ENABLED && currentRun.isDone()) {
        currentRun = schedulePeriodicEvictionRun(null);
        print("Periodic", currentRun);
      }
    }   

    @Override
    public void requestThrottle(MonitoredResource usage) {
      if ( throttles.isEmpty() || throttles.contains(usage.getType()) ) {
        throttles.add(usage.getType());
        controlledThrottle(usage);
      }
    }

    @Override
    public void cancelThrottle(MonitoredResource usage) {
      resourceManager.resetState();
      throttles.remove(usage.getType());
      if ( this.notified > 0 && this.notified < System.currentTimeMillis() - (60 * 1000) ) {
        notifyAllClear(new DetailedMemoryUsage(usage, turnCount));
      }
    }

    @Override
    public void requestStop(MonitoredResource usage) {
      stops.add(usage.getType());
      stop(new DetailedMemoryUsage(usage, turnCount));
    }

    @Override
    public void cancelStop(MonitoredResource usage) {
      if ( stops.remove(usage.getType()) && stops.isEmpty() ) {
        clear(new DetailedMemoryUsage(usage, turnCount));
      }
    }
   
    private void completeEvictions() {
      if ( currentRun.isDone() ) {
        try {
          currentRun.get();
          if ( isEmergency && emergencyCount.get() == 0 ) {
            turnCount = 1;
          }
        } catch ( InterruptedException ie ) {
          logger.warn(ie);
        } catch ( ExecutionException ee ) {
          logger.warn(ee);
        } catch ( CancellationException cancelled)  {
          logger.debug(cancelled);
        }
      } else {
        currentRun.cancel(false);
      }
    }

    private void throttleIfNeeded(MonitoredResource usage) {
      // if we are this low, stop no matter what
      if (usage.getVital() >= usage.getTotal() - (16l * 1024 * 1024) && usage.getVital() > usage.getTotal() / 2 ) {
          if ( !isStopped ) {
            logger.warn("resource usage at max");
          }
          stop(new DetailedMemoryUsage(usage, turnCount));
      } else if ( (usage.getReserved() >= usage.getTotal()- (128l * 1024 * 1024)
          && usage.getVital()>= usage.getTotal() - (256l * 1024 * 1024) ) ) {
            if ( System.nanoTime() - throttlePoll > TimeUnit.SECONDS.toNanos(2) ) {
              controlledThrottle(usage);
            }
      }
    }

    private void stopEmergency(MonitoredResource usage) {
      currentRun.cancel(false);
      completeEvictions();
      int counted = 0;
      counted = emergencyCount.get();
      isEmergency = false;
      log("Resource Eviction Stopped - " + (usage.getVital()* 100 / usage.getTotal())  + "/"
          + (usage.getReserved()* 100 / usage.getTotal()) + " evicted count:" + counted);
      turnCount = 1;
      if (isStopped || throttle > 0f) {
        clear(new DetailedMemoryUsage(usage, turnCount));
      }
    }
    
    private void controlledThrottle(MonitoredResource usage) {
      long nanoTime = System.nanoTime();
      long total = usage.getTotal();
      double start = total / 2D;
      long used = usage.getVital();
      double amt = (total - used) / start;

      double rate = calculateRate(nanoTime,used);
      float setThrottle = this.throttle;
      if ( setThrottle == 0 ) {
          setThrottle = 0.001f;
      }
      if ( rate < (DEFAULT_RATE * (amt)) ) {
          setThrottle *= .90; 
      } else {
          setThrottle *= 1.10;
      }

      if ( setThrottle < 0f ) {
          setThrottle = .000001f;
          resetEpoc(nanoTime,used);
      } else if ( setThrottle > 1.0 ) {
          setThrottle = .999999f;
          resetEpoc(nanoTime,used);
      } else {
          resetEpocIfNeeded(nanoTime, used, usage.getTotal());
      }
      
      if ( setThrottle < 0.001 ) {
        cancelThrottle(usage);
      } else if ( setThrottle > .999 ) {
        stop(new DetailedMemoryUsage(usage, 0));
      } else {
        throttle(usage, setThrottle); 
      }
      throttlePoll = nanoTime;
    }

    private void triggerEmergency(MonitoredResource usage) {
      if ( !isEmergency ) {
          log("Resource Eviction Triggered - " + (usage.getVital()* 100 / usage.getTotal()) + "/"
            + (usage.getReserved()* 100 / usage.getTotal()));
          emergencyCount.set(0);
      }
      if ( evictor.isLogging() && logger.isDebugEnabled() ) {
          logger.debug("Emergency triggered with usage " + usage);
      }
      currentRun.cancel(false);

      if (turnCount > 5 && isEmergency && !isStopped) {
        long nanoTime = System.nanoTime();
        if (turnCount > 100000) {
          logger.warn("turn count:" + turnCount);
          stop(new DetailedMemoryUsage(usage, turnCount));
        } else if ( nanoTime - throttlePoll > TimeUnit.SECONDS.toNanos(10) ) {
          if ( this.throttle < 1 ) {
              controlledThrottle(usage);
          }
        }
      }

      currentRun = emergencyEviction(turnCount++);

      // if already in emergency situation, really try hard to remove items.
      print("Emergency", currentRun);
      isEmergency = true;
    }

    /*
     * if resource usage is going down or 5 min have passed, reset the epoc and base size to try and detect rapid growth
     * in the future
     */
    private void resetEpocIfNeeded(long currentTime, long currentSize, long maxSize) {
      if (size == 0 || currentSize < size - (maxSize * .10) || epoc + (5 * 60 * TimeUnit.SECONDS.toNanos(1)) < currentTime) {
        resetEpoc(currentTime, currentSize);
      }
    }
    
    private double calculateRate(long currentTime, long currentSize) {
        if ( size == 0 || epoc == 0 ) {
            return 0;
        }
        try {
            if (evictor.isLogging() && logger.isDebugEnabled()) {
                logger.debug("Throttling size:" + ((currentSize - size)/(1024D*1024D)) + " time:" +  ((currentTime - epoc)/TimeUnit.SECONDS.toNanos(1)));
            }
            return ((currentSize - size)/(1024D*1024D)) / ((currentTime - epoc)/TimeUnit.SECONDS.toNanos(1));
        } finally{
//            resetEpoc(currentTime, currentSize);
        }
    }

    private void resetEpoc(long currentTime, long currentSize) {
      epoc = currentTime;
      size = currentSize;
    }

    private void throttle(MonitoredResource reserved, float level) {
      if (isStopped) { return; }
      resourceManager.setThrottle(level);
      if (shouldNotify()) {
        notifyThrottling(new DetailedMemoryUsage(reserved, turnCount));
      }
      throttle = level;
      try {
        log("Throttling clients to " + throttle +" due to " + reserved.getType()
            + " used:" + Conversion.memoryBytesAsSize(reserved.getVital()) + " reserved:" + Conversion.memoryBytesAsSize(reserved.getReserved())
            + " total:" + Conversion.memoryBytesAsSize(reserved.getTotal()));
      } catch ( Conversion.MetricsFormatException format ) {
        logger.info("unable to print usage", format);
      }
    }
    
    private boolean shouldNotify() {
        try {
            if ( throttle == 0f && notified == 0 ) {
                return true;
            }
            return false;
        } finally {
            notified = System.currentTimeMillis();
        }
    }

    private void stop(MemoryUsage reserved) {
      if (isStopped) { return; }
      isStopped = true;
      resourceManager.setRestricted();
      TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createFullResourceCapacityEvent("pool", reserved
          .getUsedPercentage());
      TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(event);
      try {
        log("Stopping Clients due to " + reserved.getDescription()
          + " used:" + Conversion.memoryBytesAsSize(reserved.getUsedMemory()) 
          + " total:" + Conversion.memoryBytesAsSize(reserved.getMaxMemory()));
      } catch ( Conversion.MetricsFormatException format ) {
        logger.info("unable to report stoppage", format);
      }
    }
    
    private void notifyThrottling(MemoryUsage usage) {
        TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createNearResourceCapacityEvent("pool", usage
            .getUsedPercentage());
        TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(event);
        resetEpoc(System.nanoTime(), usage.getUsedMemory());
    }
    
    private void notifyAllClear(MemoryUsage reserved) {
      TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createNormalResourceCapacityEvent("pool", reserved
        .getUsedPercentage());
      TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(event);
      resetEpoc(System.nanoTime(), reserved.getUsedMemory());
      notified = 0;
    }

    public void clear(MemoryUsage reserved) {
      if ( isStopped ) {
          notifyAllClear(reserved);
      }
      if (throttle == 0f && !isStopped) { return; }
      isStopped = false;
      throttle = 0f;
      // brake = 0;
      resourceManager.resetState();
    }
  }
}
