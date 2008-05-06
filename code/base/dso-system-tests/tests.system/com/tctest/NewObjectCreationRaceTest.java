/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class NewObjectCreationRaceTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return NewObjectCreationRaceTestApp.class;
  }

  public static class NewObjectCreationRaceTestApp extends AbstractErrorCatchingTransparentApp {

    // NOTE: it is very important to NOT reference
    // this root in the "fault" nodes, until after it has the new object in it
    private Map                 root;

    private final CyclicBarrier barrier;

    public NewObjectCreationRaceTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);

      if (getParticipantCount() < 2) { throw new AssertionError("need at least two nodes for this test to work"); }

      barrier = new CyclicBarrier(getParticipantCount());
    }

    protected void runTest() throws Throwable {
      int n = barrier.barrier();

      if (n == 0) {
        runCreateNode();
      } else {
        runFaultNode();
      }
    }

    private void runFaultNode() throws Exception {
      barrier.barrier();

      final Map faultedRoot;

      // fault the root under the same lock it was modified under
      // this avoids a ConcurrentModificationException
      synchronized (barrier) {
        faultedRoot = root;
      }

      for (Iterator i = faultedRoot.entrySet().iterator(); i.hasNext();) {
        Map.Entry entry = (Entry) i.next();
        Object key = entry.getKey();
        System.out.println(key);
        Object value = entry.getValue();
        System.out.println(value);

        if (value instanceof Ref) {
          Object ref = ((Ref) value).getRef();
          System.out.println(ref);
        }
      }

      // unblock the create node
      barrier.barrier();

      // wait for the create node to commit it's change
      barrier.barrier();

      // make sure the delta is in there
      synchronized (faultedRoot) {
        Object o = faultedRoot.get("delta");
        Assert.assertEquals("non-null", o);
        Assert.assertEquals(5, faultedRoot.size());
      }
    }

    private void runCreateNode() throws Throwable {
      final SynchronizedRef error = new SynchronizedRef(null);

      // create root in this node only
      root = new HashMap();

      final Object newObj = new Object();
      final Ref newRefToOtherNewObject = new Ref(new Object());

      synchronized (root) {
        root.put("delta", null);
        root.put("new object", newObj);
        root.put("new ref to new obj", newRefToOtherNewObject);

        Runnable otherTxn = new Runnable() {
          public void run() {
            try {
              synchronized (barrier) {
                root.put("ref to new with ref to created", new Ref(newObj));
                root.put("ref to created with ref to created", newRefToOtherNewObject);
              }
            } catch (Throwable err) {
              error.set(err);
            }
          }
        };

        Thread t1 = new Thread(otherTxn);
        t1.start();
        t1.join();

        checkError(error);

        // unblock the "fault" nodes. Need to do this in another thread so that the
        // TXN the current thread is in doesn't commit()
        Runnable doBarrier = new Runnable() {
          public void run() {
            try {
              barrier.barrier();
            } catch (Throwable err) {
              error.set(err);
            }
          }
        };
        Thread t2 = new Thread(doBarrier);
        t2.start();
        t2.join();

        checkError(error);

        // block at least until the other node(s) cause the problematic fault. This also needs to be done in another
        // thread so that this thread's TXN does not commit
        Thread t3 = new Thread(doBarrier);
        t3.start();
        t3.join();

        checkError(error);

        // create a change (ie. delta DNA) in the original transaction
        root.put("delta", "non-null");

        barrier.barrier();
      }

    }

    private void checkError(SynchronizedRef error) throws Throwable {
      Throwable t = (Throwable) error.get();
      if (t != null) { throw t; }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      new CyclicBarrierSpec().visit(visitor, config);

      String testClass;
      TransparencyClassSpec spec;
      String methodExpression;

      testClass = Ref.class.getName();
      spec = config.getOrCreateSpec(testClass);

      testClass = NewObjectCreationRaceTest.class.getName();
      spec = config.getOrCreateSpec(testClass);
      methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      config.addIncludePattern(testClass + "$*");

      testClass = NewObjectCreationRaceTestApp.class.getName();
      spec = config.getOrCreateSpec(testClass);

      methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);
      spec.addRoot("barrier", "barrier");
      spec.addRoot("root", "root");
    }

  }

  private static class Ref {
    private final Object ref;

    Ref(Object ref) {
      this.ref = ref;
    }

    Object getRef() {
      return ref;
    }
  }

}
