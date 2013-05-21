/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictionListener;
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

    private boolean stopped = false;
    private PeriodicEvictionTrigger current;
    
    public PeriodicCallable(ProgressiveEvictionManager evictor, Set<ObjectID> workingSet) {
      this.evictor = evictor;
      this.workingSet = workingSet;
      this.listeningSet = new ObjectIDSet(workingSet);
    }

    @Override
    public boolean cancel() {
      stop();
      evictor.removeEvictionListener(this);
      return true;
    }
    
    private synchronized void stop() {
      stopped = true;
      listeningSet.clear();
      workingSet.clear();
      if ( current != null ) {
        current.stop();
      }
    }

    private synchronized boolean isStopped() {
      return stopped;
    }
    
    private synchronized void setCurrent(PeriodicEvictionTrigger trigger) {
      current = trigger;
    }

    @Override
    public SampledRateCounter call() throws Exception {
      SampledRateCounter counter = new AggregateSampleRateCounter();
      ObjectIDSet rollover = new ObjectIDSet();
      try {
        evictor.addEvictionListener(this);
        for (final ObjectID mapID : workingSet) {
          PeriodicEvictionTrigger trigger = evictor.schedulePeriodicEviction(mapID);
          if ( trigger != null ) {
            setCurrent(trigger);
            counter.increment(trigger.getCount(),trigger.getRuntimeInMillis());
            if ( trigger.filterRatio() > .66f ) {
              rollover.add(mapID);
            }
          } else {
            synchronized (this) {
              listeningSet.remove(mapID);
            }
          }
          if ( isStopped() ) {
            return counter;
          }
        }
      } finally {
        synchronized (this) {
          workingSet.clear();
          current = null;
          if ( !stopped && listeningSet.isEmpty() && !rollover.isEmpty() ) {
            evictor.schedulePeriodicEvictionRun(rollover);
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
      if ( !stopped && current == null && !workingSet.isEmpty() ) {
          evictor.schedulePeriodicEvictionRun(workingSet);
      }
      return true;
    } else {
      return false;
    }
  }
}
