/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.GarbageCollector;
import com.tc.objectserver.core.api.GarbageCollectorEventListener;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.StoppableThread;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

/**
 * this tests the frequency of young and full gc
 */
public class GarbageCollectorThreadTest extends TestCase {

  private static final long TEST_DURATION_MILLIS   = 30000L;

  private static final long YOUNG_GC_FREQUENCY     = 2000L;

  private static final long FULL_GC_FREQUENCY      = 10000L;

  private static final long GC_FREQUENCY_TOLERANCE = 300L;

  public void testYoungGCOn() {

    ObjectManagerConfig config = new ObjectManagerConfig(FULL_GC_FREQUENCY, true, true, true, true, YOUNG_GC_FREQUENCY);
    CatchExceptionThreadGroup tg = new CatchExceptionThreadGroup("gc thread group");
    TestGarbageCollector collector = new TestGarbageCollector();

    GarbageCollectorThread garbageCollectorThread = new GarbageCollectorThread(tg, "YoungGCOnThread", collector, config);
    garbageCollectorThread.start();

    try {
      Thread.sleep(TEST_DURATION_MILLIS);
    } catch (InterruptedException e) {
      new AssertionError(e);
    }

    garbageCollectorThread.requestStop();

    for (Throwable t : tg.getExceptions()) {
      fail(t.getMessage());
    }

  }
  
  public void testYoungGCOff() {

    ObjectManagerConfig config = new ObjectManagerConfig(FULL_GC_FREQUENCY, true, true, true, false, -1);
    CatchExceptionThreadGroup tg = new CatchExceptionThreadGroup("gc thread group");
    TestGarbageCollector collector = new TestGarbageCollector();

    GarbageCollectorThread garbageCollectorThread = new GarbageCollectorThread(tg, "YoungGCOffThread", collector, config);
    garbageCollectorThread.start();

    try {
      Thread.sleep(TEST_DURATION_MILLIS);
    } catch (InterruptedException e) {
      new AssertionError(e);
    }

    garbageCollectorThread.requestStop();

    for (Throwable t : tg.getExceptions()) {
      fail(t.getMessage());
    }

  }

  private static final class CatchExceptionThreadGroup extends ThreadGroup {

    private Set<Throwable> exceptions = new HashSet<Throwable>();

    public CatchExceptionThreadGroup(String name) {
      super(name);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
      exceptions.add(e);
    }

    public Set<Throwable> getExceptions() {
      return exceptions;
    }

  }

  private static final class TestGarbageCollector implements GarbageCollector {

    private long lastYoungGC = System.currentTimeMillis();

    private long lastFullGC = System.currentTimeMillis();

    public void addListener(GarbageCollectorEventListener listener) {
      //
    }

    public void addNewReferencesTo(Set rescueIds) {
      //
    }

    public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
      //
    }

    public ObjectIDSet collect(Filter traverser, Collection roots, ObjectIDSet managedObjectIds) {
      return null;
    }

    public ObjectIDSet collect(Filter traverser, Collection roots, ObjectIDSet managedObjectIds, LifeCycleState state) {
      return null;
    }

    public boolean deleteGarbage(GCResultContext resultContext) {
      return false;
    }

    public boolean disableGC() {
      return false;
    }

    public void enableGC() {
      //    
    }

    public void gc() {

      long startTime = System.currentTimeMillis();
      System.out.println("[ " + Thread.currentThread().getName() + " ] calling full GC");
      long diff = startTime - lastFullGC;
      if (diff > FULL_GC_FREQUENCY + GC_FREQUENCY_TOLERANCE) { throw new AssertionError(
                                                                                        "young gc frequency is not correct [diff = "
                                                                                            + diff
                                                                                            + " ], [ frequency + tolerance = "
                                                                                            + FULL_GC_FREQUENCY
                                                                                            + GC_FREQUENCY_TOLERANCE
                                                                                            + " ] "); }

      lastYoungGC = startTime;
      lastFullGC = startTime;
    }

    public void gcYoung() {

      long startTime = System.currentTimeMillis();
      System.out.println("[ " + Thread.currentThread().getName() + " ] calling young GC");
      long diff = startTime - lastYoungGC;
      if (diff > YOUNG_GC_FREQUENCY + GC_FREQUENCY_TOLERANCE) { throw new AssertionError(
                                                                                         "young gc frequency is not correct [diff = "
                                                                                             + diff
                                                                                             + " ], [ frequency + tolerance = "
                                                                                             + YOUNG_GC_FREQUENCY
                                                                                             + GC_FREQUENCY_TOLERANCE
                                                                                             + " ] "); }

      lastYoungGC = startTime;

    }

    public boolean isDisabled() {
      return false;
    }

    public boolean isPaused() {
      return false;
    }

    public boolean isPausingOrPaused() {
      return false;
    }

    public boolean isStarted() {
      return true;
    }

    public void notifyGCComplete() {
      //
    }

    public void notifyNewObjectInitalized(ObjectID id) {
      //
    }

    public void notifyObjectCreated(ObjectID id) {
      //
    }

    public void notifyObjectsEvicted(Collection evicted) {
      //
    }

    public void notifyReadyToGC() {
      //
    }

    public void requestGCPause() {
      //
    }

    public void setState(StoppableThread st) {
      //
    }

    public void start() {
      //
    }

    public void stop() {
      //
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      return null;
    }

  }

}
