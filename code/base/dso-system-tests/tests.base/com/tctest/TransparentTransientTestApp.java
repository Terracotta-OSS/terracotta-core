/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Map;

public class TransparentTransientTestApp extends AbstractTransparentApp {
  private TestClass1 one         = new TestClass1();
  private TestClass2 two         = new TestClass2(new Object());
  private TestClass3 three       = new TestClass3();
  private TestClass4 four        = new TestClass4();
  private Map        sharedState = new HashMap();

  public TransparentTransientTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    if (one.getMap() == null) {
      notifyError("Test class one should never have a null");
    }
    synchronized (sharedState) {
      if (!sharedState.containsKey("two")) {
        sharedState.put("two", new Integer(0));
      }
      if (two.getMap() != null) {
        int i = ((Integer) sharedState.get("two")).intValue();
        ++i;
        System.out.println("PUTTING:" + i);
        sharedState.put("two", new Integer(i));
      }
      int i = ((Integer) sharedState.get("two")).intValue();
      System.out.println("GOT:" + i);
      if (i > 1) {
        notifyError("Illegal value for two:" + i);
      }
    }

    if (four.getMap() == null) {
      notifyError("Test class one should never have a null");
    }

    synchronized (sharedState) {
      if (!sharedState.containsKey("three")) {
        sharedState.put("three", new Integer(0));
      }
      if (three.getMap() != null) {
        int i = ((Integer) sharedState.get("three")).intValue();
        ++i;
        System.out.println("PUTTING:" + i);
        sharedState.put("three", new Integer(i));
      }
      int i = ((Integer) sharedState.get("three")).intValue();
      System.out.println("GOT:" + i);
      if (i > 1) {
        notifyError("Illegal value for three:" + i);
      }
    }

  }

  public static class TestClass1 {
    private transient Map m = new HashMap();

    public synchronized Map getMap() {
      return m;
    }
  }

  public class TestClass2 {
    private transient Map m = new HashMap();

    public TestClass2() {
      notifyError("This method should never be called");
    }

    public TestClass2(Object r) {
      //
    }

    public synchronized Map getMap() {
      return m;
    }
  }

  public static class TestClass3 {
    private Map m = new HashMap();

    public synchronized Map getMap() {
      return m;
    }
  }

  public static class TestClass4 {
    private transient Map m = new HashMap();

    public synchronized Map getMap() {
      return m;
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    try {
      String testClassName = TransparentTransientTestApp.class.getName();
      config.addRoot(new Root(testClassName, "one", "one"), true);
      config.addRoot(new Root(testClassName, "two", "two"), true);
      config.addRoot(new Root(testClassName, "three", "three"), true);
      config.addRoot(new Root(testClassName, "four", "four"), true);
      config.addRoot(new Root(testClassName, "sharedState", "sharedState"), true);
      config.addIncludePattern(TestClass1.class.getName(), false);
      config.addIncludePattern(TestClass2.class.getName(), true);
      config.addIncludePattern(TestClass3.class.getName(), true);
      config.addTransient(TestClass3.class.getName(), "m");
      config.addIncludePattern(TestClass4.class.getName(), false);

      String methodExpression = "* " + testClassName + "*.*(..)";
      System.err.println("Adding autolock for: " + methodExpression);
      config.addWriteAutolock(methodExpression);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

}