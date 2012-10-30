/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import java.util.Map;

/**
 *
 * @author mscott
 */
public interface EvictionTrigger {
    
    ObjectID  getId();
    boolean   startEviction(EvictableMap map);
    void      completeEviction(EvictableMap map);
    Map       collectEvictonCandidates(EvictableMap map, ClientObjectReferenceSet clients);

}
