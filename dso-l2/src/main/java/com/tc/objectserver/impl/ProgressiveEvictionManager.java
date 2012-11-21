/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.EvictionTrigger;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.api.ShutdownError;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.persistence.PersistentCollectionsUtil;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounterImpl;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;
//import java.lang.management.MemoryUsage;
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
import java.util.concurrent.TimeoutException;
import org.terracotta.corestorage.monitoring.MonitoredResource;

/**
 *
 * @author mscott
 */
public class ProgressiveEvictionManager implements ServerMapEvictionManager {

    private static final TCLogger logger = TCLogging
            .getLogger(ProgressiveEvictionManager.class);
    private static final int SLEEP_TIME = 60;
    private static final int L2_EVICTION_RESOURCEPOLLINGINTERVAL = TCPropertiesImpl
            .getProperties()
            .getInt(TCPropertiesConsts.L2_EVICTION_RESOURCEPOLLINGINTERVAL, SLEEP_TIME);
      private final static boolean                PERIODIC_EVICTOR_ENABLED        = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getBoolean(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED, true);
    private static final int L2_EVICTION_CRITICALTHRESHOLD = TCPropertiesImpl
            .getProperties()
            .getInt(TCPropertiesConsts.L2_EVICTION_CRITICALTHRESHOLD, 90);
    private final ServerMapEvictionEngine evictor;
    private final ResourceMonitor trigger;
    private final PersistentManagedObjectStore store;
    private final ObjectManager objectManager;
    private final ClientObjectReferenceSet clientObjectReferenceSet;
    private Sink evictorSink;
    private final ExecutorService agent;
    private ThreadGroup        evictionGrp;
    private final Responder responder =        new Responder();
    private final SampledRateCounter expirationStats = new SampledRateCounterImpl(new SampledRateCounterConfig(5, 100, false));
    private final SampledRateCounter evictionStats = new SampledRateCounterImpl(new SampledRateCounterConfig(5, 100, false));
    
    private final static Future<Void> completedFuture = new Future<Void>() {

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
            public Void get() throws InterruptedException, ExecutionException {
                return null;
            }

            @Override
            public Void get(long l, TimeUnit tu) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
            
    };
            
     public SampledRateCounter getExpirationStatistics() {
        return expirationStats;
    }
     
    public SampledRateCounter getEvictionStatistics() {
        return evictionStats;
    }
    
    private void resetStatistics() {
        evictionStats.setValue(0,0);
        expirationStats.setValue(0,0);
    }
    
