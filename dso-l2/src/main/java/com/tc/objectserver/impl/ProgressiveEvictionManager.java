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
import java.util.Map;
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
    private static final int EVICT_NOW_TTL_TTI = 0;
//    private final ClientObjectReferenceSet clientObjectReferenceSet = new ClientObjectReferenceSet(new ClientStateManager() {
//
//        @Override
//        public boolean startupNode(NodeID nodeID) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        @Override
//        public void shutdownNode(NodeID deadNode) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        @Override
//        public void addReference(NodeID nodeID, ObjectID objectID) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        @Override
//        public void removeReferences(NodeID nodeID, Set<ObjectID> removed, Set<ObjectID> requested) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        @Override
//        public boolean hasReference(NodeID nodeID, ObjectID objectID) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        @Override
//        public List<DNA> createPrunedChangesAndAddObjectIDTo(Collection<DNA> changes, ApplyTransactionInfo references, NodeID clientID, Set<ObjectID> objectIDs, Invalidations invalidationsForClient) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        @Override
//        public Set<ObjectID> addAllReferencedIdsTo(Set<ObjectID> rescueIds) {
//            return rescueIds;
//        }
//
//        @Override
//        public void removeReferencedFrom(NodeID nodeID, Set<ObjectID> secondPass) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        @Override
//        public Set<ObjectID> addReferences(NodeID nodeID, Set<ObjectID> oids) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        @Override
//        public int getReferenceCount(NodeID nodeID) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        @Override
//        public Set<NodeID> getConnectedClientIDs() {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        @Override
//        public void registerObjectReferenceAddListener(ObjectReferenceAddListener listener) {
//        }
//
//        @Override
//        public void unregisterObjectReferenceAddListener(ObjectReferenceAddListener listener) {
//        }
//    });
    
    private final ClientObjectReferenceSet clientObjectReferenceSet;
  private Sink                                evictorSink;
  
    public ProgressiveEvictionManager(ObjectManager mgr, MonitoredResource monitored, PersistentManagedObjectStore store, ClientObjectReferenceSet clients, ServerTransactionFactory trans, TCThreadGroup grp) {
        this.objectManager = mgr;
        this.store = store;
        this.clientObjectReferenceSet = clients;
        this.evictor = new ServerMapEvictionManagerImpl(mgr, store, clients, trans,1000 * 60 * 60 * 24); //  periodic is once a day
        this.trigger = new ResourceMonitor(monitored, 1000 * 10 *1 /*  10 sec initial sleep time */, 10 /* 10% change is happy place */ , grp);
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
    }

    @Override
  public void runEvictor() {
    final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
    
    for (final ObjectID mapID : evictableObjects) {
      doEvictionOn(mapID, true);
    }
  }


    @Override
    public void doEvictionOn(ObjectID oid, boolean periodicEvictorRun) {
    
    final ManagedObject mo = this.objectManager.getObjectByIDOrNull(oid);
    ServerMapEvictionContext context = null;
    if (mo == null) { return; }
    
    evictor.markEvictionInProgress(oid);
    final ManagedObjectState state = mo.getManagedObjectState();
    final String className = state.getClassName();

    EvictableMap ev = null;
    try {
      ev = getEvictableMapFrom(mo.getID(), state);
      context = doEviction(oid, ev, className, ev.getCacheName());
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
      evictor.markEvictionDone(oid);
    }
    }
    
    private int calculateSampleCount(EvictableMap ev,boolean emergency) {
        int samples = ev.getMaxTotalCount();
        samples = ( emergency ) ? samples/50:samples/10;
        if ( samples < 100 ) {
            samples = 100;
        } else if ( samples > 10000 ) {
            samples = 10000;
        }
        return samples;
    }
    
      /**
   * Collects random samples and initiates eviction
   * 
   * @return true, if eviction is initiated, false otherwise
   */
  private ServerMapEvictionContext doEviction(final ObjectID oid, final EvictableMap ev,
                                              final String className,
                                              final String cacheName) {
    final int targetMaxTotalCount = ev.getMaxTotalCount();
    final int currentSize = ev.getSize();
    if (currentSize == 0) {
      return null;
    }

    final int ttl = ev.getTTLSeconds();
    final int tti = ev.getTTISeconds();

    final Map samples = ev.getRandomSamples(calculateSampleCount(ev,false), clientObjectReferenceSet);

    if (samples.isEmpty()) {
      return null;
    } else {
      return new ServerMapEvictionContext(oid, targetMaxTotalCount, tti, ttl, samples, samples.size(), className, cacheName);
    }
  }
  
  void emergencyEviction() {
    final ObjectIDSet evictableObjects = store.getAllEvictableObjectIDs();
    
    for (final ObjectID mapID : evictableObjects) {
      if ( emergencyEviction(mapID) > 0 ) {
          // do nothing
      }; 
    }
  }
  
  int emergencyEviction(final ObjectID oid) {
     final ManagedObject mo = this.objectManager.getObjectByIDOrNull(oid);
   EvictableMap ev = null;
   Map samples = null; 
   String cacheName = null;
   String className = null;
   try {
       objectManager.start();
        final ManagedObjectState state = mo.getManagedObjectState();
        className = state.getClassName();

        ev = getEvictableMapFrom(mo.getID(), state);
        cacheName = ev.getCacheName();

        samples = ev.getRandomSamples(calculateSampleCount(ev, true), clientObjectReferenceSet);
        if ( samples == null || samples.isEmpty() ) {
            return 0;
        }
        samples = evictor.filter(oid, samples, ev.getTTISeconds(), ev.getTTLSeconds(), ev.getMaxTotalCount(), cacheName, true);
    } finally {
      ev.evictionCompleted();
      this.objectManager.releaseReadOnly(mo);
    }
    evictor.evictFrom(oid, samples, className, cacheName);
    if ( evictor.isLogging() && !samples.isEmpty() ) {
        log("emergency evicted: " + samples.size());
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
    public void evict(ObjectID oid, Map samples, int targetMaxTotalCount, int ttiSeconds, int ttlSeconds, int overshoot, String className, String cacheName) {
        samples = evictor.filter(oid, samples, ttiSeconds, ttlSeconds, overshoot, cacheName, false);
        evictor.evictFrom(oid, samples, className, cacheName);
        if ( evictor.isLogging() &&!samples.isEmpty() ) {
            log("evicted: " + samples.size());
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
                if ( percent > 80 ) {
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
