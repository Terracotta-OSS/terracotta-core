/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounterImpl;
import com.tc.util.ObjectIDSet;
import java.util.concurrent.Callable;

/**
 *
 * @author mscott
 */
public class PeriodicCallable implements Callable<SampledRateCounter>, CanCancel {
    
    private final ObjectIDSet workingSet;
    private final ServerMapEvictionManager evictor;
    private final ObjectManager objectManager;
    private final boolean  elementBasedTTIorTTL;

    private volatile boolean stopped = false;
    private volatile PeriodicEvictionTrigger current;
    
    public PeriodicCallable(ServerMapEvictionManager evictor, ObjectManager objectManager, ObjectIDSet workingSet,  boolean elementBasedTTIorTTL) {
        this.evictor = evictor;
        this.workingSet = workingSet;
        this.objectManager = objectManager;
        this.elementBasedTTIorTTL = elementBasedTTIorTTL;
    }

    @Override
    public boolean cancel() {
        stopped = true;
        if ( current != null ) {
            current.stop();
        }
        return true;
    }

    @Override
    public SampledRateCounter call() throws Exception {
        SampledRateCounter counter = new AggregateSampleRateCounter();
        for (final ObjectID mapID : workingSet) {
            if ( stopped ) {
                return null;
            }
            current = new PeriodicEvictionTrigger(objectManager, mapID,elementBasedTTIorTTL);
            evictor.doEvictionOn(current);
            counter.increment(current.getCount(),current.getRuntimeInMillis());
        }
        return counter;
    }    
}
