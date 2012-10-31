/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSetChangedListener;
import java.util.Collections;
import java.util.Map;

/**
 *
 * @author mscott
 */
public class CapacityEvictionTrigger extends AbstractEvictionTrigger implements ClientObjectReferenceSetChangedListener {
    
    private boolean aboveCapacity = true;
    private int count = 0;
    private int clientSet = 0;
    private final ServerMapEvictionManager mgr;

    public CapacityEvictionTrigger(ServerMapEvictionManager mgr, ObjectID oid) {
        super(oid);
        this.mgr = mgr;
    }

    @Override
    public boolean startEviction(EvictableMap map) {
  //  capacity eviction ignores underlying strategy b/c map.startEviction has already been called
        if ( !map.isEvicting() ) {
            throw new AssertionError("map is not in evicting state");
        }
        int max = map.getMaxTotalCount();
        
        if ( max != 0 && map.getSize() > max ) {
            return true;
        } else {
            map.evictionCompleted();
        }
        
        aboveCapacity = false;
        return false;
    }

    @Override
    public Map collectEvictonCandidates(final EvictableMap map, final ClientObjectReferenceSet clients) {
   // lets try and get smarter about this in the future but for now, just bring it back to capacity
        final int max = map.getMaxTotalCount();
        final int size = map.getSize();
        final int sample = size - max;
        if ( max == 0 || sample <= 0 ) {
            return Collections.emptyMap();
        }
        Map samples = map.getRandomSamples(sample, clients);
        count = samples.size();
 // didn't get the sample count we wanted.  wait for a clientobjectidset refresh, only once and try it again
        if ( count < sample ) {
            clients.addReferenceSetChangeListener(this);
            clientSet = clients.size();
        }
        return samples;
    } 
    
     @Override
    public void notifyReferenceSetChanged() {
       mgr.doEvictionOn(new AbstractEvictionTrigger(getId()) {
            private int sampleCount = 0;
            private boolean wasOver = true;
            private int clientSet = 0;

            @Override
            public Map collectEvictonCandidates(EvictableMap map, ClientObjectReferenceSet clients) {
                if ( map.getSize() <= map.getMaxTotalCount() ) {
                    wasOver = false;
                    clients.removeReferenceSetChangeListener(CapacityEvictionTrigger.this);
                    return Collections.emptyMap();
                }
                final int grab = map.getSize() - map.getMaxTotalCount();
                Map sample = map.getRandomSamples(grab, clients);
                sampleCount = sample.size();
                clientSet = clients.size();
                if ( sampleCount == grab) {
                    clients.removeReferenceSetChangeListener(CapacityEvictionTrigger.this);
                }
                return sample;
            }

            @Override
            public String toString() {
                return "ClientReferenceSetRefreshCapacityEvictor{wasover="  + wasOver 
                        + " count=" + sampleCount
                        + " clientset=" + clientSet + "}";
            }
            
            

        });
    }
                 
    @Override
    public String toString() {
        return "CapacityEvictionTrigger{count=" 
                + count + ", was above capacity=" 
                + aboveCapacity + ", client set=" 
                + clientSet + '}';
    }

}
