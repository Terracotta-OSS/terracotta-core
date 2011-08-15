/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockReadLockInsideWriteLockTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

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
    private final CyclicBarrier          barrier  = new CyclicBarrier(NODE_COUNT);

    private String element1a = "element1a";
    private String element1b = "element1b";
    private String element1c = "element1c";
    private String element1d = "element1d";
    private String element1e = "element1e";
    
    private String element2a = "element2a";
    private String element2b = "element2b";
    private String element2c = "element2c";
    private String element2d = "element2d";
    private String element2e = "element2e";
    
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
      spec.addRoot("barrier", "barrier");
    }

    @Override
    protected void runTest() throws Throwable {
      runTestLock();
//      runTestTryLock();
    }
    
    protected void runTestLock() throws Throwable {
      // make changes with lock
      if (0 == barrier.await()) {
        // this section is entirely protected by a write lock
        lock.writeLock().lock();
  
        list.add(element1a);
        assertEquals(element1a, list.get(0));
  
        lock2.readLock().lock();
  
        list.add(element1b);
        assertEquals(element1b, list.get(1));
  
        lock.readLock().lock();
  
        list.add(element1c);
        assertEquals(element1c, list.get(2));
  
        lock.readLock().unlock();
  
        list.add(element1d);
        assertEquals(element1d, list.get(3));
  
        lock2.readLock().unlock();
  
        list.add(element1e);
        assertEquals(element1e, list.get(4));
  
        lock.writeLock().unlock();
      }
      
      if (1 == barrier.await()) {
        lock.readLock().lock();
        assertEquals(5, list.size());
        assertEquals(element1a, list.get(0));
        assertEquals(element1b, list.get(1));
        assertEquals(element1c, list.get(2));
        assertEquals(element1d, list.get(3));
        assertEquals(element1e, list.get(4));
        lock.readLock().unlock();
      }

      // this section starts out with a read only lock and
      // then acquires a write lock on another lock
      if (0 == barrier.await()) {
        lock2.readLock().lock();
  
        try {
          list.add(element2a);
          fail("Expected ReadOnlyException");
        } catch (ReadOnlyException e) {
          // expected
        }
        list.get(0);
  
        lock.writeLock().lock();
  
        list.add(element2b);
        assertEquals(element2b, list.get(5));
  
        lock.readLock().lock();
  
        list.add(element2c);
        assertEquals(element2c, list.get(6));
  
        lock.readLock().unlock();
  
        list.add(element2d);
        assertEquals(element2d, list.get(7));
  
        lock.writeLock().unlock();
  
        try {
          list.add(element2e);
          fail("Expected ReadOnlyException");
        } catch (ReadOnlyException e) {
          // expected
        }
        list.get(0);
  
        lock2.readLock().unlock();
      }
      
      if (1 == barrier.await()) {
        lock.readLock().lock();
        assertEquals(8, list.size());
        assertEquals(element2b, list.get(5));
        assertEquals(element2c, list.get(6));
        assertEquals(element2d, list.get(7));
        lock.readLock().unlock();
      }
      
      barrier.await();
    }

    protected void runTestTryLock() throws Throwable {
      
      if (0 == barrier.await()) {
        // this section is entirely protected by a write lock
        assertTrue(lock.writeLock().tryLock());
        
        list.clear();
        assertEquals(0, list.size());
  
        list.add(element1a);
        assertEquals(element1a, list.get(0));
  
        assertTrue(lock2.readLock().tryLock());
  
        list.add(element1b);
        assertEquals(element1b, list.get(1));
  
        assertTrue(lock.readLock().tryLock());
  
        list.add(element1c);
        assertEquals(element1c, list.get(2));
  
        lock.readLock().unlock();
  
        list.add(element1d);
        assertEquals(element1d, list.get(3));
  
        lock2.readLock().unlock();
  
        list.add(element1e);
        assertEquals(element1e, list.get(4));
  
        lock.writeLock().unlock();
  
        // this section starts out with a read only lock and
        // then acquires a write lock on another lock
        assertTrue(lock2.readLock().tryLock());
  
        try {
          list.add(element2a);
          fail("Expected ReadOnlyException");
        } catch (ReadOnlyException e) {
          // expected
        }
        list.get(0);
  
        assertTrue(lock.writeLock().tryLock());
  
        list.add(element2b);
        assertEquals(element2b, list.get(5));
  
        assertTrue(lock.readLock().tryLock());
  
        list.add(element2c);
        assertEquals(element2c, list.get(6));
  
        lock.readLock().unlock();
  
        list.add(element2d);
        assertEquals(element2d, list.get(7));
  
        lock.writeLock().unlock();
  
        try {
          list.add(element2e);
          fail("Expected ReadOnlyException");
        } catch (ReadOnlyException e) {
          // expected
        }
        list.get(0);
  
        lock2.readLock().unlock();
      }
      
      if (1 == barrier.await()) {
        lock.readLock().lock();
        assertEquals(8, list.size());
        assertEquals(element1a, list.get(0));
        assertEquals(element1b, list.get(1));
        assertEquals(element1c, list.get(2));
        assertEquals(element1d, list.get(3));
        assertEquals(element1e, list.get(4));
        assertEquals(element2b, list.get(5));
        assertEquals(element2c, list.get(6));
        assertEquals(element2d, list.get(7));
        lock.readLock().unlock();
      }
      
      barrier.await();
    }
  }
}
