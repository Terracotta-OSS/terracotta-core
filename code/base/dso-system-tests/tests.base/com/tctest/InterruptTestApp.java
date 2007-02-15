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
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

public class InterruptTestApp extends AbstractTransparentApp {
  private volatile boolean    interruptedFlag = false;
  private final Object        lockObject      = new Object();
  private final SharedData    sharedData      = new SharedData();

  private final CyclicBarrier barrier;

  public InterruptTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();
      testWaitInterrupt1(index);
      testWaitInterrupt2(index);
      testWaitInterrupt3(index);
      testWaitInterrupt4(index);
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void testWaitInterrupt1(int index) throws Exception {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            synchronized (lockObject) {
              localBarrier.barrier();
              lockObject.wait();
              throw new AssertionError("Should have thrown an InterruptedException.");
            }
          } catch (InterruptedException e) {
            interruptedFlag = true;
          } catch (Throwable e) {
            notifyError(e);
          }
        }
      });
      t.start();
      localBarrier.barrier();
      t.interrupt();
      while (!interruptedFlag) {
        // do nothing
      }
      //while (!interruptedFlag && t.isAlive()) {
      //  t.interrupt();
     // }
      Assert.assertTrue(interruptedFlag);
      interruptedFlag = false;
    }
    barrier.barrier();
  }

  private void testWaitInterrupt2(int index) throws Exception {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          synchronized (lockObject) {
            try {
              localBarrier.barrier();
              lockObject.wait();
              throw new AssertionError("Should have thrown an InterruptedException.");
            } catch (InterruptedException e) {
              interruptedFlag = true;
            } catch (Throwable e) {
              notifyError(e);
            }
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run() {
          synchronized(lockObject) {
            try {
              localBarrier.barrier();
              lockObject.wait();
            } catch (InterruptedException ie) {
              interruptedFlag = true;
            } catch (Throwable t) {
              notifyError(t);
            }
          }
        }
      });
      t1.start();
      localBarrier.barrier();
      t2.start();
      localBarrier.barrier();
      t1.interrupt();
      while (!interruptedFlag) {
        // do nothing
      }
      //while (!interruptedFlag && t1.isAlive()) {
      //  t1.interrupt();
      //}
      interruptedFlag = false;
      t2.interrupt();
      while (!interruptedFlag) {
        // do nothing
      }
      //while (!interruptedFlag && t2.isAlive()) {
      //  t2.interrupt();
      //}
      interruptedFlag = false;
    }
    barrier.barrier();
  }
  
  private void testWaitInterrupt3(int index) throws Exception {
    System.err.println("Running test 3");
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          synchronized (lockObject) {
            try {
              localBarrier.barrier();
              lockObject.wait();
              throw new AssertionError("Should have thrown an InterruptedException.");
            } catch (InterruptedException e) {
              interruptedFlag = true;
            } catch (Throwable e) {
              notifyError(e);
            }
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run() {
          synchronized(lockObject) {
            try {
              localBarrier.barrier();
              Thread.sleep(10000);
            } catch (Throwable t) {
              notifyError(t);
            }
          }
        }
      });
      t1.start();
      localBarrier.barrier();
      t2.start();
      localBarrier.barrier();
      t1.interrupt();
      while (!interruptedFlag) {
        // do nothing
      }
      //while (!interruptedFlag && t1.isAlive()) {
      //  t1.interrupt();
      //}
      interruptedFlag = false;
    }
    barrier.barrier();
  }
  
  /**
   *  This test involves 2 nodes. One node goes to wait, while a remote node grabs the lock. The first node
   *  get interrupted, wait for the other node to release the lock and grabs the lock back.
   */
  private void testWaitInterrupt4(int index) throws Exception {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          synchronized (lockObject) {
            try {
              localBarrier.barrier();
              lockObject.wait();
              throw new AssertionError("Should have thrown an InterruptedException.");
            } catch (InterruptedException e) {
              interruptedFlag = true;
            } catch (Throwable e) {
              notifyError(e);
            }
            Assert.assertEquals(10, sharedData.getData());
            sharedData.setData(20);
          }
        }
      });
      t1.start();
      localBarrier.barrier();
      barrier.barrier();
      barrier.barrier();
      t1.interrupt();
      while (!interruptedFlag) {
        // do nothing
      }
      //while (!interruptedFlag && t1.isAlive()) {
      //  t1.interrupt();
      //}
      interruptedFlag = false;
    } else {
      barrier.barrier();
      synchronized(lockObject) {
        barrier.barrier();
        Thread.sleep(10000);
        sharedData.setData(10);
      }
    }
    barrier.barrier();
    
    Assert.assertEquals(20, sharedData.getData());
    
    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = InterruptTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("lockObject", "lockObject");
    spec.addRoot("sharedData", "sharedData");
  }
  
  private static class SharedData {
    private int data;

    public int getData() {
      return data;
    }

    public void setData(int data) {
      this.data = data;
    }
  }
}
