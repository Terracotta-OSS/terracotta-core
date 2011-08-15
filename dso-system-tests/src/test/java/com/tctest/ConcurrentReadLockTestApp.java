/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.exception.TCRuntimeException;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

public class ConcurrentReadLockTestApp extends AbstractTransparentApp {

  private final CyclicBarrier barrier;
  private final CyclicBarrier barrier2;
  private final DataRoot      dataRoot      = new DataRoot();
  private final SharedObject  sharedObjRoot = new SharedObject();

  public ConcurrentReadLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    barrier2 = new CyclicBarrier(getParticipantCount() * 2 + getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();

      testConcurrentReadMethodWithinSingleJVM(index);
      testConcurrentReadMethod(index);
      testConcurrentReadBlockWithinSingleJVM(index);
      testConcurrentReadBlock(index);
      testConcurrentReadBlockWithinSingleJVMOnSharedObject(index);
      testConcurrentReadBlockWithinSingleJVMOnSharedStr(index);
      testConcurrentReadBlockWithinSingleJVMOnUnsharedAndSharedObject(index);

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void testConcurrentReadMethod(int index) throws Exception {
    Runnable runnable = new Runnable() {
      public void run() {
        try {
          int rv = dataRoot.testConcurrentReadMethod(barrier2);
          Assert.assertEquals(1, rv);
        } catch (Exception e) {
          throw new TCRuntimeException(e);
        }
      }
    };

    Thread t1 = new Thread(runnable);
    Thread t2 = new Thread(runnable);

    t1.start();
    t2.start();
    dataRoot.testConcurrentReadMethod(barrier2);

    barrier.barrier();
  }

  private void testConcurrentReadMethodWithinSingleJVM(int index) throws Exception {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Runnable runnable = new Runnable() {
        public void run() {
          try {
            int rv = dataRoot.testConcurrentReadMethod(localBarrier);
            Assert.assertEquals(1, rv);
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      };

      Thread t1 = new Thread(runnable);
      Thread t2 = new Thread(runnable);

      t1.start();
      t2.start();
      dataRoot.testConcurrentReadMethod(localBarrier);
    }

    barrier.barrier();
  }

  private void testConcurrentReadBlock(int index) throws Exception {
    Runnable runnable = new Runnable() {
      public void run() {
        try {
          int rv = dataRoot.testConcurrentReadBlock(barrier2);
          Assert.assertEquals(2, rv);
        } catch (Exception e) {
          throw new TCRuntimeException(e);
        }
      }
    };

    Thread t1 = new Thread(runnable);
    Thread t2 = new Thread(runnable);

    t1.start();
    t2.start();
    dataRoot.testConcurrentReadMethod(barrier2);

    barrier.barrier();
  }

  private void testConcurrentReadBlockWithinSingleJVM(int index) throws Exception {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Runnable runnable = new Runnable() {
        public void run() {
          try {
            int rv = dataRoot.testConcurrentReadBlock(localBarrier);
            Assert.assertEquals(2, rv);
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      };

      Thread t1 = new Thread(runnable);
      Thread t2 = new Thread(runnable);

      t1.start();
      t2.start();
      dataRoot.testConcurrentReadMethod(localBarrier);
    }

    barrier.barrier();
  }

  private void testConcurrentReadBlockWithinSingleJVMOnSharedObject(int index) throws Exception {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      final SharedObject sharedObj = new SharedObject();
      sharedObjRoot.makeShared(sharedObj);

      Runnable runnable = new Runnable() {
        public void run() {
          try {
            int rv = dataRoot.testConcurrentReadBlockOnSharedObj(sharedObj, localBarrier);
            Assert.assertEquals(3, rv);
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      };

      Thread t1 = new Thread(runnable);
      Thread t2 = new Thread(runnable);

      t1.start();
      t2.start();
      dataRoot.testConcurrentReadBlockOnSharedObj(sharedObj, localBarrier);
    }

    barrier.barrier();
  }

  private void testConcurrentReadBlockWithinSingleJVMOnSharedStr(int index) throws Exception {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      final String str = "1234";
      sharedObjRoot.makeShared(str);

      Runnable runnable = new Runnable() {
        public void run() {
          try {
            int rv = dataRoot.testConcurrentReadBlockOnSharedObj(str, localBarrier);
            Assert.assertEquals(3, rv);
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      };

      Thread t1 = new Thread(runnable);
      Thread t2 = new Thread(runnable);

      t1.start();
      t2.start();
      dataRoot.testConcurrentReadBlockOnSharedObj(str, localBarrier);
    }

    barrier.barrier();
  }

  private void testConcurrentReadBlockWithinSingleJVMOnUnsharedAndSharedObject(int index) throws Exception {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      final SharedObject sharedObj = new SharedObject();
      sharedObjRoot.makeShared(sharedObj);

      Runnable runnable = new Runnable() {
        public void run() {
          try {
            int rv = dataRoot
                .testConcurrentReadBlockOnUnsharedAndSharedObj(new SharedObject(), sharedObj, localBarrier);
            Assert.assertEquals(4, rv);
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      };

      Thread t1 = new Thread(runnable);
      Thread t2 = new Thread(runnable);

      t1.start();
      t2.start();
      dataRoot.testConcurrentReadBlockOnUnsharedAndSharedObj(new SharedObject(), sharedObj, localBarrier);
    }

    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = ConcurrentReadLockTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "$DataRoot.testConcurrentReadMethod(..)";
    config.addAutolock(methodExpression, ConfigLockLevel.READ);

    methodExpression = "* " + testClass + "$DataRoot.testConcurrentReadBlock(..)";
    config.addAutolock(methodExpression, ConfigLockLevel.READ);

    methodExpression = "* " + testClass + "$DataRoot.testConcurrentReadBlockOnSharedObj(..)";
    config.addAutolock(methodExpression, ConfigLockLevel.READ);

    methodExpression = "* " + testClass + "$DataRoot.testConcurrentReadBlockOnUnsharedAndSharedObj(..)";
    config.addAutolock(methodExpression, ConfigLockLevel.READ);

    methodExpression = "* " + testClass + "$SharedObject.makeShared(..)";
    config.addAutolock(methodExpression, ConfigLockLevel.WRITE);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("barrier2", "barrier2");
    spec.addRoot("dataRoot", "dataRoot");
    spec.addRoot("sharedObjRoot", "sharedObjRoot");
  }

  @SuppressWarnings("unused")
  private static class SharedObject {

    private SharedObject obj;
    private String       str;

    public SharedObject() {
      super();
    }

    public synchronized void makeShared(SharedObject object) {
      this.obj = object;
    }

    public synchronized void makeShared(String object) {
      this.str = object;
    }
  }

  private static class DataRoot {
    public DataRoot() {
      super();
    }

    public synchronized int testConcurrentReadMethod(CyclicBarrier barrier) throws Exception {
      barrier.barrier();

      return 1;
    }

    public int testConcurrentReadBlock(CyclicBarrier barrier) throws Exception {
      synchronized (this) {
        barrier.barrier();

        return 2;
      }
    }

    public int testConcurrentReadBlockOnSharedObj(Object sharedObj, CyclicBarrier barrier) throws Exception {
      synchronized (sharedObj) {
        barrier.barrier();

        return 3;
      }
    }

    public int testConcurrentReadBlockOnUnsharedAndSharedObj(Object unsharedObj, Object sharedObj, CyclicBarrier barrier)
        throws Exception {
      synchronized (unsharedObj) {
        synchronized (sharedObj) {
          barrier.barrier();

          return 4;
        }
      }
    }

  }

}
