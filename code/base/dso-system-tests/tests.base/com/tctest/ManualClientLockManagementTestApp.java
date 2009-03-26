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
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.concurrent.CyclicBarrier;

public class ManualClientLockManagementTestApp extends AbstractErrorCatchingTransparentApp {

  private static final String LOCK = "lock";
  
  private final CyclicBarrier barrier = new CyclicBarrier(getParticipantCount());
  
  public ManualClientLockManagementTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  @Override
  protected void runTest() throws Throwable {
    final int index = barrier.await();
    
    testServerRecallOfPinnedLock(index);
    barrier.await();
    testLockAfterEviction(index);
    barrier.await();
    testEvictionOfHeldLock(index);
  }

  private void testServerRecallOfPinnedLock(int index) throws Throwable {
    if (index == 0) {
      ManagerUtil.beginLock(LOCK, LockLevel.WRITE);
      ManagerUtil.pinLock(LOCK);
      ManagerUtil.commitLock(LOCK);
    }
    
    barrier.await();
    
    if (index == 1) {
      ManagerUtil.beginLock(LOCK, LockLevel.READ);
    }
    
    barrier.await();
    
    if (index == 0) {
      ManagerUtil.beginLock(LOCK, LockLevel.READ);
    }
    
    barrier.await();
    
    ManagerUtil.commitLock(LOCK);   
  }
  
  private void testLockAfterEviction(int index) throws Throwable {
    if (index == 0) {
      ManagerUtil.beginLock(LOCK, LockLevel.WRITE);
      ManagerUtil.pinLock(LOCK);
      ManagerUtil.commitLock(LOCK);
      ManagerUtil.evictLock(LOCK);
    }
    
    barrier.await();
    
    if (index == 1) {
      ManagerUtil.beginLock(LOCK, LockLevel.WRITE);
      ManagerUtil.commitLock(LOCK);
    } 
    
    barrier.await();
    
    if (index == 0) {
      ManagerUtil.beginLock(LOCK, LockLevel.WRITE);
      ManagerUtil.commitLock(LOCK);
    }
  }

  private void testEvictionOfHeldLock(int index) throws Throwable {
    if (index == 0) {
      ManagerUtil.beginLock(LOCK, LockLevel.WRITE);
      ManagerUtil.pinLock(LOCK);
      ManagerUtil.evictLock(LOCK);
      ManagerUtil.commitLock(LOCK);
    }
    
    barrier.await();
    
    if (index == 1) {
      ManagerUtil.beginLock(LOCK, LockLevel.WRITE);
      ManagerUtil.commitLock(LOCK);
    }
  
    barrier.await();
    
    if (index == 0) {
      ManagerUtil.beginLock(LOCK, LockLevel.WRITE);
      ManagerUtil.commitLock(LOCK);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ManualClientLockManagementTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    spec.addRoot("barrier", "barrier");
  }
}