    public ProgressiveEvictionManager(ObjectManager mgr, MonitoredResource monitored, PersistentManagedObjectStore store, ClientObjectReferenceSet clients, ServerTransactionFactory trans, final TCThreadGroup grp) {
        this.objectManager = mgr;
        this.store = store;
        this.clientObjectReferenceSet = clients;
        this.evictor = new ServerMapEvictionEngine(mgr, trans);
        //  assume 100 MB/sec fill rate and set 0% usage poll rate to the time it would take to fill up.
        this.evictionGrp = new ThreadGroup(grp, "Eviction Group") {

            @Override
            public void uncaughtException(Thread thread, Throwable thrwbl) {
                getParent().uncaughtException(thread, thrwbl);
            }
            
        };
        this.trigger = new ResourceMonitor(monitored, (monitored.getTotal() * 1000) / (100 * 1024 * 1024), evictionGrp);      
        this.agent = new ThreadPoolExecutor(1, 64, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),new ThreadFactory() {
            private int count = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(evictionGrp, r, "Expiration Thread - " + count++);
                return t;
            }
        });
        log("critical threshold " + L2_EVICTION_CRITICALTHRESHOLD);
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
    
    private Future<Void> scheduleEvictionRun() {
        try{
            resetStatistics();
            final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
            return new FutureCallable<Void>(agent, new PeriodicCallable(this,objectManager,evictableObjects,evictor.isElementBasedTTIorTTL()));
        } catch ( ShutdownError err ) {
            //  is probably in shutodown, unregister
            trigger.unregisterForMemoryEvents(responder);
        }
        agent.shutdown();
        return completedFuture;
    }
    
    public void scheduleEvictionTrigger(final EvictionTrigger trigger) {    
        agent.submit(new Runnable() {
            @Override
            public void run()  {
                doEvictionOn(trigger);
            }
        });        
    }
    /*
     * return of false means the map is gone
     */

    @Override
    public boolean doEvictionOn(final EvictionTrigger trigger) {        
        if ( Thread.currentThread().getThreadGroup() != this.evictionGrp ) {
            scheduleEvictionTrigger(trigger);
            return true;
        }
        
        ObjectID oid = trigger.getId();

        if (!evictor.markEvictionInProgress(oid)) {
            return true;
        }

        final ManagedObject mo = this.objectManager.getObjectByIDOrNull(oid);
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
            if ( ev.getSize() == 0 ||
                 ev.getMaxTotalCount() == 0 ||
                 !trigger.startEviction(ev) ) {
                this.objectManager.releaseReadOnly(mo);
                return true;
            }
            ServerMapEvictionContext context = doEviction(trigger, ev, className, ev.getCacheName());

            // Reason for releasing the checked-out object before adding the context to the sink is that we can block on add
            // to the sink because the sink reached max capacity and blocking
            // with a checked-out object will result in a deadlock. @see DEV-5207
            trigger.completeEviction(ev);
            if ( context == null && ev.isEvicting() ) {
                throw new AssertionError(trigger.toString());
            }
            this.objectManager.releaseReadOnly(mo);
            
            if (context != null) {
                if ( trigger instanceof PeriodicEvictionTrigger && ((PeriodicEvictionTrigger)trigger).isExpirationOnly() ) {
                    expirationStats.increment(trigger.getCount(),trigger.getRuntimeInSeconds());
                } else {
                    evictionStats.increment(trigger.getCount(),trigger.getRuntimeInSeconds());
                }
                this.evictorSink.add(context);
            } 
        } finally {
            evictor.markEvictionDone(oid);
            if (evictor.isLogging()) {
                log("Evictor results " + trigger);
            }
        }

        return true;
    }

    private ServerMapEvictionContext doEviction(final EvictionTrigger trigger, final EvictableMap ev,
            final String className,
            final String cacheName) {
        int max = ev.getMaxTotalCount();

        if (max < 0) {
//  cache has no count capacity max is MAX_VALUE;
            max = Integer.MAX_VALUE;
        }

        Map samples = trigger.collectEvictonCandidates(max, ev, clientObjectReferenceSet);

        if (samples.isEmpty()) {
            return null;
        } else {
            return new ServerMapEvictionContext(trigger, samples, className, cacheName);
        }
    }

    Future<Void> emergencyEviction(final boolean blowout) {
        final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
        List<Future<Void>> push = new ArrayList<Future<Void>>(evictableObjects.size());
        Random r = new Random();
        List<ObjectID> list = new ArrayList<ObjectID>(evictableObjects);
        resetStatistics();
        while ( !list.isEmpty() ) {
            final ObjectID mapID = list.remove(r.nextInt(list.size()));
            push.add(agent.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    EmergencyEvictionTrigger trigger = new EmergencyEvictionTrigger(objectManager, mapID , blowout);
                    doEvictionOn(trigger);
                    return null;
                }
            }
            ));
        }
        return new GroupFuture(push);
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

    class Responder implements MemoryEventsListener {

        long last = System.currentTimeMillis();
        long epoc = System.currentTimeMillis();
        long size = 0;
        volatile boolean isEmergency = false;

        Future<Void> currentRun = completedFuture;

        @Override
        public void memoryUsed(MemoryUsage usage, boolean recommendOffheap) {
            try {
                int percent = usage.getUsedPercentage();
                long current = System.currentTimeMillis();
                if (evictor.isLogging()) {
                    log("Percent usage:" + percent + " time:" + (current - last) + " msec.");
                }
                long max = usage.getMaxMemory();
                long reserve = usage.getUsedMemory();


                if (reserve >= calculateThreshold(reserve,max)) {                    
                    if ( !isEmergency || currentRun.isDone() ) {
                        log("Emergency Triggered - " + (reserve*100/max));
                        currentRun.cancel(false);
                        currentRun = emergencyEviction(isEmergency);// if already in emergency situation, really try hard to remove items.
                        isEmergency = true;
                    }
                } else {
                    clientObjectReferenceSet.size();
                    if ( isEmergency ) {
                        isEmergency = false;
                        currentRun.cancel(false);
                    }
                    if ( PERIODIC_EVICTOR_ENABLED && currentRun.isDone() ) {
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
        
        private long calculateThreshold(long reserve, long max) {
/*  gap is to handle rapidly growing resource usage.  We take the average growth
 * and based on that, if we are going to go over on the next poll, go ahead and fire now to get a head start.
 * should consider a moving time window.
 */
            long aveGrow = (reserve-size) / ( System.currentTimeMillis() - epoc );
            long gap = ( System.currentTimeMillis() - last ) * aveGrow;
            if ( gap < 0 ) {
                gap = 0;
            }
            return (max * L2_EVICTION_CRITICALTHRESHOLD / 100) - gap;
        }
    /* 
    * if resource usage is going down or 5 min have passed, reset the epoc and base size to try and detect rapid 
    * growth in the future
    */        
        private void resetEpocIfNeeded(long currenTime, long currentSize, long maxSize) {
            if ( currentSize < size - ( maxSize *.10 ) || epoc + (5 * 60 * 1000) < currenTime) {
                epoc = currenTime;
                size = maxSize;
            }
        }
    }
}
