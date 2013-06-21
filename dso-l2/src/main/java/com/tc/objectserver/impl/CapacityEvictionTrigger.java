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
  private boolean                  valid         = true;
  private final ServerMapEvictionManager    evictor;
  private boolean                   completed = true;
  private int                      count = 0;
  private final java.util.UUID           id = java.util.UUID.randomUUID();

  public CapacityEvictionTrigger(ServerMapEvictionManager engine, ObjectID oid) {
    super(oid);
    this.evictor = engine;
  }

  @Override
  public boolean startEviction(EvictableMap map) {
    start();

    try {
      if ( !valid ) {
  // job has already been performed on previous iteration
        return false;
      }
    } finally {
      reset();
    }
    
    max = map.getMaxTotalCount();
    size = map.getSize();
    count += 1;
    // ignore return value, capacity needs to make an independent decision on whether to run
    if (max >= 0 && size > max) {
      return super.startEviction(map);
    }
    aboveCapacity = false;
    return false;
  }
  
  private synchronized void start() {
    while ( !completed ) {
      try {
        this.wait();
      } catch ( InterruptedException ie ) {
        throw new AssertionError("no interruptions");
      }
    }
    completed = false;
  }

  @Override
  public synchronized void completeEviction(EvictableMap map) {
    if ( !valid ) {
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
      if (getCount() == 0) {
        valid = true;
        registerForUpdates(clients);
      } else {
        valid = false;
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
    if ( clientSet == null ) {
      return;
    }
    clientSet.removeReferenceSetChangeListener(this);
    clientSet = null;
    if ( !valid || getCount() > 0 ) {
      throw new AssertionError("capacity trigger in illegal state");
    }
    evictor.doEvictionOn(this);
  }
  
  @Override
  public synchronized boolean isValid() {
    return valid;
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
    return "CapacityEvictionTrigger{id=" + id + ", count=" + count +", size=" + size + ", max=" + max + ", valid=" + valid
           + ", was above capacity=" + aboveCapacity + ", client set=" + clientSetCount + ", parent="
           + super.toString() + '}';
  }

}
