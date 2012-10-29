/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

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
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.persistence.PersistentCollectionsUtil;
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.terracotta.corestorage.monitoring.MonitoredResource;

/**
 *
 * @author mscott
 */
public class ProgressiveEvictionManager implements ServerMapEvictionManager  {
      private static final TCLogger               logger                          = TCLogging
                                                                                  .getLogger(ProgressiveEvictionManager.class);
    private final ServerMapEvictionManagerImpl evictor;
    private final ResourceMonitor trigger;
    private final PersistentManagedObjectStore store;
    private final ObjectManager  objectManager;
    private final ClientObjectReferenceSet clientObjectReferenceSet;
    private static final long SLEEP_TIME = 1000 * 60;
    private long lastEmergency = 0;
  private Sink                                evictorSink;
  private final Timer                               expiry = new Timer("Expiry Timer", true);
  private final Set<ObjectID>                         expirySet = new ObjectIDSet();
  private int                                   serverSizeHint = 256;
  
    public ProgressiveEvictionManager(ObjectManager mgr, MonitoredResource monitored, PersistentManagedObjectStore store, ClientObjectReferenceSet clients, ServerTransactionFactory trans, TCThreadGroup grp) {
        this.objectManager = mgr;
        this.store = store;
        this.clientObjectReferenceSet = clients;
        this.evictor = new ServerMapEvictionManagerImpl(mgr, store, clients, trans,1000 * 60 * 60 * 24); //  periodic is once a day
        this.trigger = new ResourceMonitor(monitored, SLEEP_TIME , grp);
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
        trigger.registerForMemoryEvents(new Responder());
        runEvictor();
    }

    @Override
  public void runEvictor() {
    final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
    serverSizeHint = evictableObjects.size();
    for (final ObjectID mapID : evictableObjects) {
      doEvictionOn(mapID, true);
    }
  }
/*
 * return of false means the map is gone
 */

    @Override
    public boolean doEvictionOn(ObjectID oid, boolean periodicEvictorRun) {
        if ( !evictor.markEvictionInProgress(oid) ) {
            return true;
        }
        
        try {
          final ManagedObject mo = this.objectManager.getObjectByIDOrNull(oid);
          if (mo == null) { 
              log("Managed object gone : " + oid);
              return false; 
          }

          final ManagedObjectState state = mo.getManagedObjectState();
          final String className = state.getClassName();

          EvictableMap ev = getEvictableMapFrom(mo.getID(), state);
          if ( periodicEvictorRun && !ev.startEviction() ) {
              this.objectManager.releaseReadOnly(mo);
              log("Already evicting on " + ev.getCacheName() + " periodic mode " + periodicEvictorRun);
              return true;
          }

          ServerMapEvictionContext context = doEviction(oid, ev, className, ev.getCacheName(),periodicEvictorRun);
          if (context == null) {
              ev.evictionCompleted();
              log("Nothing to evict " + ev.getCacheName() + " periodic mode " + periodicEvictorRun);
          }
        // Reason for releasing the checked-out object before adding the context to the sink is that we can block on add
        // to the sink because the sink reached max capacity and blocking
        // with a checked-out object will result in a deadlock. @see DEV-5207
          this.objectManager.releaseReadOnly(mo);
          if ( context != null ) {
              this.evictorSink.add(context);
          }
      } finally {
          evictor.markEvictionDone(oid);
      }

      return true;
    }
        
    private int calculateSampleCount(EvictableMap ev,boolean emergency) {
        int samples = ev.getMaxTotalCount();
        if ( samples == 0 ) {// zero has special meaning, it's either pinned or a store
            return 0;
        }
        samples = ( emergency ) ? samples/50:samples/10;
        if ( samples < 100 ) {
            samples = 100;
        } else if ( samples > 1000000 ) {
            samples = 1000000;
        }
        return samples;
    }
    
    private void scheduleExpiry(final ObjectID oid,final int ttl,final int tti) {
        if ( ttl <= 0 && tti <= 0 ) {
            return;
        }
        
        synchronized ( expirySet ) {
            if ( !expirySet.contains(oid) ) {
                expirySet.add(oid);
                log("Scheduling eviction on " + oid + " in " + ((ttl > tti ? ttl : tti) * 1000 * 4) + " with tti/ttl:" + tti + "/" + ttl);
                expiry.schedule(new TimerTask() {

                      @Override
                      public void run() {
                          boolean exists = doEvictionOn(oid, true);
                          expirySet.remove(oid);
                          if ( exists ) {
                              scheduleExpiry(oid, ttl, tti);
                          }
                      }

                    },(ttl > tti ? ttl : tti) * 1000 * 4);
            }
        }
    }
    
