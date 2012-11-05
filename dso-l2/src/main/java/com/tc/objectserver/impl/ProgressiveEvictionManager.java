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
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;
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
    
    private static final int                    SLEEP_TIME = 60;
    private static final int                    L2_CACHEMANAGER_RESOURCEPOLLINGINTERVAL = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getInt(TCPropertiesConsts.L2_CACHEMANAGER_RESOURCEPOLLINGINTERVAL,SLEEP_TIME);              
              
    private final ServerMapEvictionEngine       evictor;
    private final ResourceMonitor               trigger;
    private final PersistentManagedObjectStore  store;
    private final ObjectManager                 objectManager;
    private final ClientObjectReferenceSet      clientObjectReferenceSet;
    private long                                lastEmergency = 0;
    private Sink                                evictorSink;
    private final Timer                         expiry = new Timer("Expiry Timer", true);
    private final Set<ObjectID>                 expirySet = new ObjectIDSet();
    private int                                 serverSizeHint = 256;
  
    public ProgressiveEvictionManager(ObjectManager mgr, MonitoredResource monitored, PersistentManagedObjectStore store, ClientObjectReferenceSet clients, ServerTransactionFactory trans, TCThreadGroup grp) {
        this.objectManager = mgr;
        this.store = store;
        this.clientObjectReferenceSet = clients;
        this.evictor = new ServerMapEvictionEngine(mgr, trans); 
        this.trigger = new ResourceMonitor(monitored, L2_CACHEMANAGER_RESOURCEPOLLINGINTERVAL * 1000, grp);
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
        expiry.schedule(new TimerTask() {
            @Override
            public void run() {
                runEvictor();
            }
        }, ServerMapEvictionEngine.DEFAULT_SLEEP_TIME, ServerMapEvictionEngine.DEFAULT_SLEEP_TIME
        );
    }

    @Override
  public void runEvictor() {
    final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
    serverSizeHint = evictableObjects.size();
    for (final ObjectID mapID : evictableObjects) {
      doEvictionOn(new PeriodicEvictionTrigger(objectManager, mapID, evictor.isElementBasedTTIorTTL()));
    }
  }
/*
 * return of false means the map is gone
 */

    @Override
    public boolean doEvictionOn(final EvictionTrigger trigger) {
        ObjectID  oid = trigger.getId();
        
        if ( !evictor.markEvictionInProgress(oid) ) {
            return true;
        }
        
        try {
          final ManagedObject mo = this.objectManager.getObjectByIDOrNull(oid);
          if (mo == null) { 
              if ( evictor.isLogging() ) {
                log("Managed object gone : " + oid);
              }
              return false; 
          }

          final ManagedObjectState state = mo.getManagedObjectState();
          final String className = state.getClassName();

          EvictableMap ev = getEvictableMapFrom(mo.getID(), state);
          if ( !trigger.startEviction(ev) ) {
              this.objectManager.releaseReadOnly(mo);
              return true;
          }

          ServerMapEvictionContext context = doEviction(trigger, ev, className, ev.getCacheName());
          if (context == null) {
              trigger.completeEviction(ev);
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
          if ( evictor.isLogging() ) {
              log("Evictor results " + trigger);
          }
      }

      return true;
    }
    
    private void scheduleExpiry(final ObjectID oid,final int ttl,final int tti) {
        if ( ttl <= 0 && tti <= 0 ) {
            return;
        }
        
        synchronized ( expirySet ) {
            if ( !expirySet.contains(oid) ) {
                expirySet.add(oid);
                if ( evictor.isLogging() ) {
                    log("Scheduling eviction on " + oid + " in " + ((ttl > tti ? ttl : tti) * 1000 * 2) + " with tti/ttl:" + tti + "/" + ttl);
                }
                expiry.schedule(new TimerTask() {

                      @Override
                      public void run() {
                          boolean exists = doEvictionOn(new PeriodicEvictionTrigger(objectManager, oid, evictor.isElementBasedTTIorTTL()));
                          expirySet.remove(oid);
                          if ( exists ) {
                              scheduleExpiry(oid, ttl, tti);
                          }
                      }

                    },(ttl > tti ? ttl : tti) * 1000 * 2);
            }
        }
    }
    
  private ServerMapEvictionContext doEviction(final EvictionTrigger trigger, final EvictableMap ev,
                                              final String className,
                                              final String cacheName) {
    final int currentSize = ev.getSize();
    int max = ev.getMaxTotalCount();
    
    if (currentSize == 0) {
      return null;
    }
    
    if ( max == 0 ) {
        if ( evictor.isLogging() ) {
            log(ev.getCacheName() + " is pinned or a store");
        }
        return null;
    }
    
    if ( max < 0 ) {
//  cache has no count capacity max is MAX_VALUE;
        max = Integer.MAX_VALUE;
    }
    
    Map samples = trigger.collectEvictonCandidates(max, ev,clientObjectReferenceSet);

    if (samples.isEmpty()) {
        return null;
    } else {
      return new ServerMapEvictionContext(trigger, samples, className, cacheName);
    }
  }
  
  void emergencyEviction() {
    final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
    boolean handled = false;
    boolean blowout = ( lastEmergency> 0 && lastEmergency - System.currentTimeMillis() < SLEEP_TIME );  // if the last emergency is less than the default sleep time, blow it out
    while ( !handled ) {
        for (final ObjectID mapID : evictableObjects) {
            EmergencyEvictionTrigger trigger = new EmergencyEvictionTrigger(objectManager,mapID,blowout);
            doEvictionOn(trigger);
          if ( trigger.getSampleCount() > 0 || blowout ) {
              handled = true;
          }; 
        }
        blowout = true;
    }
    lastEmergency = System.currentTimeMillis();
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
                } else {
             // polling this force refresh and trigger any capacity evictors that didn't finish there job
                    clientObjectReferenceSet.size();
                }
            } catch ( UnsupportedOperationException us ) {
                runEvictor();
                log(us.toString());
            }
        }
        
    }
}
