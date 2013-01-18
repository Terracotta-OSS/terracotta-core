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
    private int sampleCount;
    private int sizeCount;

  // private final ObjectManager mgr;

    public EmergencyEvictionTrigger(ObjectManager mgr, ObjectID oid, int blowout) {
        super(oid);
        this.blowout = blowout;
    // this.mgr = mgr;
    }

    @Override
    public ServerMapEvictionContext collectEvictionCandidates(int max, String className, EvictableMap map, ClientObjectReferenceSet clients) {
        sizeCount = map.getSize();
        int get = boundsCheckSampleSize(( blowout > 6 ) ? sizeCount : (int)Math.round(sizeCount * Math.pow(10,blowout-6)));
        if ( get < 10 * (blowout)) {
            get = 10 * (blowout);
        }
        Map<Object, ObjectID>  sampled = map.getRandomSamples(get,clients);
        return createEvictionContext(className, filter(sampled));
    }
    /**
     * only take the early half, make this pluggable?
    */
    private Map<Object, ObjectID> filter(Map<Object, ObjectID> sample) {
        return sample;
//        return filterByOid(sample);
    }

  // private Map<Object, ObjectID> filterByOid(Map<Object, ObjectID> sample) {
  // TreeMap<ObjectID,Object> rev = new TreeMap<ObjectID, Object>();
  // for ( Map.Entry<Object,ObjectID> entry : sample.entrySet() ) {
  // rev.put(entry.getValue(),entry.getKey());
  // }
  // int nz = sample.size()/2;
  // int count = 0;
  // sample.clear();
  // for (Entry<ObjectID, Object> ag : rev.entrySet()) {
  // if (count++>nz) {
  // sample.put(ag.getValue(),ag.getKey());
  // }
  // }
  // return sample;
  // }

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
