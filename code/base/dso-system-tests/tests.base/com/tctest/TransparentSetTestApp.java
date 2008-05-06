/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.simulator.app.Application;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Application for testing DSO sets.
 */
public class TransparentSetTestApp extends AbstractTransparentApp implements Application {
  private static final int INITIAL_STAGE               = 0;
  private static final int ADD_COMPLETE_STAGE          = 1;
  private static final int ASSERT_MAX_COUNT_SIZE_STAGE = 2;
  private static final int REMOVE_COMPLETE_STAGE       = 3;
  private static final int ASSERT_REMOVE_SIZE_STAGE    = 4;

  private Set              set                         = new HashSet();

  public TransparentSetTestApp(String globalId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(globalId, cfg, listenerProvider);
  }

  public void run() {
    int maxCount = getParticipantCount() * getIntensity();
    List testObjects = new ArrayList();
    moveToStage(INITIAL_STAGE);
    for (int i = 0; i < getIntensity(); i++) {
      TestObject to = new TestObject(getApplicationId(), 1);
      testObjects.add(to);
      synchronized (set) {
        int size = set.size();
        set.add(to);
        Assert.eval(set.size() == size + 1);
	if((size+1) % 50 == 0) System.out.println("XXX added " + (size+1));
      }
       ThreadUtil.reallySleep(20);
    }
    moveToStageAndWait(ADD_COMPLETE_STAGE);

    checkSetSize(maxCount);

    moveToStageAndWait(ASSERT_MAX_COUNT_SIZE_STAGE);

    int removeCount = getIntensity() / 2;
    for (int i = 0; i < removeCount; i++) {
      synchronized (set) {
        int size = set.size();
        boolean wasRemoved = set.remove(testObjects.get(i));
        Assert.eval("Test object should have been removed  but wasn't: " + testObjects.get(i), wasRemoved);
        Assert.eval(set.size() == size - 1);
	if((size-1) % 50 == 0) System.out.println("XXX remain " + (size-1));
      }
       ThreadUtil.reallySleep(20);
    }

    moveToStageAndWait(REMOVE_COMPLETE_STAGE);

    checkSetSize(maxCount - getParticipantCount() * removeCount);

    moveToStageAndWait(ASSERT_REMOVE_SIZE_STAGE);

    synchronized (set) {
      set.clear();
      Assert.eval(set.size() == 0);
    }

    checkSetSize(0);
    notifyResult(Boolean.TRUE);
  }

  private void checkSetSize(int s) {
    synchronized (set) {
      String error = "set size:" + set.size() + " expecting: " + s + " for: " + this.getApplicationId();
      System.out.println(error);
      Map res = new HashMap();
      for (Iterator i = set.iterator(); i.hasNext();) {
        TestObject to = (TestObject) i.next();
        String key = to.getId();
        if (!res.containsKey(key)) {
          res.put(key, new Long(0));
        } else {
          long v = ((Long) res.get(key)).longValue();
          res.put(key, new Long(++v));
        }
      }
      if (set.size() != s) {

      throw new AssertionError("" + res + error); }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = TransparentSetTestApp.class.getName();
    config.addRoot(new Root(testClassName, "set", "set"), true);
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
