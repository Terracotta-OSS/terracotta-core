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
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;
import com.tc.stats.counter.CounterManager;
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
 *
 * @author mscott
 */
public class ProgressiveEvictionManager implements ServerMapEvictionManager {

    private static final TCLogger logger = TCLogging
            .getLogger(ProgressiveEvictionManager.class);
    private static final long L2_EVICTION_RESOURCEPOLLINGINTERVAL = TCPropertiesImpl
            .getProperties()
            .getLong(TCPropertiesConsts.L2_EVICTION_RESOURCEPOLLINGINTERVAL, -1);
      private final static boolean                PERIODIC_EVICTOR_ENABLED        = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getBoolean(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED, true);
    private static final int L2_EVICTION_CRITICALTHRESHOLD = TCPropertiesImpl
            .getProperties()
            .getInt(TCPropertiesConsts.L2_EVICTION_CRITICALTHRESHOLD, 90);
    private static final int L2_EVICTION_HALTTHRESHOLD = TCPropertiesImpl
            .getProperties()
            .getInt(TCPropertiesConsts.L2_EVICTION_HALTTHRESHOLD, 98);
     private static final long L2_EVICTION_CRITICALUPPERBOUND = TCPropertiesImpl
            .getProperties()
            .getLong(TCPropertiesConsts.L2_EVICTION_CRITICALUPPERBOUND, 10l * 1024 * 1024 * 1024);
     private static final long L2_EVICTION_CRITICALLOWERBOUND = TCPropertiesImpl
            .getProperties()
            .getLong(TCPropertiesConsts.L2_EVICTION_CRITICALLOWERBOUND, 16l * 1024 * 1024);    /*  3 max offheap pages  */
    private final ServerMapEvictionEngine evictor;
    private final ResourceMonitor trigger;
    private final PersistentManagedObjectStore store;
    private final ObjectManager objectManager;
    private final ClientObjectReferenceSet clientObjectReferenceSet;
    private Sink evictorSink;
    private final ExecutorService agent;
    private ThreadGroup        evictionGrp;
    private final Responder responder =        new Responder();
    private final SampledRateCounter expirationStats;
    private final SampledRateCounter evictionStats;
  private final ResourceManager resourceManager;
    
    private final static Future<SampledRateCounter> completedFuture = new Future<SampledRateCounter>() {

        private final SampledRateCounter zeroStats = new SampledRateCounterImpl(new SampledRateCounterConfig(5, 100, false));
        
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
    public SampledRateCounter getExpirationStatistics() {
        return expirationStats;
    }
     
    @Override
    public SampledRateCounter getEvictionStatistics() {
        return evictionStats;
    }
    
    public ProgressiveEvictionManager(ObjectManager mgr, MonitoredResource monitored, PersistentManagedObjectStore store, ClientObjectReferenceSet clients, ServerTransactionFactory trans, final TCThreadGroup grp, final ResourceManager resourceManager, final CounterManager counterManager) {
        this.objectManager = mgr;
        this.store = store;
        this.clientObjectReferenceSet = clients;
      this.resourceManager = resourceManager;
      this.evictor = new ServerMapEvictionEngine(mgr, trans);
        //  assume 100 MB/sec fill rate and set 0% usage poll rate to the time it would take to fill up.
        this.evictionGrp = new ThreadGroup(grp, "Eviction Group") {

            @Override
            public void uncaughtException(Thread thread, Throwable thrwbl) {
                getParent().uncaughtException(thread, thrwbl);
            }
            
        };
        long sleeptime = L2_EVICTION_RESOURCEPOLLINGINTERVAL;
        if ( sleeptime < 0 ) {
    //  100MB a second
            sleeptime = (monitored.getTotal() * 1000) / (100 * 1024 * 1024);
        }
        this.trigger = new ResourceMonitor(monitored, sleeptime, evictionGrp);      
        this.agent = new ThreadPoolExecutor(1, 64, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),new ThreadFactory() {
            private int count = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(evictionGrp, r, "Expiration Thread - " + count++);
                return t;
            }
        });
        try {
            Runnable rb = new Runnable() {

                @Override
                public void run() {
                    resourceManager.setThrowException();
                }

            };
            if ( monitored.getType() == MonitoredResource.Type.HEAP ) {
                monitored.addUsedThreshold(MonitoredResource.Direction.RISING, L2_EVICTION_HALTTHRESHOLD*monitored.getTotal()/100, rb);
            }
            if ( monitored.getType() == MonitoredResource.Type.OFFHEAP ) {
                monitored.addReservedThreshold(MonitoredResource.Direction.RISING, L2_EVICTION_HALTTHRESHOLD*monitored.getTotal()/100, rb);
            }
        } catch ( UnsupportedOperationException uns ) {
            logger.info("threshold monitor not registered", uns);
        }
        
