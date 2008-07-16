/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockReadLockInsideWriteLockTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  @Override
  protected Class getApplicationClass() {
    return ReentrantReadWriteLockReadLockInsideWriteLockTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  public static class ReentrantReadWriteLockReadLockInsideWriteLockTestApp extends AbstractErrorCatchingTransparentApp {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock lock2 = new ReentrantReadWriteLock(true);
    private final List                   list  = new ArrayList();

    public ReentrantReadWriteLockReadLockInsideWriteLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = ReentrantReadWriteLockReadLockInsideWriteLockTestApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      config.addIncludePattern(testClass + "$*");

      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("lock", "lock");
      spec.addRoot("lock2", "lock2");
      spec.addRoot("list", "list");
    }

    @Override
    protected void runTest() throws Throwable {
      runTestLock();
      runTestTryLock();
    }
    
    protected void runTestLock() throws Throwable {

      // this section is entirely protected by a write lock
      lock.writeLock().lock();

      list.add(new Object());
      list.get(0);

      lock2.readLock().lock();

      list.add(new Object());
      list.get(0);

      lock.readLock().lock();

      list.add(new Object());
      list.get(0);

      lock.readLock().unlock();

      list.add(new Object());
      list.get(0);

      lock2.readLock().unlock();

      list.add(new Object());
      list.get(0);

      lock.writeLock().unlock();

      // this section starts out with a read only lock and
      // then acquires a write lock on another lock
      lock2.readLock().lock();

      try {
        list.add(new Object());
        fail("Expected ReadOnlyException");
      } catch (ReadOnlyException e) {
        // expected
      }
      list.get(0);

      lock.writeLock().lock();

      list.add(new Object());
      list.get(0);

      lock.readLock().lock();

      list.add(new Object());
      list.get(0);

      lock.readLock().unlock();

      list.add(new Object());
      list.get(0);

      lock.writeLock().unlock();

      try {
        list.add(new Object());
        fail("Expected ReadOnlyException");
      } catch (ReadOnlyException e) {
        // expected
      }
      list.get(0);

      lock2.readLock().unlock();
    }

    protected void runTestTryLock() throws Throwable {

      // this section is entirely protected by a write lock
      assertTrue(lock.writeLock().tryLock());

      list.add(new Object());
      list.get(0);

      assertTrue(lock2.readLock().tryLock());

      list.add(new Object());
      list.get(0);

      assertTrue(lock.readLock().tryLock());

      list.add(new Object());
      list.get(0);

      lock.readLock().unlock();

      list.add(new Object());
      list.get(0);

      lock2.readLock().unlock();

      list.add(new Object());
      list.get(0);

      lock.writeLock().unlock();

      // this section starts out with a read only lock and
      // then acquires a write lock on another lock
      assertTrue(lock2.readLock().tryLock());

      try {
        list.add(new Object());
        fail("Expected ReadOnlyException");
      } catch (ReadOnlyException e) {
        // expected
      }
      list.get(0);

      assertTrue(lock.writeLock().tryLock());

      list.add(new Object());
      list.get(0);

      assertTrue(lock.readLock().tryLock());

      list.add(new Object());
      list.get(0);

      lock.readLock().unlock();

      list.add(new Object());
      list.get(0);

      lock.writeLock().unlock();

      try {
        list.add(new Object());
        fail("Expected ReadOnlyException");
      } catch (ReadOnlyException e) {
        // expected
      }
      list.get(0);

      lock2.readLock().unlock();
    }
  }
}
