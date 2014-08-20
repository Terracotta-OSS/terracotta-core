/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.invalidation.Invalidations;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.api.ObjectReferenceAddListener;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This trigger is fired by the periodic TTI/TTL eviction scheduler as well as resource
 * monitoring events and tries to evict expired items first but will also evict 
 * live items if the current count is over the max count.
 * 
 * The attempted sample count is 10% of the current size of the segment but can 
 * vary based on L1 client references.  If current count is grater than max count,
 * the difference is used as the sample count.
 * 
 * 
 * @author mscott
 */
public class PeriodicEvictionTrigger extends AbstractEvictionTrigger {
    
    private final boolean runAlways;
    private boolean dumpLive = false;
    private int count = 0;
    private int sampled = 0;
    private int sampleAmount = 100;
    private int filtered = 0;
    private int alive = 0;
    private int expired = 0;
    private int overflow = 0;
    private int excluded = 0;
    private int missing = 0;
    private int tti = 0;
    private int ttl = 0;
    private boolean completed = false;
    private volatile boolean stop = false;
    private final ObjectManager  mgr;
    private final ObjectIDSet exclusionList;
    private final ObjectIDSet passList = new BitSetObjectIDSet();
    
    private static final ClientObjectReferenceSet noReference = new ClientObjectReferenceSet(new ClientStateManager() {

        @Override
        public boolean startupNode(NodeID nodeID) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void shutdownNode(NodeID deadNode) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean addReference(NodeID nodeID, ObjectID objectID) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removeReferences(NodeID nodeID, Set<ObjectID> removed, Set<ObjectID> requested) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasReference(NodeID nodeID, ObjectID objectID) {
            return false;
        }

        @Override
        public List<DNA> createPrunedChangesAndAddObjectIDTo(Collection<DNA> changes, ApplyTransactionInfo references, NodeID clientID, Set<ObjectID> objectIDs, Invalidations invalidationsForClient) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set<ObjectID> addAllReferencedIdsTo(Set<ObjectID> rescueIds) {
            return Collections.<ObjectID>emptySet();
        }

        @Override
        public void removeReferencedFrom(NodeID nodeID, Set<ObjectID> secondPass) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set<ObjectID> addReferences(NodeID nodeID, Set<ObjectID> oids) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getReferenceCount(NodeID nodeID) {
            return 0;
        }

        @Override
        public Set<NodeID> getConnectedClientIDs() {
            return Collections.<NodeID>emptySet();
        }

        @Override
        public void registerObjectReferenceAddListener(ObjectReferenceAddListener listener) {
        }

        @Override
        public void unregisterObjectReferenceAddListener(ObjectReferenceAddListener listener) {
        }
    });
    
    public PeriodicEvictionTrigger(ObjectManager mgr, ObjectID oid) {
        this(mgr,oid,new BitSetObjectIDSet(), true);
    }
    
    public PeriodicEvictionTrigger duplicate() {
        PeriodicEvictionTrigger nt = new PeriodicEvictionTrigger(mgr,getId());
        nt.sampleAmount = sampleAmount - 5;
        if ( nt.sampleAmount <= 0 ) {
            nt.sampleAmount = 1;
        }
        return nt;
    }
    
    public PeriodicEvictionTrigger(ObjectManager mgr, ObjectID oid, ObjectIDSet exclude, boolean runAlways) {
        super(oid);
        this.runAlways = runAlways;
        this.mgr = mgr;
        this.exclusionList = exclude;
    }
    
    public ObjectIDSet getExclusionList() {
        return passList;
    }

    @Override
    public int getCount() {
        return filtered;
    }
    
    protected ObjectManager getObjectManager() {
        return mgr;
    }
    
    public boolean isExpirationOnly() {
        return !dumpLive;
    }

    @Override
    public boolean startEviction(EvictableMap map) {
        if ( stop ) {
          return false;
        }
        tti = map.getTTISeconds();
        ttl = map.getTTLSeconds();
        if ( tti > 0 || 
          ttl > 0 || 
          runAlways ||
          map.getSize() > map.getMaxTotalCount() ) {

          return super.startEviction(map);
        }
        return false;
    }

    @Override
    public void completeEviction(EvictableMap map) {
        completed = true;
        super.completeEviction(map);
    }
    
    public void stop() {
        stop = true;
    }

    @Override
    public ServerMapEvictionContext collectEvictionCandidates(int max, String className, EvictableMap map,
                                                              ClientObjectReferenceSet clients) {
        int samples = calculateSampleCount(max, map);

        final SamplingType samplingType = (dumpLive) ? SamplingType.FOR_EVICTION : SamplingType.FOR_EXPIRATION;
        Map<Object, EvictableEntry> grabbed = ( !stop ) ?
            map.getRandomSamples(samples, (dumpLive) ? clients : noReference, samplingType) :
            Collections.<Object,EvictableEntry>emptyMap();

        sampled = grabbed.size();

        if ( dumpLive ) {
            overflow += grabbed.size();
        }

        return createEvictionContext(className, grabbed);
    }

