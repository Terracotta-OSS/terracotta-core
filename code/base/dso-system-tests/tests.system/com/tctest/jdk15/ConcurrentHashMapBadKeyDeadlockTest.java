/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

/**
 * (see CDV-1160) -- This test reproduces a deadlock associated with using certain key types, such as Class, in a
 * ConcurrentHashMap. The problem happens when the key type has a hashCode() that is not stable across nodes.
 */
public class ConcurrentHashMapBadKeyDeadlockTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public ConcurrentHashMapBadKeyDeadlockTest() {
    // this.disableAllUntil("2010-03-01");
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private static final long                    DURATION      = 30L * 1000L; // test duration (30 seconds)
    private static final long                    LOG_INTERVAL  = 5L * 1000L; // write output every 5 seconds
    private static final int                     INNER_CLASSES = 20;

    /** map of Class to hashcode */
    private ConcurrentHashMap<Class<?>, Integer> mapRoot;
    private final CyclicBarrier                  barrier;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      config.addIncludePattern(ConcurrentHashMapBadKeyDeadlockTest.class.getName() + ".Inner*");

      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("mapRoot", "mapRoot");
      spec.addRoot("barrier", "barrier");

    }

    @Override
    protected void runTest() throws Throwable {

      // Fill the shared map with keys
      int index = barrier.await();
      if (index == 0) {
        mapRoot = new ConcurrentHashMap<Class<?>, Integer>();
        for (int i = 0; i < INNER_CLASSES; i++) {
          Class<?> clazz = getClassInstance(i);
          int hash = clazz.hashCode();
          // System.out.println("adding class " + clazz.getSimpleName() + " with hash " + hash);
          mapRoot.put(clazz, Integer.valueOf(hash));
        }

        // Validate the map
        Class<?>[] classes = new Class<?>[mapRoot.size()];
        for (Class clazz : mapRoot.keySet()) {
          int i = getClassIndex(clazz);
          classes[i] = clazz;
        }
        for (int i = 0; i < classes.length; ++i) {
          assertNotNull(classes[i]);
          assertNotNull(mapRoot.get(classes[i]));
        }
        assertEquals(INNER_CLASSES, mapRoot.size());
      }

      // On each thread, call size() and mutate individual entries repeatedly
      index = barrier.await();

      Random r = new Random();
      final long start = System.currentTimeMillis();

      int count = 0;
      int mismatches = 0;

      long time = 0;
      long nextLogTime = LOG_INTERVAL;
      while ((time = System.currentTimeMillis() - start) < DURATION) {
        count++;

        // UNCOMMENT THESE LINES TO CAUSE DEADLOCK:
        int size = mapRoot.size();
        assertEquals(INNER_CLASSES, size);

        int i = r.nextInt(INNER_CLASSES);
        Class<?> clazz = getClassInstance(i);
        Integer oldHash = mapRoot.get(clazz);
        if (oldHash == null) {
          System.err.println("On node " + index + " map didn't contain " + clazz.getName());
        }
        assertNotNull(oldHash);
        if (oldHash != null && oldHash.intValue() != clazz.hashCode()) {
          ++mismatches;
        }
        if (time > nextLogTime) {
          System.out.println("Iteration " + count + " on node " + index + ": found " + mismatches
                             + " mismatched hash codes (expect > 0)");
          nextLogTime += LOG_INTERVAL;
        }
        mapRoot.put(clazz, Integer.valueOf(clazz.hashCode()));
      }

      // System.out.println("on node " + index + " found " + mismatches + " mismatches");
      assertTrue(index == 0 || mismatches > 0);
    }

    /**
     * @param i a number corresponding to the class, e.g., 1 -> Inner01.
     */
    Class<?> getClassInstance(int i) {
      String name = String.format("%s$Inner%02d", ConcurrentHashMapBadKeyDeadlockTest.class.getName(), i);

      try {
        return Class.forName(name);
      } catch (ClassNotFoundException e) {
        fail("Unable to make inner class with name " + name);
        throw new IllegalStateException("not reachable");
      }
    }

    /**
     * Given a class with a simple name like "Inner02", return 2.
     */
    int getClassIndex(Class<?> clazz) {
      String name = clazz.getSimpleName();
      return Integer.parseInt(name.substring(5)); // E.g., "Inner02"
    }

  }

  public static class Inner00 {
    //
  }

  public static class Inner01 {
    //
  }

  public static class Inner02 {
    //
  }

  public static class Inner03 {
    //
  }

  public static class Inner04 {
    //
  }

  public static class Inner05 {
    //
  }

  public static class Inner06 {
    //
  }

  public static class Inner07 {
    //
  }

  public static class Inner08 {
    //
  }

  public static class Inner09 {
    //
  }

  public static class Inner10 {
    //
  }

  public static class Inner11 {
    //
  }

  public static class Inner12 {
    //
  }

  public static class Inner13 {
    //
  }

  public static class Inner14 {
    //
  }

  public static class Inner15 {
    //
  }

  public static class Inner16 {
    //
  }

  public static class Inner17 {
    //
  }

  public static class Inner18 {
    //
  }

  public static class Inner19 {
    //
  }
}
