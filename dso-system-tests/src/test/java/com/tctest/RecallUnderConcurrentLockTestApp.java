/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.runner.AbstractTransparentApp;

public class RecallUnderConcurrentLockTestApp extends AbstractTransparentApp {

  private static final String LOCK_STRING = "lock";

  private final CyclicBarrier barrier     = new CyclicBarrier(getParticipantCount());

  public RecallUnderConcurrentLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = RecallUnderConcurrentLockTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("barrier", "barrier");
  }

  public void run() {
    try {
      int nodeId = this.barrier.await();

      if (nodeId == 0) {
        // Grab the greedy lock
        ManagerUtil.beginLock(LOCK_STRING, Manager.LOCK_TYPE_WRITE);
        ManagerUtil.commitLock(LOCK_STRING, Manager.LOCK_TYPE_WRITE);

        ManagerUtil.beginLock(LOCK_STRING, Manager.LOCK_TYPE_CONCURRENT);
        try {
          this.barrier.await();
          // other node tries to take the lock here
          ThreadUtil.reallySleep(5000);
        } finally {
          // Unlock before awaiting
          ManagerUtil.commitLock(LOCK_STRING, Manager.LOCK_TYPE_CONCURRENT);
        }
        this.barrier.await();
      } else {
        this.barrier.await();
        ManagerUtil.beginLock(LOCK_STRING, Manager.LOCK_TYPE_READ);
        try {
          this.barrier.await();
        } finally {
          ManagerUtil.commitLock(LOCK_STRING, Manager.LOCK_TYPE_READ);
        }
      }

      this.barrier.await();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
