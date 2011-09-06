/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.context.PeriodicDGCResultContext;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollectorEventListener;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.text.PrettyPrinter;
import com.tc.util.concurrent.LifeCycleState;

import java.util.Collection;

import junit.framework.TestCase;

/**
 * this tests the frequency of young and full gc
 */
public class GarbageCollectorThreadTest extends TestCase {

  private static final long TEST_DURATION_MILLIS = 30000L;
  private static final long YOUNG_GC_FREQUENCY   = 3000L;
  private static final long FULL_GC_FREQUENCY    = 10000L;

  public void testYoungGCOnNoFullGC() {

    ObjectManagerConfig config = new ObjectManagerConfig(FULL_GC_FREQUENCY, false, true, true, true,
                                                         YOUNG_GC_FREQUENCY, 1000);
    TestGarbageCollector collector = new TestGarbageCollector();

    ThreadGroup gp = new ThreadGroup("test group");
    GarbageCollectorThread garbageCollectorThread = new GarbageCollectorThread(gp, "YoungGCOnThread", collector, config);
    garbageCollectorThread.start();

    try {
      Thread.sleep(TEST_DURATION_MILLIS);
    } catch (InterruptedException e) {
      new AssertionError(e);
    }

    garbageCollectorThread.requestStop();

    assertTrue("young gc on only", collector.fullGCCount == 0);
    assertTrue("should call full young gc", collector.youngGCCount > 0);

  }

  public void testYoungGCOn() {

    ObjectManagerConfig config = new ObjectManagerConfig(FULL_GC_FREQUENCY, true, true, true, true, YOUNG_GC_FREQUENCY,
                                                         1000);
    TestGarbageCollector collector = new TestGarbageCollector();

    ThreadGroup gp = new ThreadGroup("test group");
    GarbageCollectorThread garbageCollectorThread = new GarbageCollectorThread(gp, "YoungGCOnThread", collector, config);
    garbageCollectorThread.start();

    try {
      Thread.sleep(TEST_DURATION_MILLIS);
    } catch (InterruptedException e) {
      new AssertionError(e);
    }

    garbageCollectorThread.requestStop();

    assertTrue("young and full gc on", collector.fullGCCount > 0);
    assertTrue("should call full young gc", collector.youngGCCount > 0);

  }

  public void testYoungGCOff() {

    ObjectManagerConfig config = new ObjectManagerConfig(FULL_GC_FREQUENCY, true, true, true, false, -1, 1000);
    TestGarbageCollector collector = new TestGarbageCollector();

    ThreadGroup gp = new ThreadGroup("test group");
    GarbageCollectorThread garbageCollectorThread = new GarbageCollectorThread(gp, "YoungGCOffThread", collector,
                                                                               config);
    garbageCollectorThread.start();

    try {
      Thread.sleep(TEST_DURATION_MILLIS);
    } catch (InterruptedException e) {
      new AssertionError(e);
    }

    garbageCollectorThread.requestStop();

    assertTrue("should not call young gen when young is configured to be off", collector.youngGCCount == 0);
    assertTrue("should call full gc", collector.fullGCCount > 0);

  }

  private static final class TestGarbageCollector implements GarbageCollector {

    public long youngGCCount = 0;
    public long fullGCCount  = 0;

    public void addListener(GarbageCollectorEventListener listener) {
      //
    }

    public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
      //
    }

    public boolean deleteGarbage(PeriodicDGCResultContext resultContext) {
      return false;
    }

    public void waitToDisableGC() {
      // do nothing
    }

    public boolean requestDisableGC() {
      return false;
    }

    public void enableGC() {
      //
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

    public void setPeriodicEnabled(final boolean periodEnable) {
      // do nothing
    }

    public boolean isPeriodicEnabled() {
      return false;
    }

    public void setState(LifeCycleState st) {
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

    public boolean requestGCStart() {
      return true;
    }

    public void waitToStartGC() {
      // do nothing
    }

    public void waitToStartInlineGC() {
      // do nothing
    }

    public void doGC(GCType type) {
      if (GCType.FULL_GC.equals(type)) {
        this.fullGCCount++;

      }
      if (GCType.YOUNG_GEN_GC.equals(type)) {
        this.youngGCCount++;
      }
    }

  }

}
