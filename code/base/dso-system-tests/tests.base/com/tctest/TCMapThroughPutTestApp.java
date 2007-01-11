/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class TCMapThroughPutTestApp extends AbstractTransparentApp {
  private int                 NUM_OF_ITERATIONS = 100000;
  private final static int    NUM_OF_LEVELS     = 20;

  private final HashMap       tcHashMap         = new HashMap();
  private final Hashtable     tcHashtable       = new Hashtable();

  private final CyclicBarrier barrier;

  public TCMapThroughPutTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int nodeId = barrier.barrier();
      if (nodeId == 0) {
        long freeMemory = Runtime.getRuntime().freeMemory();
        System.err.println("******free mem: " + freeMemory);
        long requireMemory = 1024000000;
        NUM_OF_ITERATIONS = (int)(NUM_OF_ITERATIONS * (freeMemory*1.0 / requireMemory));
        System.err.println("***********NUM_OF_ITERATIONS: " + NUM_OF_ITERATIONS);
      }
      
      barrier.barrier();

      putAndGetTest(nodeId);

      putTest(nodeId);

      removeTest(nodeId);

    } catch (Throwable t) {
      notifyError(t);
    }
  }
  
  private void clear() throws Exception {
    synchronized(tcHashMap) {
      tcHashMap.clear();
    }
    synchronized(tcHashtable) {
      tcHashtable.clear();
    }
  }

  private void putAndGetTest(int nodeId) throws Exception {
    if (nodeId == 0) {
      System.err.println("Put and Get Test start ...");
      clear();
    }
    barrier.barrier();

    runPutAndGetTest(nodeId, tcHashMap);

    barrier.barrier();

    runPutAndGetTest(nodeId, tcHashtable);

    barrier.barrier();

    if (nodeId == 0) {
      System.err.println("Put and Get Test end ...");
    }
    barrier.barrier();
  }

  private void putTest(int nodeId) throws Exception {
    if (nodeId == 0) {
      System.err.println("Put Test start ...");
      clear();
    }
    barrier.barrier();

    runPutTest(nodeId, tcHashMap);

    barrier.barrier();

    runPutTest(nodeId, tcHashtable);

    barrier.barrier();

    if (nodeId == 0) {
      System.err.println("Put Test end ...");
    }
    barrier.barrier();
  }

  private void removeTest(int nodeId) throws Exception {
    if (nodeId == 0) {
      System.err.println("Remove Test start ...");
      clear();
    }
    barrier.barrier();

    runRemoveTest(nodeId, tcHashMap);

    barrier.barrier();

    runRemoveTest(nodeId, tcHashtable);

    barrier.barrier();

    if (nodeId == 0) {
      System.err.println("Remove Test end ...");
    }
    barrier.barrier();
  }

  private void runPutAndGetTest(int nodeId, Map sharedMap) throws Exception {
    if (nodeId == 0) {
      new TCMapPutter(NUM_OF_ITERATIONS, NUM_OF_LEVELS, sharedMap);
    }

    barrier.barrier();

    if (nodeId != 0) {
      new TCMapReader(NUM_OF_ITERATIONS, sharedMap);
    }
  }

  private void runPutTest(int nodeId, Map sharedMap) throws Exception {
    if (nodeId == 0) {
      new TCMapPutter(NUM_OF_ITERATIONS, NUM_OF_LEVELS, sharedMap);
    }

    barrier.barrier();

    if (nodeId != 0) {
      new TCMapPutter(NUM_OF_ITERATIONS, NUM_OF_LEVELS, sharedMap);
    }
  }

  private void runRemoveTest(int nodeId, Map sharedMap) throws Exception {
    if (nodeId == 0) {
      new TCMapPutter(NUM_OF_ITERATIONS, NUM_OF_LEVELS, sharedMap);
    }

    barrier.barrier();

    if (nodeId == 1) {
      new TCMapRemover(NUM_OF_ITERATIONS, sharedMap);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = TCMapThroughPutTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(TCMapPutter.class.getName());
    config.addIncludePattern(TCMapReader.class.getName());
    config.addIncludePattern(TCMapRemover.class.getName());
    config.addIncludePattern(DeepLargeObject.class.getName());

    String writeAllowdMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowdMethodExpression);

    writeAllowdMethodExpression = "* " + TCMapPutter.class.getName() + "*.*(..)";
    config.addWriteAutolock(writeAllowdMethodExpression);
    writeAllowdMethodExpression = "* " + TCMapReader.class.getName() + "*.*(..)";
    config.addWriteAutolock(writeAllowdMethodExpression);
    writeAllowdMethodExpression = "* " + TCMapRemover.class.getName() + "*.*(..)";
    config.addWriteAutolock(writeAllowdMethodExpression);
    writeAllowdMethodExpression = "* " + DeepLargeObject.class.getName() + "*.*(..)";
    config.addWriteAutolock(writeAllowdMethodExpression);

    spec.addRoot("tcHashMap", "tcHashMap");
    spec.addRoot("tcHashtable", "tcHashtable");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("NUM_OF_ITERATIONS", "NUM_OF_ITERATIONS");
  }

  public static class TCMapPutter {
    public TCMapPutter(int numOfObjects, int numOfLevels, Map sharedMap) {
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < numOfObjects; i++) {
        synchronized (sharedMap) {
          sharedMap.put("key" + i, new DeepLargeObject(numOfLevels));
        }
      }

      long endTime = System.currentTimeMillis();
      System.err.println("TPS for type " + sharedMap.getClass().getName() + ": " + (numOfObjects * 1000.0)
                         / (endTime - startTime));
    }

    public TCMapPutter(int numOfObjects, int numOfLevels, int nodeId, Map sharedMap) {
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < numOfObjects; i++) {
        synchronized (sharedMap) {
          sharedMap.put("key" + i, new DeepLargeObject(numOfLevels));
          System.err.println("Node: " + nodeId + " putting key " + i);
        }
      }

      long endTime = System.currentTimeMillis();
      System.err.println("TPS for type " + sharedMap.getClass().getName() + ": " + (numOfObjects * 1000.0)
                         / (endTime - startTime));
    }
  }

  public static class TCMapReader {
    public TCMapReader(int numOfObjects, Map sharedMap) {
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < numOfObjects; i++) {
        Object o = null;
        synchronized (sharedMap) {
          o = sharedMap.get("key" + i);
        }
      }

      long endTime = System.currentTimeMillis();
      System.err.println("TPS for type " + sharedMap.getClass().getName() + ": " + (numOfObjects * 1000.0)
                         / (endTime - startTime));
    }
  }

  public static class TCMapRemover {
    public TCMapRemover(int numOfObjects, Map sharedMap) {
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < numOfObjects; i++) {
        Object o = null;
        while (o == null) {
          synchronized (sharedMap) {
            o = sharedMap.remove("key" + i);
          }
        }
      }

      long endTime = System.currentTimeMillis();
      System.err.println("TPS for type " + sharedMap.getClass().getName() + ": " + (numOfObjects * 1000.0)
                         / (endTime - startTime));
    }
  }

  public static class DeepLargeObject {
    private int             numOfLevel;

    private DeepLargeObject childObject;

    public DeepLargeObject(int numOfLevel) {
      this.numOfLevel = numOfLevel;
      if (numOfLevel > 0) {
        this.childObject = new DeepLargeObject(numOfLevel - 1);
      } else {
        this.childObject = null;
      }
    }

    public DeepLargeObject getChildObject() {
      return childObject;
    }

    public int getNumOfLevel() {
      return numOfLevel;
    }
  }
}
