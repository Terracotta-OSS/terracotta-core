/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author mscott
 */
public class PeriodicEvictionTrigger extends AbstractEvictionTrigger {
    
    private final boolean ELEMENT_BASED_TTI_TTL_ENABLED;
    private boolean dumpLive = false;
    private int sampled = 0;
    private int alive = 0;
    private int expired = 0;
    private int overflow = 0;
    private String cacheName;
    private final ObjectManager  mgr;

    public PeriodicEvictionTrigger(ObjectManager mgr, ObjectID oid, boolean ELEMENT_BASED_TTI_TTL_ENABLED) {
        super(oid);
        this.ELEMENT_BASED_TTI_TTL_ENABLED = ELEMENT_BASED_TTI_TTL_ENABLED;
        this.mgr = mgr;
    }

    @Override
    public boolean startEviction(EvictableMap map) {
        cacheName = map.getCacheName();
        if ( map.getTTISeconds() > 0 || 
                map.getTTLSeconds() > 0 || 
                ELEMENT_BASED_TTI_TTL_ENABLED ||
                map.getSize() > map.getMaxTotalCount() ) {
            return super.startEviction(map);
        }
        return false;
    }
    
    @Override
    public Map collectEvictonCandidates(EvictableMap map, ClientObjectReferenceSet clients) {
        int samples = calculateSampleCount(map);
        Map s = filter(map.getRandomSamples(Math.round(samples * 1.5f), clients),map.getTTISeconds(),map.getTTLSeconds(),samples);
        sampled = s.size();
        return s;
    }
      
    protected int calculateSampleCount(EvictableMap ev) {
        int max = ev.getMaxTotalCount();
        int count = ev.getSize();

        int samples = max/10;
        if ( samples < 100 ) {
            samples = 100;
        } else if ( samples > 1000000 ) {
            samples = 1000000;
        }
        
        if ( max > 0 && count - max > 0 ) {
            samples = count - max;
            dumpLive = true;
        }
        
        return samples;
    }
    
   private Map filter(final Map samples, final int ttiSeconds,
                    final int ttlSeconds, int targetCount) {
    final HashMap candidates = new HashMap(samples.size());
    final int now = (int) (System.currentTimeMillis() / 1000);
    for (final Iterator iterator = samples.entrySet().iterator(); candidates.size() < targetCount && iterator.hasNext();) {
      final Map.Entry e = (Map.Entry) iterator.next();
        int expiresIn = expiresIn(now, e.getValue(), ttiSeconds, ttlSeconds);
        if ( expiresIn <= 0 ) {
            candidates.put(e.getKey(), e.getValue());
            expired+=1;
        } else if ( dumpLive ) {
            // Element already expired
            candidates.put(e.getKey(), e.getValue());
            overflow+=1;
        } else {
            alive+=1;
        }
    }

    return candidates;
}   
      /**
   * This method tries to compute when an entry will expire relative to "now". If the value is not an EvictableEntry or
   * if tti/ttl is 0 and the property ehcache.storageStrategy.dcv2.perElementTTITTL.enable is not set to true, then it
   * always returns 0, ie. Expire Now.
   * 
   * @return when values is going to expire relative to "now", a negative number or zero indicates the value is expired.
   */
  protected int expiresIn(final int now, final Object value, final int ttiSeconds, final int ttlSeconds) {
    if (!(value instanceof ObjectID)) {
      // When tti/ttl == 0 here, then cache is set to eternal (by default or in config explicitly), so all entries can
      // be evicted if maxInDisk size is reached.
      return Integer.MIN_VALUE;
    }
    final ObjectID oid = (ObjectID) value;
    final ManagedObject mo = this.mgr.getObjectByIDOrNull(oid);
    if (mo == null) { return 0; }
    try {
      final EvictableEntry ev = getEvictableEntryFrom(mo);
      if (ev != null) {
        int time = ev.expiresIn(now, ttiSeconds, ttlSeconds);
        return time;
      } else {
        return Integer.MIN_VALUE;
      }
    } finally {
      this.mgr.releaseReadOnly(mo);
    }
  }

  private EvictableEntry getEvictableEntryFrom(final ManagedObject mo) {
    final ManagedObjectState state = mo.getManagedObjectState();
    if (state instanceof EvictableEntry) { return (EvictableEntry) state; }
    // TODO:: Custom mode support
    return null;
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

    public int compareTo(Object o) {
      return expiresIn - ((ExpiryKey) o).expiresIn;
    }

  }

    @Override
    public String toString() {
        String flag = ( expired + overflow > 0 ) ? ", ELEMENTS_EVICTED" : "";
        String element = ( ELEMENT_BASED_TTI_TTL_ENABLED ) ? ", ELEMENT_BASED_TTI_TTL_ENABLED" : "";
        return "PeriodicEvictionTrigger{" 
                + "name=" + cacheName + " - " + getId()
                + element
                + ", over capacity=" + dumpLive 
                + ", count=" + sampled
                + ", overflow=" + overflow 
                + ", expired=" + expired 
                + ", alive=" + alive 
                + flag
                + '}';
    }
  
  
}
