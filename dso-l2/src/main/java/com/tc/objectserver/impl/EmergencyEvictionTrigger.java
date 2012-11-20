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
    
    private final boolean blowout;
    private int sampleCount;
    private int sizeCount;

    public EmergencyEvictionTrigger(ObjectManager mgr, ObjectID oid, boolean blowout) {
        super(oid);
        this.blowout = blowout;
    }

    @Override
    public boolean startEviction(EvictableMap map) {
        return super.startEviction(map);
    }  

    @Override
    public Map collectEvictonCandidates(int max, EvictableMap map, ClientObjectReferenceSet clients) {
        sizeCount = map.getSize();
        int get = boundsCheckSampleSize(( blowout ) ? sizeCount : sizeCount / 2);
        Map sampled = map.getRandomSamples(get,clients);
//        Map sampled = map.getRandomSamples(sizeCount/5,new ClientObjectReferenceSet(new ClientStateManager() {
//
//            @Override
//            public boolean startupNode(NodeID nodeID) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            @Override
//            public void shutdownNode(NodeID deadNode) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            @Override
//            public void addReference(NodeID nodeID, ObjectID objectID) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            @Override
//            public void removeReferences(NodeID nodeID, Set<ObjectID> removed, Set<ObjectID> requested) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            @Override
//            public boolean hasReference(NodeID nodeID, ObjectID objectID) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            @Override
//            public List<DNA> createPrunedChangesAndAddObjectIDTo(Collection<DNA> changes, ApplyTransactionInfo references, NodeID clientID, Set<ObjectID> objectIDs, Invalidations invalidationsForClient) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            @Override
//            public Set<ObjectID> addAllReferencedIdsTo(Set<ObjectID> rescueIds) {
//                return rescueIds;
//            }
//
//            @Override
//            public void removeReferencedFrom(NodeID nodeID, Set<ObjectID> secondPass) {
//            }
//
//            @Override
//            public Set<ObjectID> addReferences(NodeID nodeID, Set<ObjectID> oids) {
//                return oids;
//            }
//
//            @Override
//            public int getReferenceCount(NodeID nodeID) {
//                return 0;
//            }
//
//            @Override
//            public Set<NodeID> getConnectedClientIDs() {
//                return Collections.<NodeID>emptySet();
//            }
//
//            @Override
//            public void registerObjectReferenceAddListener(ObjectReferenceAddListener listener) {
//
//            }
//
//            @Override
//            public void unregisterObjectReferenceAddListener(ObjectReferenceAddListener listener) {
//
//            }
//        }));
        sampleCount = sampled.size();
        return processSample(sampled);
    }
    
    public int getSampleCount() {
        return sampleCount;
    }

    @Override
    public String toString() {
        return "EmergencyEvictionTrigger{blowout=" + blowout + ", sample=" + sampleCount + ", size=" + sizeCount + ", parent=" + super.toString() + '}';
    }
}
