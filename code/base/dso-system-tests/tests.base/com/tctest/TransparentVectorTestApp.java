/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 *
 */
public class TransparentVectorTestApp extends AbstractErrorCatchingTransparentApp {

  public TransparentVectorTestApp(String globalId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(globalId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  private static final int    INITIAL_STAGE               = 0;
  private static final int    ADD_COMPLETE_STAGE          = 1;
  private static final int    ASSERT_MAX_COUNT_SIZE_STAGE = 2;
  private static final int    REMOVE_COMPLETE_STAGE       = 3;
  private static final int    ASSERT_REMOVE_SIZE_STAGE    = 4;

  private final Vector        vector                      = new Vector();
  // private List subList;

  private final CyclicBarrier barrier;

  public void moveToStageAndWait(int stage) {
    super.moveToStageAndWait(stage);
  }

  protected void runTest() throws Throwable {
    int maxCount = getParticipantCount() * getIntensity();
    List testObjects = new Vector();
    moveToStage(INITIAL_STAGE);
    for (int i = 0; i < getIntensity(); i++) {
      TestObject to = new TestObject(getApplicationId(), i);
      testObjects.add(to);
      synchronized (vector) {
        int size = vector.size();
        vector.add(to);
        Assert.eval(vector.size() == size + 1);
      }
    }
    moveToStageAndWait(ADD_COMPLETE_STAGE);

    checkSize(maxCount);

    moveToStageAndWait(ASSERT_MAX_COUNT_SIZE_STAGE);
    int removeCount = getIntensity() / 2;
    for (int i = 0; i < removeCount; i++) {
      synchronized (vector) {
        int size = vector.size();
        Object toRemove = testObjects.get(i);
        boolean wasRemoved = vector.remove(toRemove);
        if (!wasRemoved) {
          System.out.println("List:" + vector + " list hash:" + System.identityHashCode(vector));
          Assert.eval("Test object should have been removed  but wasn't: " + testObjects.get(i), wasRemoved);
          Assert.eval(vector.size() == size - 1);
        }
      }
    }

    moveToStageAndWait(REMOVE_COMPLETE_STAGE);

    checkSize(maxCount - getParticipantCount() * removeCount);

    moveToStageAndWait(ASSERT_REMOVE_SIZE_STAGE);

    synchronized (vector) {
      vector.clear();
      Assert.eval(vector.size() == 0);
    }

    checkSize(0);

    int num = barrier.barrier();

    if (num == 0) {
      synchronized (vector) {
        vector.setSize(5);
      }
    }

    barrier.barrier();

    checkSize(5);

    for (int i = 0; i < 5; i++) {
      Object val = vector.get(i);
      if (val != null) {
        notifyError("Expected null at index " + i + ", but found " + val);
        return;
      }
    }

    barrier.barrier();

    // subList = vector.subList(1, 3);
    //
    // Assert.assertNotNull(subList);
    // Assert.assertEquals(2, subList.size());

    notifyResult(Boolean.TRUE);
  }

  private void checkSize(int s) {
    synchronized (vector) {
      if (vector.size() != s) {
        Map res = new HashMap();
        for (Iterator i = vector.iterator(); i.hasNext();) {
          TestObject to = (TestObject) i.next();
          String key = to.getId();
          if (!res.containsKey(key)) {
            res.put(key, new Long(0));
          } else {
            long v = ((Long) res.get(key)).longValue();
            res.put(key, new Long(++v));
          }
        }
        throw new AssertionError("" + res);
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    new CyclicBarrierSpec().visit(visitor, config);
    String testClassName = TransparentVectorTestApp.class.getName();
    config.addRoot(new Root(testClassName, "vector", "vector"), true);
    config.addRoot(new Root(testClassName, "subList", "subList"), true);
    config.addRoot(new Root(testClassName, "barrier", "barrier"), true);
    String methodExpression = "* " + testClassName + ".*(..)";
    System.err.println("Adding autolock for: " + methodExpression);
    config.addWriteAutolock(methodExpression);
    config.addIncludePattern(TestObject.class.getName());
  }

  static class TestObject {
    private String id;
    private int    count;

    TestObject(String id, int count) {
      this.id = id;
      this.count = count;
    }

    public String getId() {
      return id;
    }

    public String toString() {
      return "TestObject(" + id + "," + count + ")";
    }
  }

}