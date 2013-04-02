/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;

/**
 * EvictionTriggers signal that an eviction operation needs to occur on a given
 * map.
 * 
 * @author mscott
 */
public interface EvictionTrigger {
    /**
     * 
     * @return the objectid of the target map
     */
    ObjectID  getId();
    /**
     * change or confirm state on the target map and perform any other preprocessing at the
     * start of eviction
     * 
     * @param map the target map for evction
     * @return <code>true</code> if eviction should be started and the map has the proper state
     *         <code>false</code> abort eviction and exit.  map state should not be changed
     */
    boolean   startEviction(EvictableMap map);
    /**
     * return state on the map to non-evicting and perform any cleanup at the end of eviction
     * 
     * @param map
     */
    void      completeEviction(EvictableMap map);
    /**
     * Produce the map of evictable items contained in the map
     * 
     * @param targetMax max on the map segment
     * @param map       target map
     * @param clients   the client object id reference set
     * @return          a map of evictable items
     */
    ServerMapEvictionContext collectEvictionCandidates(int targetMax, String className, EvictableMap map, ClientObjectReferenceSet clients);
        
    long getRuntimeInMillis();
    
    int getCount();

    String getName();
    
    boolean isValid();    
}
