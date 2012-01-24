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
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public class ManualClientLockManagementTestApp extends AbstractErrorCatchingTransparentApp {

  private static final String LOCK    = "lock";

  private final CyclicBarrier barrier = new CyclicBarrier(getParticipantCount());

  public ManualClientLockManagementTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  @Override
  protected void runTest() throws Throwable {
    final int index = barrier.await();

    testServerRecallOfPinnedLock(index);
    barrier.await();
    testGarbageCollectionOfPinnedLock(index);
    barrier.await();
    testGarbageCollectionOfUnpinnedLock(index);
  }

  private void testServerRecallOfPinnedLock(int index) throws Throwable {
    System.err.println("testServerRecallOfPinnedLock(" + index + ")");
    if (index == 0) {
      ManagerUtil.beginLock(LOCK, Manager.LOCK_TYPE_WRITE);
      ManagerUtil.pinLock(LOCK);
      ManagerUtil.commitLock(LOCK, Manager.LOCK_TYPE_WRITE);
    }

    barrier.await();

    if (index == 1) {
      ManagerUtil.beginLock(LOCK, Manager.LOCK_TYPE_READ);
    }

    barrier.await();

    if (index == 0) {
      ManagerUtil.beginLock(LOCK, Manager.LOCK_TYPE_READ);
      ManagerUtil.unpinLock(LOCK);
    }

    barrier.await();

    ManagerUtil.commitLock(LOCK, Manager.LOCK_TYPE_READ);
  }

  private void testGarbageCollectionOfPinnedLock(int index) throws Throwable {
    System.err.println("testGarbageCollectionOfPinnedLock(" + index + ")");
    if (index == 0) {
      ManagerUtil.beginLock(LOCK, Manager.LOCK_TYPE_WRITE);
      ManagerUtil.pinLock(LOCK);
      ManagerUtil.commitLock(LOCK, Manager.LOCK_TYPE_WRITE);
    }

    barrier.await();

    ThreadUtil.reallySleep(2000);

    if (index == 1) {
      ManagerUtil.beginLock(LOCK, Manager.LOCK_TYPE_READ);
    }

    barrier.await();

    ThreadUtil.reallySleep(2000);

    if (index == 0) {
      ManagerUtil.beginLock(LOCK, Manager.LOCK_TYPE_READ);
    }

    barrier.await();

    ManagerUtil.commitLock(LOCK, Manager.LOCK_TYPE_READ);

    barrier.await();

    ThreadUtil.reallySleep(2000);

    barrier.await();
    if (index == 1) {
      System.err.println("Client " + ManagerUtil.getClientID() + " Should Have Collected");
    }

    ThreadUtil.reallySleep(2000);

    if (index == 0) {
      ManagerUtil.unpinLock(LOCK);
    }

    ThreadUtil.reallySleep(2000);
    if (index == 0) {
      System.err.println("Client " + ManagerUtil.getClientID() + " Should Have Collected");
    }

    barrier.await();
  }

  private void testGarbageCollectionOfUnpinnedLock(int index) throws Throwable {
    if (index == 0) {
      ManagerUtil.beginLock(LOCK, Manager.LOCK_TYPE_WRITE);
      ManagerUtil.pinLock(LOCK);
      ManagerUtil.commitLock(LOCK, Manager.LOCK_TYPE_WRITE);
      ManagerUtil.unpinLock(LOCK);
    }

    barrier.await();

    ThreadUtil.reallySleep(2000);

    barrier.await();
    System.err.println("Client " + ManagerUtil.getClientID() + " Should Have Collected");

    ManagerUtil.beginLock(LOCK, Manager.LOCK_TYPE_READ);
    ManagerUtil.commitLock(LOCK, Manager.LOCK_TYPE_READ);

    barrier.await();

    ThreadUtil.reallySleep(2000);

    barrier.await();
    System.err.println("Client " + ManagerUtil.getClientID() + " Should Have Collected");

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ManualClientLockManagementTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    spec.addRoot("barrier", "barrier");
  }
}