        log("critical threshold " + L2_EVICTION_CRITICALTHRESHOLD);
      this.evictionStats = (SampledRateCounter)counterManager.createCounter(new SampledRateCounterConfig(5, 100, true));
      this.expirationStats = (SampledRateCounter)counterManager.createCounter(new SampledRateCounterConfig(5, 100, true));
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
        trigger.registerForMemoryEvents(responder);
    }

    @Override
    public void runEvictor() {
            scheduleEvictionRun();
    }
    
    private Future<SampledRateCounter> scheduleEvictionRun() {
        try{
            clientObjectReferenceSet.refreshClientObjectReferencesNow();
            final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
            return new FutureCallable<SampledRateCounter>(agent, new PeriodicCallable(this,objectManager,evictableObjects,evictor.isElementBasedTTIorTTL()));
        } catch ( ShutdownError err ) {
            //  is probably in shutdown, unregister
            trigger.unregisterForMemoryEvents(responder);
        }
        agent.shutdown();
        return completedFuture;
    }
    
    public void scheduleEvictionTrigger(final EvictionTrigger triggerParam) {
        final SampledRateCounter count = new AggregateSampleRateCounter();
        final Future<SampledRateCounter> run = agent.submit(new Runnable() {
            @Override
            public void run()  {
                doEvictionOn(triggerParam);
                count.increment(triggerParam.getCount(), triggerParam.getRuntimeInMillis());
            }
        },count);
        print(triggerParam.getName(), run);
    }
    /*
     * return of false means the map is gone
     */

    @Override
    public boolean doEvictionOn(final EvictionTrigger triggerParam) {
        if ( Thread.currentThread().getThreadGroup() != this.evictionGrp ) {
            scheduleEvictionTrigger(triggerParam);
            return true;
        }
        
        ObjectID oid = triggerParam.getId();

        if (!evictor.markEvictionInProgress(oid)) {
            return true;
        }

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
            if ( !triggerParam.startEviction(ev) ) {
                this.objectManager.releaseReadOnly(mo);
                return true;
            }
            ServerMapEvictionContext context = doEviction(triggerParam, ev, className, ev.getCacheName());

            // Reason for releasing the checked-out object before adding the context to the sink is that we can block on add
            // to the sink because the sink reached max capacity and blocking
            // with a checked-out object will result in a deadlock. @see DEV-5207
            triggerParam.completeEviction(ev);
            if ( context == null && ev.isEvicting() ) {
                throw new AssertionError(triggerParam.toString());
            }
            this.objectManager.releaseReadOnly(mo);
            
            if (context != null) {
                if ( triggerParam instanceof PeriodicEvictionTrigger && ((PeriodicEvictionTrigger)triggerParam).isExpirationOnly() ) {
                    expirationStats.increment(triggerParam.getCount(),triggerParam.getRuntimeInMillis());
                } else {
                    evictionStats.increment(triggerParam.getCount(),triggerParam.getRuntimeInMillis());
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
            final String className,
            final String cacheName) {
        int max = ev.getMaxTotalCount();

        if (max < 0) {
//  cache has no count capacity max is MAX_VALUE;
            max = Integer.MAX_VALUE;
        }

        Map samples = triggerParam.collectEvictonCandidates(max, ev, clientObjectReferenceSet);

        if (samples.isEmpty()) {
            return null;
        } else {
          logger.info("Evicting elements " + samples);
            return new ServerMapEvictionContext(triggerParam, samples, className, cacheName);
        }
    }

    Future<SampledRateCounter> emergencyEviction(final boolean blowout) {
        final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
        List<Future<SampledRateCounter>> push = new ArrayList<Future<SampledRateCounter>>(evictableObjects.size());
        Random r = new Random();
        List<ObjectID> list = new ArrayList<ObjectID>(evictableObjects);

        clientObjectReferenceSet.refreshClientObjectReferencesNow();
        final AggregateSampleRateCounter rate = new AggregateSampleRateCounter();
        while ( !list.isEmpty() ) {
            final ObjectID mapID = list.remove(r.nextInt(list.size()));
            push.add(agent.submit(new Callable<SampledRateCounter>() {
                @Override
                public SampledRateCounter call() throws Exception {
                    EmergencyEvictionTrigger triggerLocal = new EmergencyEvictionTrigger(objectManager, mapID , blowout);
                    doEvictionOn(triggerLocal);
                    rate.increment(triggerLocal.getCount(), triggerLocal.getRuntimeInMillis());
                    return rate;
                }
            }
            ));
        }
        return new GroupFuture<SampledRateCounter>(push);
    }

    private EvictableMap getEvictableMapFrom(final ObjectID id, final ManagedObjectState state) {
        if (!PersistentCollectionsUtil.isEvictableMapType(state.getType())) {
            throw new AssertionError(
                    "Received wrong object thats not evictable : "
                    + id + " : "
                    + state);
        }
        return (EvictableMap) state;
    }

    private void log(String msg) {
        logger.info(msg);
    }

    @Override
    public void evict(ObjectID oid, Map samples, String className, String cacheName) {
        evictor.evictFrom(oid, samples, className, cacheName);
        if (evictor.isLogging() && !samples.isEmpty()) {
            log("Evicted: " + samples.size() + " from: " + className + "/" + cacheName);
        }
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
                    if ( rate == null ) {
                        return;
                    }
                    if (rate.getValue()==0 && evictor.isLogging()) {
                        log("Eviction Run:" + name + " " + rate + " client references=" + clientObjectReferenceSet.size());
                    } else {
                        log("Eviction Run:" + name + " " + rate);
                    }
                } catch ( ExecutionException exp ) {
                    logger.warn("eviction run", exp);
                } catch ( InterruptedException it ) {
                    logger.warn("eviction run", it);
                }
            }
        });
    }

    class Responder implements MemoryEventsListener {

        private long last = System.currentTimeMillis();
        private long epoc = System.currentTimeMillis();
        private long size = 0;
        private boolean isEmergency = false;
        private boolean isThrottling = false;
        private boolean isStopped = false;

        Future<SampledRateCounter> currentRun = completedFuture;

        @Override
        public void memoryUsed(MemoryUsage usage, boolean recommendOffheap) {
            try {
                long current = System.currentTimeMillis();
                long max = usage.getMaxMemory();
                long reserve = usage.getUsedMemory();
                long threshold = calculateThreshold(usage,(max * L2_EVICTION_CRITICALTHRESHOLD / 100));
                if ( evictor.isLogging() ) {
                    log("Percent usage:" + (reserve*100/max) + " time:" + (current - last) + " msec. threshold:" + (threshold*100/max));
                }
//  since removal is so imporant for all eviction operations.  Update this number on every round.
                clientObjectReferenceSet.refreshClientObjectReferencesNow();
                if (reserve >= threshold ) {  
                    if ( !isEmergency || currentRun.isDone() ) {
                        log("Emergency Triggered - " + (reserve * 100 / max));
                        currentRun.cancel(false);
                        if (currentRun != completedFuture) {
                            print("Emergency", currentRun);
                        }
                        
                        if ( isEmergency && !isThrottling && !isStopped ) {
                            throttle(usage);
                        }
                        
                        currentRun = emergencyEviction(isEmergency);// if already in emergency situation, really try hard to remove items.
                        isEmergency = true;
                    }
  //  if we are about to OOME, stop the world
                    if ( reserve >= calculateThreshold(usage,(max * L2_EVICTION_HALTTHRESHOLD / 100)) ) {
                        if ( !isThrottling ) {
                            throttle(usage);
  //  double check one more time, weighting will make the throttling threshold lower than the stopping but make sure
  //  we really need to stop
                        } else if ( !isStopped ) {
                            stop(usage);
                        }
                    }
                } else {
                    if ( isStopped || isThrottling ) {
                        clear(usage);
                    }
                    if ( isEmergency ) {
                        isEmergency = false;
                        currentRun.cancel(false);
                    }
                    if ( PERIODIC_EVICTOR_ENABLED && currentRun.isDone() ) {
                        if (currentRun != completedFuture) {
                            print("Periodic", currentRun);
                        }
                        currentRun = scheduleEvictionRun();
                    }
                }
                last = current;
                resetEpocIfNeeded(current,reserve,max);
            } catch (UnsupportedOperationException us) {
                if ( currentRun.isDone() ) {
                    currentRun = scheduleEvictionRun();
                }
                log(us.toString());
            }
        }
        
        private long calculateThreshold(MemoryUsage usage, long level) {
/*  gap is to handle rapidly growing resource usage.  We take the average growth
 * and based on that, if we are going to go over on the next poll, go ahead and fire now to get a head start.
 * should consider a moving time window.
 * 
 */
            long max = usage.getMaxMemory();
            long reserve = usage.getUsedMemory();
            long aveGrow = (reserve-size) / ( System.currentTimeMillis() - epoc );
            int weight = (isEmergency) ? 1 : 2;
            if ( isThrottling ) {
                weight = 0;
            }
            long gap = ( System.currentTimeMillis() - last ) * aveGrow * weight;
/**
 * if size is zero, our initial grow reading is very suspect, particularly for
 * heap implementations.
 */
            if ( size == 0 || gap < 0 || gap > level / 2) {
                gap = 0;
            }
            long threshold = level - gap;
   //  bounds checks
            if ( threshold < max - L2_EVICTION_CRITICALUPPERBOUND ) {
                threshold = max - L2_EVICTION_CRITICALUPPERBOUND;
            } else if ( threshold > max - L2_EVICTION_CRITICALLOWERBOUND ) {
                threshold = max - L2_EVICTION_CRITICALLOWERBOUND;
            }

            return threshold;
        }
    /*
    * if resource usage is going down or 5 min have passed, reset the epoc and base size to try and detect rapid
    * growth in the future
    */
        private void resetEpocIfNeeded(long currentTime, long currentSize, long maxSize) {
            if ( size == 0 || currentSize < size - ( maxSize *.10 ) || epoc + (5 * 60 * 1000) < currentTime) {
                resetEpoc(currentTime,currentSize);
            }
        }
        
        private void resetEpoc(long currentTime, long currentSize) {
            epoc = currentTime;
            size = currentSize;
        }
        
        private void throttle(MemoryUsage reserved) {
            isThrottling = true;
            resourceManager.setThrottle(1);
            TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createNearResourceCapacityEvent("pool",reserved.getUsedMemory()*100/reserved.getMaxMemory());
            TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(event);
            resetEpoc(System.currentTimeMillis(),reserved.getUsedMemory());
        }
        
        private void stop(MemoryUsage reserved) {
            if ( isStopped ) {
                return;
            }
            isStopped = true;
            resourceManager.setThrowException();
            TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createFullResourceCapacityEvent("pool",reserved.getUsedMemory()*100/reserved.getMaxMemory());
            TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(event);
        }
        
        public void clear(MemoryUsage reserved) {
            if ( !isThrottling ) {
                return;
            }
            isStopped = false;
            isThrottling = false;
            resourceManager.clear();
            TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createNormalResourceCapacityEvent("pool",reserved.getUsedMemory()*100/reserved.getMaxMemory());
            TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(event);
            resetEpoc(System.currentTimeMillis(),reserved.getUsedMemory());
        }
    }
}
