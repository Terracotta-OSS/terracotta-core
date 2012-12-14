/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import java.util.Map;

/**
 * This trigger is fired by the resource monitor if the monitored resource goes
 * over the critical threshold of resource (default is 90% but can be set by TC Property
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
    private int sampleCount;
    private int sizeCount;

    public EmergencyEvictionTrigger(ObjectManager mgr, ObjectID oid, int blowout) {
        super(oid);
        this.blowout = blowout;
    }

    @Override
    public boolean startEviction(EvictableMap map) {
        sizeCount = map.getSize();
        return super.startEviction(map);
    }  

    @Override
    public ServerMapEvictionContext collectEvictonCandidates(int max, String className, EvictableMap map, ClientObjectReferenceSet clients) {
        sizeCount = map.getSize();
        int get = boundsCheckSampleSize(( blowout > 10 ) ? sizeCount : sizeCount * blowout / 10);
        if ( get < 2 ) {
            get = 2;
        }
        Map sampled = map.getRandomSamples(get,clients);

        return createEvictionContext(className, sampled);
    }
    
    public int getSampleCount() {
        return sampleCount;
    }

    @Override
    public String getName() {
        return "Emergency";
    }
    
    @Override
    public String toString() {
        return "EmergencyEvictionTrigger{blowout=" + blowout + ", sample=" + sampleCount + ", size=" + sizeCount + ", parent=" + super.toString() + '}';
    }
}
