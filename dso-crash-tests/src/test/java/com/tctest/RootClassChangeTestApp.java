/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class RootClassChangeTestApp extends AbstractErrorCatchingTransparentApp {
  private final String        appId;
  private final int           nodeCount;
  private final boolean       adapted;
  private final HashMap       tcHashMap;
  private final int           numOfLevels;
  private final int           numOfIterations;
  private final CyclicBarrier barrier;

  public RootClassChangeTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.appId = appId;
    nodeCount = cfg.getGlobalParticipantCount();
    adapted = Boolean.valueOf(cfg.getAttribute(appId + ApplicationConfig.ADAPTED_KEY)).booleanValue();

    tcHashMap = new HashMap();
    numOfLevels = 20;
    numOfIterations = 100;
    barrier = new CyclicBarrier(nodeCount);
  }

  // un-adapted version has a field missing that adapted version has
  public void runTest() throws Throwable {
    System.out.println("***** Test starting: appId=[" + appId + "] adapted=[" + adapted + "] nodeCount=[" + nodeCount
                       + "]");

    if (!adapted) {
      runPutTest();
      runGetTest();
    }

    System.out.println("***** Barrier 1: appId=[" + appId + "] adapted=[" + adapted + "]");

    barrier.barrier();

    if (adapted) {
      runPutTest();
      runGetTest();
    }

    System.out.println("***** Barrier 2: appId=[" + appId + "] adapted=[" + adapted + "]");

    barrier.barrier();

    if (!adapted) {
      runPutTest();
      runGetTest();
    }

    System.out.println("***** Barrier 3: appId=[" + appId + "] adapted=[" + adapted + "]");

    barrier.barrier();

    if (adapted) {
      runPutTest();
      runGetTest();
    }

    System.out.println("***** Barrier 4: appId=[" + appId + "] adapted=[" + adapted + "]");

    barrier.barrier();
  }

  private void runGetTest() {
    System.out.println("***** appId=[" + appId + "] adapted=[" + adapted + "] runGetTest");

    Set keySet;
    synchronized (tcHashMap) {
      keySet = tcHashMap.keySet();
    }

    System.out.println("***** keySetSize=[" + keySet.size() + "]");

    for (Iterator iter = keySet.iterator(); iter.hasNext();) {
      Object key = iter.next();
      DeepLargeObject dlo;
      synchronized (tcHashMap) {
        dlo = (DeepLargeObject) tcHashMap.get(key);
      }

      Object foo;
      try {
        foo = dlo.getFooObject();
      } catch (Throwable t) {
        foo = null;
      }

      System.out.println("***** tcHashMap: key=[" + key + "] numLevel=[" + dlo.getNumOfLevel() + "] childObjectLevel=["
                         + dlo.getChildObject().getNumOfLevel() + "] fooObject=[" + foo + "]");
    }
  }

  private void runPutTest() throws Exception {
    System.out.println("***** appId=[" + appId + "] adapted=[" + adapted + "] runPutTest");

    int nextPosition = tcHashMap.size();

    for (int i = 0; i < numOfIterations; i++) {
      int key = i + nextPosition;

      System.out.println("***** key=[" + key + "]");

      synchronized (tcHashMap) {
        tcHashMap.put("key" + key, new DeepLargeObject(numOfLevels));
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = RootClassChangeTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    config.addIncludePattern(testClass + "$*");

    spec.addRoot("tcHashMap", "tcHashMap");
    spec.addRoot("barrier", "barrier");

    config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");
  }

  public static class DeepLargeObject {
    private int             numOfLevel;

    private DeepLargeObject childObject;

    // Code added by adapter:
    // private FooObject foo;

    public DeepLargeObject(int numOfLevel) {
      this.numOfLevel = numOfLevel;
      if (numOfLevel > 0) {
        this.childObject = new DeepLargeObject(numOfLevel - 1);
      } else {
        this.childObject = null;
      }
      setFooObject();
    }

    private void setFooObject() {
      // Code added by adapter:
      // foo = new FooObject("Yeah", 5);
    }

    public DeepLargeObject getChildObject() {
      return childObject;
    }

    public int getNumOfLevel() {
      return numOfLevel;
    }

    public FooObject getFooObject() {
      // Code changed to following by adapter:
      // return foo;
      throw new AssertionError();
    }
  }

  public static class FooObject {
    private String name;

    private int    count;

    public FooObject(String name, int count) {
      this.name = name;
      this.count = count;
    }

    public String toString() {
      return "FooObject: name=[" + name + "] count=[" + count + "]";
    }
  }
}
