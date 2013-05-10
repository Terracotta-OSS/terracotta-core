/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictionListener;
import com.tc.objectserver.api.ObjectManager;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.ObjectIDSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 *
 * @author mscott
 */
public class PeriodicCallable implements Callable<SampledRateCounter>, CanCancel, EvictionListener {
    
    private final Set<ObjectID> workingSet;
    private final Set<ObjectID> listeningSet;
    private final ProgressiveEvictionManager evictor;
    private final ObjectManager objectManager;

    private boolean stopped = false;
    private PeriodicEvictionTrigger current;
    
    public PeriodicCallable(ProgressiveEvictionManager evictor, ObjectManager objectManager, Set<ObjectID> workingSet) {
      this.evictor = evictor;
      this.workingSet = workingSet;
      this.listeningSet = new ObjectIDSet(workingSet);
      this.objectManager = objectManager;
    }

    @Override
    public synchronized boolean cancel() {
      stopped = true;
      if ( current != null ) {
          current.stop();
      }
      evictor.removeEvictionListener(this);
      return true;
    }
    
    private synchronized boolean setCurrent(PeriodicEvictionTrigger trigger) {
      current = trigger;
      if ( stopped ) {
        return false;
      }
      return true;
    }

    @Override
    public SampledRateCounter call() throws Exception {
      SampledRateCounter counter = new AggregateSampleRateCounter();
      ObjectIDSet rollover = new ObjectIDSet();
      try {
        evictor.addEvictionListener(this);
        for (final ObjectID mapID : workingSet) {
          PeriodicEvictionTrigger trigger = new PeriodicEvictionTrigger(objectManager, mapID);
          if ( !setCurrent(trigger) ) {
            return counter;
          }
          evictor.doEvictionOn(trigger);
          counter.increment(trigger.getCount(),trigger.getRuntimeInMillis());
          if ( trigger.filterRatio() > .66f ) {
            rollover.add(mapID);
          }
        }
      } finally {
        synchronized (this) {
          workingSet.clear();
          current = null;
          if ( listeningSet.isEmpty() && !rollover.isEmpty() ) {
            evictor.scheduleEvictionRun(rollover);
          } else {
            workingSet.addAll(rollover);
          }
        }
      }

      return counter;
    }

  @Override
  public boolean evictionStarted(ObjectID oid) {
    return false;
  }

  @Override
  public synchronized boolean evictionCompleted(ObjectID oid) {
    listeningSet.remove(oid);
    if ( listeningSet.isEmpty() ) {
      if ( current == null && !workingSet.isEmpty() ) {
          evictor.scheduleEvictionRun(workingSet);
      }
      return true;
    } else {
      return false;
    }
  }
}
