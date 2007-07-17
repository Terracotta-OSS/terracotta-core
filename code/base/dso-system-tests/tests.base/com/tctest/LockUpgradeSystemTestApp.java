/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.object.tx.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LockUpgradeSystemTestApp extends AbstractTransparentApp {

  private Map root = new HashMap();

  public LockUpgradeSystemTestApp(String globalId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(globalId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = LockUpgradeSystemTestApp.class.getName();

    config.addRoot(new Root(testClassName, "root", "root" + "Lock"), true);

    String methodExpression = "* " + testClassName + ".writeLock(..)";
    config.addWriteAutolock(methodExpression);

    methodExpression = "* " + testClassName + ".readLock(..)";
    config.addReadAutolock(methodExpression);
  }

  public void run() {
    Random random = new Random(new Random(System.currentTimeMillis() + getApplicationId().hashCode()).nextLong());

    for (int i = 0; i < 100; i++) {
      readLock(random.nextInt(4));
    }
  }

  private void readLock(int depth) {
    synchronized (root) {
      // we should have a read lock now

      // upgrade
      writeLock(depth);

      // shouldn't be able to write
      tryWrite();
    }
  }

  private void readLock() {
    synchronized (root) {
      tryWrite();
    }
  }

  private void tryWrite() {
    try {
      root.put("key", "value");
      throw new RuntimeException("read-only transaction context is busted");
    } catch (ReadOnlyException roe) {
      // expected
    }
  }

  private void writeLock(int depth) {
    synchronized (root) {
      readLock();
      writeLock();

      if (depth > 0) {
        writeLock(depth - 1);
      }
    }
  }

  private void writeLock() {
    synchronized (root) {
      //
    }
  }

}
