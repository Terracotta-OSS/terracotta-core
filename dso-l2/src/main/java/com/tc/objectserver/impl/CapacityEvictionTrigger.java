/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSetChangedListener;
import java.util.Collections;
import java.util.Map;

/**
 * This trigger is invoked by a server map with the size of the map goes over 
 * the max count + some overshoot count ( default is 15% of the max count and is 
 * set via TCProperty ehcache.storageStrategy.dcv2.eviction.overshoot ) and attempts
 * to bring the size of the cache to the max capacity
 * 
 * @author mscott
 */
public class CapacityEvictionTrigger extends AbstractEvictionTrigger implements ClientObjectReferenceSetChangedListener {
    
    private boolean aboveCapacity = true;
    private int count = 0;
    private int clientSetCount = 0;
    private int max = 0;
    private int size = 0;
    private final ServerMapEvictionManager mgr;
    private ClientObjectReferenceSet clientSet;

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
        
        max = map.getMaxTotalCount();
        size = map.getSize();
        if ( max > 0 && size > max ) {
            if ( !map.isEvicting() ) {
                throw new AssertionError("map is not in evicting state");
            }
            super.startEviction(map);
            return true;
        } else {
            map.evictionCompleted();
        }
        
        aboveCapacity = false;
        return false;
    }
            
    @Override
    public ServerMapEvictionContext collectEvictonCandidates(final int maxParam, String className, final EvictableMap map, final ClientObjectReferenceSet clients) {
   // lets try and get smarter about this in the future but for now, just bring it back to capacity
        final int sample = boundsCheckSampleSize(size - maxParam);

        if ( maxParam == 0 ) {
            throw new AssertionError("triggers should never start evicting a pinned cache or store");
        }
        if ( sample <= 0 ) {
            processSample(Collections.<Object,ObjectID>emptyMap());
            return null;
        }
        Map samples = map.getRandomSamples(sample, clients);
        count = samples.size();
 // didn't get the sample count we wanted.  wait for a clientobjectidset refresh, only once and try it again
        if ( count < size - maxParam ) {
            clients.addReferenceSetChangeListener(this);
            clientSetCount = clients.size();
            clientSet = clients;
        }
        return new ServerMapEvictionContext(this, processSample(samples) , className, map.getCacheName());
    } 
    
     @Override
    public void notifyReferenceSetChanged() {
       mgr.doEvictionOn(new AbstractEvictionTrigger(getId()) {
            private int sampleCount = 0;
            private int sizeInternal = 0;
            private int maxInternal = 0;
            private boolean wasOver = true;
            private int clientSetCountInternal = 0;

            @Override
            public boolean startEviction(EvictableMap map) {
                sizeInternal = map.getSize();
                maxInternal = map.getMaxTotalCount();
                if ( sizeInternal <= maxInternal ) {
                    wasOver = false;
                    clientSet.removeReferenceSetChangeListener(CapacityEvictionTrigger.this);
                    return false;
                } else {
                    return super.startEviction(map);
                }
            }
            
            @Override
            public ServerMapEvictionContext collectEvictonCandidates(int maxParam, String className, EvictableMap map, ClientObjectReferenceSet clients) {
                final int grab = boundsCheckSampleSize(sizeInternal - maxParam);
                Map sample;
                if ( grab > 0 ) {
                    sample = map.getRandomSamples(grab, clients);
                    clientSetCountInternal = clients.size();
                } else {
                    sample = Collections.emptyMap();
                }
                sampleCount = sample.size();
                if ( sampleCount >= sizeInternal - maxParam ) {
                    clients.removeReferenceSetChangeListener(CapacityEvictionTrigger.this);
                }
                if ( sample.isEmpty() ) {
                    return null;
                } else {
                    return new ServerMapEvictionContext(this, processSample(sample) , className, map.getCacheName());
                }
            }

            @Override
            public String getName() {
                return "CapacityOnClientUpdate";
            }
        
            @Override
            public String toString() {
                return "ClientReferenceSetRefreshCapacityEvictor{wasover="  + wasOver 
                        + ", count=" + sampleCount
                        + ", size=" + sizeInternal 
                        + ", max=" + maxInternal 
                        + ", clientset=" + clientSetCountInternal
                        + ", parent=" + super.toString() 
                        + "}";
            }
        });
    }

    @Override
    public String getName() {
        return "Capacity";
    }

    @Override
    public String toString() {
        return "CapacityEvictionTrigger{"
                + "count=" + count 
                + ", size=" + size 
                + ", max=" + max 
                + ", was above capacity=" 
                + aboveCapacity + ", client set=" 
                + clientSetCount 
                + ", parent=" + super.toString()
                + '}';
    }

}
