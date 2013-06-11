/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;

import java.util.Iterator;
import java.util.Map;

/**
 * This trigger is fired by the resource monitor if the monitored resource goes
 * over the critical threshold of resource (can be set by TC Property
 * l2.eviction.criticalThreshold).
 * 
 * The sample count is defined by the percentage of the mapSize required to achieve
 * the target critical capacity assuming all elements are the same size.  The sample taken
 * is random throughout the map regardless of elements liveliness.  This trigger will continually
 * fire until the monitored resource falls below the critical threshold.
 * 
 * @author mscott
 */
public class EmergencyEvictionTrigger extends AbstractEvictionTrigger {
    
    private final int blowout;
    private int sizeCount;

    public EmergencyEvictionTrigger(ObjectID oid, int blowout) {
        super(oid);
        this.blowout = blowout;
    }

    @Override
    public ServerMapEvictionContext collectEvictionCandidates(int max, String className, EvictableMap map, ClientObjectReferenceSet clients) {
        sizeCount = map.getSize();
        int get = boundsCheckSampleSize(( blowout > 6 ) ? sizeCount : (int)Math.round(sizeCount * Math.pow(10,blowout-6)));
        if ( get < 10 * (blowout)) {
            get = 10 * (blowout);
        }
        Map<Object, EvictableEntry>  sampled = map.getRandomSamples(get,clients, SamplingType.FOR_EVICTION);
        return createEvictionContext(className, filter(sampled, map.isEvictionEnabled(), map.getTTISeconds(), map.getTTLSeconds()));
    }
    
    private Map<Object, EvictableEntry> filter(final Map<Object, EvictableEntry> samples, boolean evictionEnabled, final int ttiSeconds,
                        final int ttlSeconds) {
 //  eviction is enabled, go ahead and evict it all
        if ( evictionEnabled ) {
            return samples;
        }
// no eviction, just expire, we only got here with eviction disabled and tti/ttl set.
        final int now = (int) (System.currentTimeMillis() / 1000);
        final Iterator<Map.Entry<Object, EvictableEntry>> iterator = samples.entrySet().iterator();
        while (iterator.hasNext()) {
            if ( iterator.next().getValue().expiresIn(now, ttiSeconds, ttlSeconds) >= 0 ) {
                iterator.remove();
            }
        }
        return samples;
    }  
  
    @Override
    public String getName() {
        return "Emergency";
    }
    
    @Override
    public String toString() {
        return "EmergencyEvictionTrigger{blowout=" + blowout + ", size=" + sizeCount + ", parent=" + super.toString() + '}';
    }
}