  private ServerMapEvictionContext doEviction(final ObjectID oid, final EvictableMap ev,
                                              final String className,
                                              final String cacheName,boolean periodic) {
    final int currentSize = ev.getSize();
    
    if (currentSize == 0) {
        log("Map size is zero nothing to evict " + oid);
      return null;
    }
    
    int sampleCount = calculateSampleCount(ev,false);
    if ( sampleCount <= 0 ) {
        log("Zero capacity count on " + oid + " " + cacheName + " it's a store or pinned");
        return null;
    }
    
    int overflow = currentSize - ev.getMaxTotalCount();
    if ( overflow > 0 ) {
        log("Overflow on " + oid + " " + cacheName + " evicting " + overflow);
        sampleCount = overflow;
        periodic = false;
    }

    
    final int ttl = ev.getTTLSeconds();
    final int tti = ev.getTTISeconds();

    scheduleExpiry(oid,ttl,tti);

    Map samples = ev.getRandomSamples(sampleCount, clientObjectReferenceSet);
    log("Sampled " + oid + " " + cacheName + " " + samples.size());

    samples = evictor.filter(oid, samples, tti, ttl, sampleCount, cacheName, !periodic);
    log("Filtered " + oid + " " + cacheName + " " + samples.size());

    if (samples.isEmpty()) {
        return null;
    } else {
      return new ServerMapEvictionContext(oid, samples, className, cacheName);
    }
  }
  
  void emergencyEviction() {
    final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
    boolean handled = false;
    boolean blowout = ( lastEmergency> 0 && lastEmergency - System.currentTimeMillis() < SLEEP_TIME );  // if the last emergency is less than the default sleep time, blow it out
    while ( !handled ) {
        for (final ObjectID mapID : evictableObjects) {
          if ( emergencyEviction(mapID,blowout) > 0 || blowout ) {
              handled = true;
          }; 
        }
        blowout = true;
    }
    lastEmergency = System.currentTimeMillis();
  }
  
  int emergencyEviction(final ObjectID oid, boolean blowout) {
      if ( !evictor.markEvictionInProgress(oid) ) {
            return 0;
        }
     final ManagedObject mo = this.objectManager.getObjectByIDOrNull(oid);
   EvictableMap ev = null;
   Map samples = null; 
   String cacheName = null;
   String className = null;
   try {
        final ManagedObjectState state = mo.getManagedObjectState();
        className = state.getClassName();

        ev = getEvictableMapFrom(mo.getID(), state);

        int overshoot =  ev.getSize() - ev.getMaxTotalCount();
        int sampleCount = calculateSampleCount(ev, blowout);
        
        if ( sampleCount == 0 ) {
            return 0;
        }

        if ( !ev.startEviction() ) {
            return 0;
        }

        if ( overshoot > sampleCount ) {
            sampleCount = overshoot;
        }
                
        cacheName = ev.getCacheName();

        samples = ev.getRandomSamples(sampleCount, clientObjectReferenceSet);
        samples = evictor.filter(oid, samples, ev.getTTISeconds(), ev.getTTLSeconds(), sampleCount, cacheName, blowout);

        if ( samples == null || samples.isEmpty() ) {
            ev.evictionCompleted();
            return 0;
        }
    } finally {
      this.objectManager.releaseReadOnly(mo);
      evictor.markEvictionDone(oid);
    }
    if ( !samples.isEmpty() ) {
        evictor.evictFrom(oid, samples, className, cacheName);
        if ( evictor.isLogging() ) {
            log("emergency evicted: " + samples.size());
        }
    }

    return samples.size();
  }
    
  private EvictableMap getEvictableMapFrom(final ObjectID id, final ManagedObjectState state) {
    if (!PersistentCollectionsUtil.isEvictableMapType(state.getType())) { throw new AssertionError(
                                                                                                   "Received wrong object thats not evictable : "
                                                                                                       + id + " : "
                                                                                                       + state); }
    return (EvictableMap) state;
  }
  
  private void log(String msg) {
    logger.info("Resource Monitor Eviction - " + msg);
  }
  
    @Override
    public void evict(ObjectID oid, Map samples, String className, String cacheName) {
        evictor.evictFrom(oid, samples, className, cacheName);
        if ( evictor.isLogging() &&!samples.isEmpty() ) {
            log("Evicted: " + samples.size() + " from: " + className + "/" + cacheName);
        }
    }

    @Override
    public PrettyPrinter prettyPrint(PrettyPrinter out) {
        return evictor.prettyPrint(out);
    }
    
    class Responder implements MemoryEventsListener {
        long last = System.currentTimeMillis();
        @Override
        public void memoryUsed(MemoryUsage usage, boolean recommendOffheap) {
            try {
                int percent = usage.getUsedPercentage();
                long current = System.currentTimeMillis();
                if ( evictor.isLogging() ) {
                    log("Percent usage:" + percent + " time:" + ((current-last)/1000) + " sec");
                }
                last = current;
                if ( percent > 90 ) {
                    emergencyEviction();
                } else if ( percent > 10 ) {  /*  at 10% usage, run the evictor */
                    runEvictor();
                }
            } catch ( UnsupportedOperationException us ) {
                runEvictor();
            }
        }
        
    }
}
