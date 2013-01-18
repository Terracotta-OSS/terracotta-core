/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
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
    private ClientObjectReferenceSet clientSet;
    private boolean repeat = false;
    private boolean restart = false;

    public CapacityEvictionTrigger( ObjectID oid) {
        super(oid);
    }

    @Override
    public boolean startEviction(EvictableMap map) {
  //  capacity eviction ignores underlying strategy b/c map.startEviction has already been called
        if ( restart ) {
            if ( !map.startEviction() ) {
                return false;
            }
        } 
        
        repeat = false;
        restart = true;
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
    public ServerMapEvictionContext collectEvictionCandidates(final int maxParam, String className, final EvictableMap map, final ClientObjectReferenceSet clients) {
   // lets try and get smarter about this in the future but for now, just bring it back to capacity
        final int sample = boundsCheckSampleSize(size - maxParam);

        if ( maxParam == 0 ) {
            throw new AssertionError("triggers should never start evicting a pinned cache or store");
        }
        
        Map samples = ( sample > 0 ) ? map.getRandomSamples(sample, clients) : Collections.<Object,ObjectID>emptyMap();

        count = samples.size();
 // didn't get the sample count we wanted.  wait for a clientobjectidset refresh, only once and try it again
        if ( count < size - maxParam ) {
            repeat = true;
            if ( count == 0 ) {
                restart = false;
                registerForUpdates(clients);
                samples = Collections.<Object,ObjectID>emptyMap();
            }
        }
        
        return createEvictionContext(className, samples);
    } 
    
    private synchronized void registerForUpdates(ClientObjectReferenceSet clients) {
        if ( clientSet == null ) {
            clients.addReferenceSetChangeListener(this);
            clientSetCount = clients.size();
            clientSet = clients;
        }
    }
    
    private synchronized void waitForClient() {
        while ( clientSet != null ) {
            try {
                this.wait(2000);
                if ( clientSet != null ) {
                    clientSet.refreshClientObjectReferencesNow();
                }
            } catch ( InterruptedException ie ) {
                throw new AssertionError(ie);
            }
        }
    }
    
    private synchronized void clientUpdated() {
        clientSet.removeReferenceSetChangeListener(this);
        clientSet = null;
        this.notify();
    }

    @Override
    public boolean isValid() {
        if ( repeat ) {
            waitForClient();
            return true;
        }
        return super.isValid();
    }

    @Override
    public void completeEviction(EvictableMap map) {
        if ( restart ) {
            super.completeEviction(map);
        }
    }

     @Override
    public void notifyReferenceSetChanged() {
         clientUpdated();
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
                + ", repeat=" + repeat 
                + ", was above capacity=" 
                + aboveCapacity + ", client set=" 
                + clientSetCount 
                + ", parent=" + super.toString()
                + '}';
    }

}
