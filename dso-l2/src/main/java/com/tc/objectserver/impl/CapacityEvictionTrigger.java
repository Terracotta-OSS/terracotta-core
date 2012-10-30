/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import java.util.Map;

/**
 *
 * @author mscott
 */
public class CapacityEvictionTrigger extends AbstractEvictionTrigger {
    
    private boolean aboveCapacity = true;
    private int count = 0;

    public CapacityEvictionTrigger(ObjectID oid) {
        super(oid);
    }

    @Override
    public boolean startEviction(EvictableMap map) {
  //  capacity eviction ignores underlying strategy b/c map.startEviction has already been called
        if ( !map.isEvicting() ) {
            throw new AssertionError("map is not in evicting state");
        }
        if ( map.getSize() > map.getMaxTotalCount() ) {
            return true;
        } else {
            map.evictionCompleted();
        }
        aboveCapacity = false;
        return false;
    }

    @Override
    public Map collectEvictonCandidates(EvictableMap map, ClientObjectReferenceSet clients) {
   // lets try and get smarter about this in the future but for now, just bring it back to capacity
        Map samples = map.getRandomSamples(map.getSize() - map.getMaxTotalCount(), clients);
        count = samples.size();
        return samples;
    }

    @Override
    public String toString() {
        return "CapacityEvictionTrigger{count=" + count + ", was above capacity=" + aboveCapacity + '}';
    }

}
