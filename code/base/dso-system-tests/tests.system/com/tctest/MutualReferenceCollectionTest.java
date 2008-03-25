/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * DEV-1153: Deadlock if two threads hydrate a collection each referencing the other collection If this test fails
 * (deadlock), it will fail with a timeout.
 */
public class MutualReferenceCollectionTest extends TransparentTestBase {
  private static final int NODE_COUNT = 3;

  public MutualReferenceCollectionTest() {
    // DEV-1153 - MNK-481
    disableAllUntil("2008-06-01");
  }
  
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {
    private CyclicBarrier barrier    = new CyclicBarrier(NODE_COUNT);
    private List          firstList  = new ArrayList();
    private List          secondList = new ArrayList();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    protected void runTest() throws Throwable {
      initLists();
      System.out.println("Done initializing lists...");
      
      CyclicBarrier readBarrier = new CyclicBarrier(2);
      ListWorker worker1 = new ListWorker(readBarrier, firstList);
      ListWorker worker2 = new ListWorker(readBarrier, secondList);

      System.out.println("Waiting to start workers...");
      barrier.barrier();      
      
      worker1.start();
      worker2.start();
      System.out.println("workers started...");
      worker1.join();
      worker2.join();

      if (worker1.getError() != null) { throw worker1.getError(); }
      if (worker2.getError() != null) { throw worker2.getError(); }
    }

    /**
     * set up lists and mutually reference each other
     */
    private void initLists() throws Exception {
      if (barrier.barrier() == 0) {
        synchronized (firstList) {
          for (int i = 0; i < 5000; i++) {
            firstList.add(new Object());
          }
          // reference oddList
          firstList.add(secondList);
        }
        synchronized (secondList) {
          for (int i = 1; i < 5000; i++) {
            secondList.add(new Object());
          }
          // reference evenList
          secondList.add(firstList);
        }
      }
      barrier.barrier();
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("barrier", "barrier");
      spec.addRoot("firstList", "firstList");
      spec.addRoot("secondList", "secondList");
      String methodExpression = "* " + testClassName + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      config.addIncludePattern(testClassName + "$*");
      config.addReadAutolock("* " + testClassName + "$*.*(..)");

      new CyclicBarrierSpec().visit(visitor, config);
    }

    private static class ListWorker extends Thread {
      private Throwable     error;
      private CyclicBarrier readBarrier;
      private List          list;

      public ListWorker(CyclicBarrier readBarrier, List list) {
        this.readBarrier = readBarrier;
        this.list = list;
      }

      private void doWork() throws Throwable {
        // iterate through the list to force faulting
        synchronized (list) {
          for (Iterator it = list.iterator(); it.hasNext();) {
            it.next();
            Thread.sleep(5 + (int) (Math.random() * 10));
          }
        }
      }

      public void run() {
        try {
          System.out.println("waiting for readBarrier...");
          readBarrier.barrier();
          System.out.println("Client " + ManagerUtil.getClientID() + ", thread " + getName() + ": start work...");
          doWork();
          System.out.println("Client " + ManagerUtil.getClientID() + ", thread " + getName() + ": Done");
        } catch (Throwable t) {
          error = t;
        }
      }

      public Throwable getError() {
        return error;
      }
    }
  }
}
