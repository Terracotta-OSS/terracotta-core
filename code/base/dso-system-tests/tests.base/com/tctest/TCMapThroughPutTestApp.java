/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.exception.TCRuntimeException;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class TCMapThroughPutTestApp extends AbstractTransparentApp {
  private int                 NUM_OF_ITERATIONS = 2000;
  private final static int    NUM_OF_KEYS       = 20;
  private final static int    NUM_OF_LEVELS     = 10;
  private final static int    NUM_OF_THREADS    = 1;

  private final List          hashMapList       = new ArrayList();
  private final List          hashtableList     = new ArrayList();

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
      /*if (nodeId == 0) {
        long freeMemory = Runtime.getRuntime().freeMemory();
        System.err.println("******free mem: " + freeMemory);
        long requireMemory = 1024000000;
        NUM_OF_ITERATIONS = (int) (NUM_OF_ITERATIONS * (freeMemory * 1.0 / requireMemory));
        System.err.println("***********NUM_OF_ITERATIONS: " + NUM_OF_ITERATIONS);
      }

      barrier.barrier();*/

      synchronized (hashMapList) {
        hashMapList.add(tcHashMap);
      }
      synchronized (hashtableList) {
        hashtableList.add(tcHashtable);
      }

      barrier.barrier();

      putTest(nodeId);

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void putTest(int nodeId) throws Exception {
    if (nodeId == 0) {
      System.err.println("Put Test start ...");
    }
    barrier.barrier();

    runPutTest(nodeId, tcHashMap);

    runPutTest(nodeId, tcHashtable);

    if (nodeId == 0) {
      System.err.println("Put Test end ...");
    }
    barrier.barrier();
  }

  private void runPutTest(int nodeId, Map sharedMap) throws Exception {
    Thread[] numOfPutter = new Thread[NUM_OF_THREADS];
    CyclicBarrier localBarrier = new CyclicBarrier(NUM_OF_THREADS + 1);
    for (int i = 0; i < NUM_OF_THREADS; i++) {
      numOfPutter[i] = new Thread(new TCMapPutter(NUM_OF_ITERATIONS, NUM_OF_LEVELS, localBarrier, sharedMap));
    }
    for (int i = 0; i < NUM_OF_THREADS; i++) {
      numOfPutter[i].start();
    }
    localBarrier.barrier();

    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = TCMapThroughPutTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(TCMapPutter.class.getName());
    config.addIncludePattern(DeepLargeObject.class.getName());

    String writeAllowdMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowdMethodExpression);

    writeAllowdMethodExpression = "* " + TCMapPutter.class.getName() + "*.*(..)";
    config.addWriteAutolock(writeAllowdMethodExpression);
    writeAllowdMethodExpression = "* " + DeepLargeObject.class.getName() + "*.*(..)";
    config.addWriteAutolock(writeAllowdMethodExpression);

    spec.addRoot("hashMapList", "hashMapList");
    spec.addRoot("hashtableList", "hashtableList");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("NUM_OF_ITERATIONS", "NUM_OF_ITERATIONS");
  }

  public static class TCMapPutter implements Runnable {
    private final int           numOfObjects;
    private final int           numOfLevels;
    private final Map           sharedMap;
    private final CyclicBarrier barrier;

    public TCMapPutter(int numOfObjects, int numOfLevels, CyclicBarrier barrier, Map sharedMap) {
      this.numOfObjects = numOfObjects;
      this.numOfLevels = numOfLevels;
      this.barrier = barrier;
      this.sharedMap = sharedMap;
    }

    public void run() {
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < numOfObjects; i++) {
        synchronized (sharedMap) {
          int key = i % NUM_OF_KEYS;
          sharedMap.put("key" + key, new DeepLargeObject(numOfLevels));
        }
      }

      long endTime = System.currentTimeMillis();
      System.err.println(endTime-startTime);
      System.err.println("TPS for type " + sharedMap.getClass().getName() + ": " + (numOfObjects * 1000.0)
                         / (endTime - startTime));

      try {
        barrier.barrier();
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      }
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
