/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Hashtable;

/*
 * Assert some things about Hashtable instances that are not shared
 *
 * This test was first written in response to DEV-958 -- it should evolve
 */
public class UnsharedHashtableTest extends TransparentTestBase {
  private final static int NODE_COUNT = 1;

  public void setUp() throws Exception {
    super.setUp();

    getTransparentAppConfig().setClientCount(NODE_COUNT);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    protected void runTest() throws Throwable {
      final Hashtable ht = new Hashtable();
      final int size = 500;

      for (int i = 0; i < size; i++) {
        Integer I = new Integer(i);
        ht.put(I, I);
      }

      final Stop stop = new Stop();

      Thread mutator = new Thread() {
        public void run() {
          Integer I = new Integer(ht.size());
          while (!stop.stop) {
            ht.remove(I);
            ht.put(I, I);
          }
        }
      };
      mutator.start();

      final long end = System.currentTimeMillis() + 60000;

      try {
        while (System.currentTimeMillis() < end) {
          Object[] array = ht.values().toArray();

          // this code here to make sure the call to toArray() above can never be optimized out
          if (array.hashCode() == end) {
            System.err.println("some useless printing");
          }
        }
      } finally {
        stop.stop = true;
        try {
          mutator.join();
        } catch (Throwable t) {
          notifyError(t);
        }
      }
    }

  }

  private static class Stop {
    volatile boolean stop;
  }

}
