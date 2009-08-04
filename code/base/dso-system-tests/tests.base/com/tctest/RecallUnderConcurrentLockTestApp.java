/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.lockmanager.api.LockLevel;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.CyclicBarrier;

public class RecallUnderConcurrentLockTestApp extends AbstractTransparentApp {

  private static final String LOCK_STRING = "lock";

  private CyclicBarrier       barrier = new CyclicBarrier(getParticipantCount());

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
      int nodeId = barrier.await();

      if (nodeId == 0) {
        //Grab the greedy lock
        ManagerUtil.beginLock(LOCK_STRING, LockLevel.WRITE);
        ManagerUtil.commitLock(LOCK_STRING);

        ManagerUtil.beginLock(LOCK_STRING, LockLevel.CONCURRENT);
        try {
          barrier.await();
          //other node tries to take the lock here
          barrier.await();
        } finally {
          ManagerUtil.commitLock(LOCK_STRING);
        }
      } else {
        barrier.await();
        ManagerUtil.beginLock(LOCK_STRING, LockLevel.READ);
        try {
          barrier.await();
        } finally {
          ManagerUtil.commitLock(LOCK_STRING);
        }
      }
      
      barrier.await();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
