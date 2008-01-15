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
 * DEV-1153: Deadlock if two threads hydrate a collection each referencing the other collection
 * 
 * If this test fails (deadlock), it will fail with a timeout.
 */
public class MutualReferenceCollectionTest extends TransparentTestBase {
  private static final int NODE_COUNT = 2;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {
    private CyclicBarrier barrier  = new CyclicBarrier(NODE_COUNT);
    private List          evenList = new ArrayList();
    private List          oddList  = new ArrayList();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    protected void runTest() throws Throwable {
      initLists();
      
      ListWorker worker1 = new ListWorker(evenList);
      ListWorker worker2 = new ListWorker(oddList);
      
      barrier.barrier();
      
      worker1.start();
      worker2.start();
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
        synchronized (evenList) {
          for (int i = 0; i < 10000; i += 2) {
            evenList.add(new Integer(i));
          }
          // reference oddList
          evenList.add(oddList);
        }
        synchronized (oddList) {
          for (int i = 1; i < 10000; i += 2) {
            oddList.add(new Integer(i));
          }
          // reference evenList
          oddList.add(evenList);
        }
      }
      barrier.barrier();
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("barrier", "barrier");
      spec.addRoot("evenList", "evenList");
      spec.addRoot("oddList", "oddList");
      String methodExpression = "* " + testClassName + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      config.addIncludePattern(testClassName + "$*");
      config.addReadAutolock("* " + testClassName + "$*.*(..)");

      new CyclicBarrierSpec().visit(visitor, config);
    }

    private static class ListWorker extends Thread {
      private Throwable error;
      private List      list;

      public ListWorker(List list) {
        this.list = list;
      }

      private void doWork() throws Throwable {
        // iterate through the list to force faulting
        synchronized (list) {
          for (Iterator it = list.iterator(); it.hasNext();) {
            it.next();
            Thread.sleep(5 + (int)(Math.random() * 10));
          }
        }
      }

      public void run() {
        try {
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
