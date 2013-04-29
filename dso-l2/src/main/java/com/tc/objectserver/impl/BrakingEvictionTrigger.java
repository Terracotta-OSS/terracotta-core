/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import java.util.Map;

/**
 *
 * @author mscott
 */
public class BrakingEvictionTrigger extends AbstractEvictionTrigger {
    
    private final int turns ;

    public BrakingEvictionTrigger(ObjectID oid, int turns) {
        super(oid);
        this.turns = turns;
    }

    @Override
    public ServerMapEvictionContext collectEvictionCandidates(int targetMax, String className, EvictableMap map, ClientObjectReferenceSet clients) {
        int size = map.getSize();
        
        Map sampled = map.getRandomSamples(Math.round(size*turns/10000f),clients, SamplingType.FOR_EVICTION);

        return createEvictionContext(className, sampled);
    }

    
}
