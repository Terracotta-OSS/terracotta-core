/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;


import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSetChangedListener;

import java.util.Collections;
import java.util.Map;

/**
 * This trigger is invoked by a server map with the size of the map goes over the max count + some overshoot count (
 * default is 15% of the max count and is set via TCProperty ehcache.storageStrategy.dcv2.eviction.overshoot ) and
 * attempts to bring the size of the cache to the max capacity
 * 
 * @author mscott
 */
public class CapacityEvictionTrigger extends AbstractEvictionTrigger implements ClientObjectReferenceSetChangedListener {

  private boolean                  aboveCapacity  = true;
  private int                      clientSetCount = 0;
  private int                      max            = 0;
  private int                      size           = 0;
  private ClientObjectReferenceSet clientSet;
  private boolean                  repeat         = false;
  private final ServerMapEvictionManager    evictor;
  private boolean                   completed = false;

  public CapacityEvictionTrigger(ServerMapEvictionManager engine, ObjectID oid) {
    super(oid);
    this.evictor = engine;
  }

  @Override
  public boolean startEviction(EvictableMap map) {
    max = map.getMaxTotalCount();
    size = map.getSize();
    if ( repeat == true ) {
      waitForCompletion();
    }
    completed = false;
    // ignore return value, capacity needs to make an independent decision on whether to run
    if (max >= 0 && size > max) {
      return super.startEviction(map);
    } 
    aboveCapacity = false;
    return false;
  }
  
  private synchronized void waitForCompletion() {
    while ( !completed ) {
      try {
        this.wait();
      } catch ( InterruptedException ie ) {
        throw new AssertionError("no interruptions");
      }
    }
  }

  @Override
  public synchronized void completeEviction(EvictableMap map) {
    if ( !repeat ) {
      super.completeEviction(map);
    }  
    completed = true;
    this.notify();
  }

  @Override
  public ServerMapEvictionContext collectEvictionCandidates(final int maxParam, String className,
                                                            final EvictableMap map,
                                                            final ClientObjectReferenceSet clients) {

    // lets try and get smarter about this in the future but for now, just bring it back to capacity
    final int sample = boundsCheckSampleSize(size - maxParam);
    Map<Object, EvictableEntry> samples = (sample > 0) ? map.getRandomSamples(sample, clients, SamplingType.FOR_EVICTION)
        : Collections.<Object, EvictableEntry>emptyMap();
    // didn't get the sample count we wanted. wait for a clientobjectidset refresh, only once and try it again
    try {
      return createEvictionContext(className, samples);
    } finally {
      int count = getCount();
      if (count == 0) {
        repeat = true;
        registerForUpdates(clients);
      } else {
        repeat = false;
      }
    }
  }

  private synchronized void registerForUpdates(ClientObjectReferenceSet clients) {
    if (clientSet != null) {
      throw new AssertionError("double register for updates");
    }
    
    clientSet = clients;
    clientSetCount = clients.size();
    clients.addReferenceSetChangeListener(this);
  }
    
  private synchronized void clientUpdated() {
    clientSet.removeReferenceSetChangeListener(this);
    clientSet = null;
    reset();
    evictor.doEvictionOn(this);
  }
  
  @Override
  public synchronized boolean isValid() {
    return repeat;
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
    return "CapacityEvictionTrigger{" + ", size=" + size + ", max=" + max + ", repeat=" + repeat
           + ", was above capacity=" + aboveCapacity + ", client set=" + clientSetCount + ", parent="
           + super.toString() + '}';
  }

}
