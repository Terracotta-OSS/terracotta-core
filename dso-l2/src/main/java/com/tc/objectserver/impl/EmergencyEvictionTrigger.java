/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import java.util.Map;

/**
 *
 * @author mscott
 */
public class EmergencyEvictionTrigger extends PeriodicEvictionTrigger {
    
    private final boolean blowout;
    private int sampleCount;

    public EmergencyEvictionTrigger(ObjectManager mgr, ObjectID oid, boolean blowout) {
        super(mgr, oid, true);
        this.blowout = blowout;
    }

    @Override
    protected long calculateSampleCount(long max, EvictableMap ev) {
        return super.calculateSampleCount(max, ev) * 5;
    }

    @Override
    public boolean startEviction(EvictableMap map) {
        return super.startEviction(map);
    }

    @Override
    public Map collectEvictonCandidates(int max, EvictableMap map, ClientObjectReferenceSet clients) {
        Map sampled = super.collectEvictonCandidates( max, map, clients);
        sampleCount = sampled.size();
        return sampled;
    }

    @Override
    protected int expiresIn(int now, Object value, int ttiSeconds, int ttlSeconds) {
        if ( blowout ) {
            return 0;
        } else {
            return super.expiresIn(now, value, ttiSeconds, ttlSeconds);
        }
    }
    
    public int getSampleCount() {
        return sampleCount;
    }

    @Override
    public String toString() {
        return "EmergencyEvictionTrigger{" + "blowout=" + blowout + ", sampleCount=" + sampleCount + '}';
    }
}
