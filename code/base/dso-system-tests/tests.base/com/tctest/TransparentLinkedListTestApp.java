/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class TransparentLinkedListTestApp extends AbstractTransparentApp {

  private static final int INITIAL_STAGE               = 0;
  private static final int ADD_COMPLETE_STAGE          = 1;
  private static final int ASSERT_MAX_COUNT_SIZE_STAGE = 2;
  private static final int REMOVE_COMPLETE_STAGE       = 3;
  private static final int ASSERT_REMOVE_SIZE_STAGE    = 4;

  private LinkedList       list                        = new LinkedList();

  public TransparentLinkedListTestApp(String applicationId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(applicationId, cfg, listenerProvider);
  }

  public void run() {
    int maxCount = getParticipantCount() * getIntensity();
    List testObjects = new ArrayList();
    moveToStage(INITIAL_STAGE);
    for (int i = 0; i < getIntensity(); i++) {
      TestObject to = new TestObject(getApplicationId(), i);
      testObjects.add(to);
      synchronized (list) {
        int size = list.size();
        list.add(to);
        Assert.eval(list.size() == size + 1);
      }
    }
    moveToStageAndWait(ADD_COMPLETE_STAGE);

    checkSize(maxCount);

    moveToStageAndWait(ASSERT_MAX_COUNT_SIZE_STAGE);

    int removeCount = getIntensity() / 2;
    for (int i = 0; i < removeCount; i++) {
      synchronized (list) {
        try {
          int size = list.size();
          boolean wasRemoved = list.remove(testObjects.get(i));
          Assert.eval("Test object should have been removed  but wasn't: " + testObjects.get(i), wasRemoved);
          Assert.eval(list.size() == size - 1);
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }
    }

    moveToStageAndWait(REMOVE_COMPLETE_STAGE);

    checkSize(maxCount - getParticipantCount() * removeCount);

    moveToStageAndWait(ASSERT_REMOVE_SIZE_STAGE);

    synchronized (list) {
      list.clear();
      Assert.eval(list.size() == 0);
    }

    checkSize(0);
    notifyResult(Boolean.TRUE);
  }

  private void checkSize(int s) {
    synchronized (list) {
      if (list.size() != s) {
        System.out.println("list:" + list.size() + " expecting:" + s);
        Map res = new HashMap();
        for (Iterator i = list.iterator(); i.hasNext();) {
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
    String testClassName = TransparentLinkedListTestApp.class.getName();
    config.addIncludePattern(testClassName);
    config.addRoot(new Root(testClassName, "list", "list"), true);
    config.addWriteAutolock("* " + testClassName + ".*(..)");
    config.addIncludePattern(TestObject.class.getName());
    visitor.visit(config, AbstractTransparentApp.class);
  }

  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    String classname = TransparentLinkedListTestApp.class.getName();
    config.addIncludePattern(classname);
    config.addRoot("list", classname + ".list");
    config.addWriteAutolock("* " + classname + ".*(..)");
    config.addIncludePattern(TestObject.class.getName());
    visitor.visitDSOApplicationConfig(config, AbstractTransparentApp.class);
  }

  static class TestObject {
    private String id;
    private int    count;

    TestObject(String i, int count) {
      this.id = i;
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