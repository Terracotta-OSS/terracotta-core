/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.TCObjectNotSharableException;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author hhuynh
 */
public class NotShareableReadWriteLockTestApp extends AbstractErrorCatchingTransparentApp {

  private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private List                   list = new ArrayList();             // root

  public NotShareableReadWriteLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    testNotShareableLock(lock.readLock());
    testNotShareableLock(lock.writeLock());
  }

  private void testNotShareableLock(Lock readOrWriteLock) throws Exception {
    readOrWriteLock.lock();
    try {
      synchronized (list) {
        list.add(readOrWriteLock);
      }
      System.err.println("XXX Not expected to reach.");
      throw new AssertionError("Expecting TCObjectNotSharableException while trying to share read/write lock");
    } catch (TCObjectNotSharableException e) {
      // expected
      System.out.println("TCObjectNotSharableException thrown as expected");
    } finally {
      readOrWriteLock.unlock();
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = NotShareableReadWriteLockTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("list", "list");
  }
}
