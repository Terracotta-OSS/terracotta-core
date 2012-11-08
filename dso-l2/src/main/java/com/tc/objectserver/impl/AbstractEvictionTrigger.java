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
    boolean started = false;

    public AbstractEvictionTrigger(ObjectID oid) {
        this.oid = oid;
    }

    @Override
    public ObjectID getId() {
        return oid;
    }
    
    @Override
    public boolean startEviction(EvictableMap map) {
        started = map.startEviction();
        return started;
    }
    
    @Override
    public void completeEviction(EvictableMap map) {
        if ( started ) {
            map.evictionCompleted();
        }
    }    

    @Override
    public String toString() {
        return "AbstractEvictionTrigger{" + "oid=" + oid + '}';
    }
}
