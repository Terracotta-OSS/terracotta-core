/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

public class NestedReadOnlyTransactionTestApp extends AbstractTransparentApp {
  private final CyclicBarrier barrier;
  private final DataRoot      dataRoot = new DataRoot();

  public NestedReadOnlyTransactionTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();
      nestedReadLockTest(index);
      nestedWriteLockTest(index);
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void nestedReadLockTest(int index) throws Exception {
    if (index == 0) {
      ManagerUtil.monitorEnter(dataRoot, Manager.LOCK_TYPE_WRITE);
      dataRoot.setLongValue(15);
      Assert.assertEquals(15, dataRoot.getSynchronizedLongValue());
    }
    try {
      dataRoot.assertLongValue(index, 15);
    } finally {
      if (index == 0) {
        dataRoot.setCommit(true);
        ManagerUtil.monitorExit(dataRoot, Manager.LOCK_TYPE_WRITE);
      }
    }

    barrier.barrier();
  }

  private void nestedWriteLockTest(int index) throws Exception {
    if (index == 0) {
      dataRoot.clear();
    }

    barrier.barrier();

    if (index == 0) {
      ManagerUtil.monitorEnter(dataRoot, Manager.LOCK_TYPE_WRITE);
      dataRoot.setLongValue(15);
    }
    try {
      if (index == 0) {
        synchronized (dataRoot) {
          long l = dataRoot.getLongValue();
          Assert.assertEquals(15, l);
        }
      }

      barrier.barrier();

      long l = dataRoot.getLongValue();
      Assert.assertEquals(15, l);

      barrier.barrier();
    } finally {
      if (index == 0) {
        ManagerUtil.monitorExit(dataRoot, Manager.LOCK_TYPE_WRITE);
      }
    }

    barrier.barrier();

    Assert.assertEquals(15, dataRoot.getLongValue());

    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = NestedReadOnlyTransactionTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addReadAutolock("* " + testClass + "*.*getSynchronizedLongValue(..)");
    config.addReadAutolock("* " + testClass + "*.*assertLongValue(..)");

    config.addIncludePattern(testClass + "$*");

    new CyclicBarrierSpec().visit(visitor, config);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("dataRoot", "dataRoot");
  }

  private static class DataRoot {
    private final Object readLockObject = new Object();
    private boolean      commit;
    private long         longValue;

    public DataRoot() {
      super();
    }

    public long getSynchronizedLongValue() {
      synchronized (readLockObject) {
        return longValue;
      }
    }

    public long getLongValue() {
      return longValue;
    }

    public void setLongValue(long longValue) {
      this.longValue = longValue;
    }

    public void setCommit(boolean commit) {
      this.commit = commit;
    }

    public void assertLongValue(int index, int newValue) {
      synchronized (readLockObject) {
        if (index == 0) {
          Assert.assertEquals(newValue, longValue);
        } else {
          if (commit) {
            Assert.assertEquals(newValue, longValue);
          } else {
            Assert.assertEquals(0, longValue);
          }
        }
      }
    }

    public synchronized void clear() {
      this.longValue = 0;
      this.commit = false;
    }
  }
}
