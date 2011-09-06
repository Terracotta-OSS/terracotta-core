/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;
import com.terracottatech.config.PersistenceMode;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ArrayGCTest extends GCTestBase {

  protected Class getApplicationClass() {
    return App.class;
  }

  protected boolean useExternalProcess() {
    return true;
  }

  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    super.setupConfig(configFactory);
    configFactory.setPersistenceMode(PersistenceMode.PERMANENT_STORE);
  }

  public static class App extends AbstractErrorCatchingTransparentApp {
    private static long         DURATION     = 5 * 60 * 1000;
    private static long         END          = System.currentTimeMillis() + DURATION;

    private static final String TYPE_NEW     = "N";
    private static final String TYPE_ELEMENT = "E";
    private static final String TYPE_COPY    = "C";

    private static final int    NUM          = 500;
    private static final int    LEN          = 50;

    private final CyclicBarrier barrier;
    private final Map           root;
    private final Random        random;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);

      if (getParticipantCount() < 2) { throw new AssertionError(); }

      barrier = new CyclicBarrier(getParticipantCount());
      root = new HashMap();

      long seed = new Random().nextLong();
      System.err.println("seed for " + getApplicationId() + " is " + seed);
      random = new Random(seed);
    }

    protected void runTest() throws Throwable {
      final int index = barrier.barrier();

      if (index == 0) {
        populate();
      }

      barrier.barrier();

      if ((index % 2) == 0) {
        read();
      } else {
        createAndMutate();
      }
    }

    private void populate() {
      for (int i = 0, n = NUM; i < n; i++) {
        synchronized (root) {
          root.put(TYPE_NEW + i, newArray());
          root.put(TYPE_ELEMENT + i, newArray());
          root.put(TYPE_COPY + i, newArray());
        }
      }
    }

    private Object[] newArray() {
      return newArray(random.nextInt(LEN));
    }

    private Object[] newArray(int length) {
      Object[] rv = new Object[length];
      for (int i = 0, n = rv.length; i < n; i++) {
        rv[i] = new Object();
      }
      return rv;
    }

    private void read() {
      final int h = hashCode();

      while (!shouldEnd()) {
        Object[] array = getRandomArray();

        for (int i = 0, n = array.length; i < n; i++) {
          synchronized (array) {
            Object o = array[i]; // resolve array element

            // this bit of code should prevent any optimizer from removing the array access above
            if (o.hashCode() == h) {
              System.err.println("OK");
            }
          }
        }
      }
    }

    private Object[] getRandomArray() {
      return getArray(getRandomType(), getRandomNum());
    }

    private int getRandomNum() {
      return random.nextInt(NUM);
    }

    private String getRandomType() {
      switch (random.nextInt(3)) {
        case 0: {
          return TYPE_COPY;
        }
        case 1: {
          return TYPE_ELEMENT;
        }
        case 2: {
          return TYPE_NEW;
        }
      }

      throw new AssertionError();
    }

    private Object[] getArray(String type, int num) {
      synchronized (root) {
        return (Object[]) root.get(type + num);
      }
    }

    public void createAndMutate() {
      while (!shouldEnd()) {
        String type = getRandomType();
        int num = getRandomNum();

        if (TYPE_COPY.equals(type)) {
          Object[] array = getArray(type, num);
          Object[] newArray = newArray(array.length);
          synchronized (array) {
            System.arraycopy(newArray, 0, array, 0, array.length);
          }
        } else if (TYPE_ELEMENT.equals(type)) {
          Object[] array = getArray(type, num);
          synchronized (array) {
            for (int i = 0, n = array.length; i < n; i++) {
              array[i] = new Object();
            }
          }
        } else if (TYPE_NEW.equals(type)) {
          synchronized (root) {
            root.put(type + num, newArray());
          }
        } else {
          throw new AssertionError(type);
        }
      }
    }

    private static boolean shouldEnd() {
      // slow down for the monkeys
      ThreadUtil.reallySleep(10);

      return System.currentTimeMillis() > END;
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      new CyclicBarrierSpec().visit(visitor, config);

      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("root", "root");
      spec.addRoot("barrier", "barrier");

      config.addReadAutolock("* " + testClassName + ".getArray(..)");
      config.addReadAutolock("* " + testClassName + ".read()");

      config.addWriteAutolock("* " + testClassName + ".populate()");
      config.addWriteAutolock("* " + testClassName + ".createAndMutate()");

    }

  }

}
