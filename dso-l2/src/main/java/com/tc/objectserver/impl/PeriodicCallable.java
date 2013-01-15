/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
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

    private volatile boolean stopped = false;
    private volatile PeriodicEvictionTrigger current;
    
    public PeriodicCallable(ServerMapEvictionManager evictor, ObjectManager objectManager, ObjectIDSet workingSet) {
        this.evictor = evictor;
        this.workingSet = workingSet;
        this.objectManager = objectManager;
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
        while (!workingSet.isEmpty()) {
            ObjectIDSet rollover = new ObjectIDSet();
            for (final ObjectID mapID : workingSet) {
                if ( stopped ) {
                    return counter;
                }
                current = new PeriodicEvictionTrigger(objectManager, mapID);
                evictor.doEvictionOn(current);
                counter.increment(current.getCount(),current.getRuntimeInMillis());
                if ( current.filterRatio() > .66f ) {
                    rollover.add(mapID);
                }
            }
            workingSet.clear();
            workingSet.addAll(rollover);
        }
        return counter;
    }
}
