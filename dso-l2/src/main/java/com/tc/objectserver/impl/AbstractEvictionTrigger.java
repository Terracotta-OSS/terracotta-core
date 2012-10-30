/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.EvictionTrigger;

/**
 *
 * @author mscott
 */
public abstract class AbstractEvictionTrigger implements EvictionTrigger {
    
    private final ObjectID oid;

    public AbstractEvictionTrigger(ObjectID oid) {
        this.oid = oid;
    }

    @Override
    public ObjectID getId() {
        return oid;
    }
    
    @Override
    public boolean startEviction(EvictableMap map) {
        return map.startEviction();
    }
    
    @Override
    public void completeEviction(EvictableMap map) {
        map.evictionCompleted();
    }    
    
}