    @Override
    protected ServerMapEvictionContext createEvictionContext(String className, Map<Object, EvictableEntry> sample) {
        ServerMapEvictionContext cxt = super.createEvictionContext(className, sample);
        if ( cxt == null ) {
            return null;
        }
        return new PeriodicServerMapEvictionContext(cxt);
    }

    protected int calculateSampleCount(int max, EvictableMap ev) {
        count = ev.getSize();

        sampled = count/sampleAmount;
        
        if ( sampled < 100 ) {
            sampled = 100;
        }
        
        if ( ev.isEvictionEnabled() && count - max > 0 ) {
            sampled = count - max;
            dumpLive = true;
        }
        
        return boundsCheckSampleSize(sampled);
    }
    
    
    public float filterRatio() {
        if ( expired == 0 ) {
            return 0f;
        }
        return expired * 1.0f / expired + excluded + alive;
    }
    
  private Map<Object, EvictableEntry> filter(final Map<Object, EvictableEntry> samples, final int ttiSeconds,
                    final int ttlSeconds) {
    final int now = (int) (System.currentTimeMillis() / 1000);
    final Iterator<Map.Entry<Object, EvictableEntry>> iterator = samples.entrySet().iterator();
    while ( iterator.hasNext() ) {
        if ( stop ) {
 //  don't unset flag, may need it later
            return Collections.emptyMap();
        }
      final Map.Entry<Object, EvictableEntry> e = iterator.next();
        long expiresIn = expiresIn(now, e.getValue(), ttiSeconds, ttlSeconds);
        if ( expiresIn == 0 ) {
  //  didn't find the object, ignore this one
            iterator.remove();
            missing += 1;
        } else if ( exclusionList != null && exclusionList.contains(e.getValue().getObjectID()) ) {
            iterator.remove();
            excluded += 1;
        } else if ( expiresIn < 0 ) {
//            candidates.put(e.getKey(), e.getValue());
            expired+=1;
        } else {
            alive+=1;
 //  know what we have already tested
 //  factor in a freshness value?
           iterator.remove();
           passList.add(e.getValue().getObjectID());
        }
    }
    return samples;
 }   
  /**
   * This method tries to compute when an entry will expire relative to "now". If the value is not an EvictableEntry or
   * if tti/ttl is 0 and the property ehcache.storageStrategy.dcv2.perElementTTITTL.enable is not set to true, then it
   * always returns 0, ie. Expire Now.
   * 
   * @return when values is going to expire relative to "now", a negative number or zero indicates the value is expired.
   */
  protected long expiresIn(final int now, final EvictableEntry value, final int ttiSeconds, final int ttlSeconds) {
    return value.expiresIn(now, ttiSeconds, ttlSeconds);
  }

  static final class ExpiryKey implements Comparable {

    private final int   expiresIn;
    private final Map.Entry e;

    public ExpiryKey(int expiresIn, Map.Entry e) {
      this.expiresIn = expiresIn;
      this.e = e;
    }

    public int expiresIn() {
      return expiresIn;
    }

    public Map.Entry getEntry() {
      return e;
    }

    @Override
    public int compareTo(Object o) {
      return expiresIn - ((ExpiryKey) o).expiresIn;
    }

  }
  
  
    @Override
    public String getName() {
        return "Periodic";
    }

    @Override
    public String toString() {
        String flag = ( expired + overflow > 0 ) ? ", ELEMENTS_EVICTED" : "";
        String element = ( runAlways ) ? "RUN_ALWAYS, " : "";
        String fullRun = ( completed ) ? ", COMPLETED" : "";
        return "PeriodicEvictionTrigger{" 
                + element
                + "over capacity=" + dumpLive 
                + ", count=" + count
                + ", sampled=" + sampled
                + ", filtered=" + filtered
                + ", overflow=" + overflow 
                + ", expired=" + expired 
                + ", missing=" + missing 
                + ", excluded=" + excluded 
                + ", tti/ttl=" + tti + "/" + ttl 
                + ", alive=" + alive 
                + ", parent=" + super.toString()
                + fullRun
                + flag
                + '}';
    }
    
    class PeriodicServerMapEvictionContext extends ServerMapEvictionContext {
        ServerMapEvictionContext root;
        Map<Object, EvictableEntry> cached;
        
        public PeriodicServerMapEvictionContext(ServerMapEvictionContext root) {
            super(PeriodicEvictionTrigger.this,  root.getTTISeconds(), root.getTTLSeconds(), root.getRandomSamples(), root.getClassName(), root.getCacheName());
        }
//  do this to move the filtering outside the scope of the ServerMap lock
        @Override
        public Map<Object, EvictableEntry> getRandomSamples() {
            if ( dumpLive ) {
                return super.getRandomSamples();
            }
            if ( cached == null ) {
                cached = filter(super.getRandomSamples(),tti,ttl);
                filtered = cached.size();
            }
            return cached;
        }
        
    }
  
  
}
