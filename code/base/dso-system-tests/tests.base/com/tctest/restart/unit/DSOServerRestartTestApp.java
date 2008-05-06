/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.unit;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.FutureResult;

import com.tc.exception.TCRuntimeException;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.LockDefinitionImpl;
import com.tc.object.config.Root;
import com.tctest.restart.AbstractRestartTestApp;

import java.lang.reflect.InvocationTargetException;

public class DSOServerRestartTestApp extends AbstractRestartTestApp implements RestartUnitTestApp {

  private final FutureResult fallThroughControl          = new FutureResult();
  private final Object[]     sharedLockHolder            = new Object[1];
  private final Object[]     distributedSharedLockHolder = new Object[1];
  private CyclicBarrier      startBarrier;

  public DSOServerRestartTestApp(ThreadGroup threadGroup) {
    super(threadGroup);
    changeState(INIT);
  }

  public String toString() {
    return getClass().getName() + "[" + getID() + ", " + state.peek() + "]";
  }

  public void setStartBarrier(CyclicBarrier startBarrier) {
    this.startBarrier = startBarrier;
  }

  public void setSharedLock(Object lck) {
    synchronized (this.sharedLockHolder) {
      this.sharedLockHolder[0] = lck;
    }
  }

  public void setDistributedSharedLock(Object lck) {
    synchronized (this.distributedSharedLockHolder) {
      this.distributedSharedLockHolder[0] = lck;
    }
  }

  public Object getSharedLock() {
    Object rv = null;
    synchronized (this.sharedLockHolder) {
      if (this.sharedLockHolder[0] != null) rv = this.sharedLockHolder[0];
    }
    if (rv == null) {
      synchronized (this.distributedSharedLockHolder) {
        rv = this.distributedSharedLockHolder[0];
      }
    }
    System.out.println("returning shared lock...");
    return rv;
  }

  public synchronized void reset() {
    if (!(isEnd() || isInit())) throw new IllegalStateException("call to reset() when not in STOP state or INIT");
    changeState(INIT);
  }

  public void doWait(long millis) {
    if (!isInit()) throw new IllegalStateException("call to doWait() when not in INIT state.");
    try {
      changeState(START);
      this.startBarrier.barrier();
      System.out.println("About to call basicDoWait()...");
      basicDoWait(millis);
      changeState(END);
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  private void basicDoWait(long millis) throws InterruptedException {
    Object sharedLock = getSharedLock();
    synchronized (sharedLock) {
      changeState(WAITER);
      sharedLock.wait(millis);
      changeState(HOLDER);
      // clear the state so no one can check it until it is set again.
      this.state.clear();
    }
  }

  public void doNotify() {
    Object sharedLock = getSharedLock();
    synchronized (sharedLock) {
      System.out.println("About to call notify()");
      sharedLock.notify();
    }
  }

  public void doNotifyAll() {
    Object sharedLock = getSharedLock();
    synchronized (sharedLock) {
      System.out.println("About to call notifyAll()");
      sharedLock.notifyAll();
    }
  }

  public void attemptLock() {
    if (!isInit()) throw new IllegalStateException("call to attemptLock() when not in INIT state.");
    try {
      changeState(START);
      this.startBarrier.barrier();
      System.out.println("About to call basicAttemptLock()...");
      basicAttemptLock();
      changeState(END);
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  private void basicAttemptLock() throws InterruptedException, InvocationTargetException {
    synchronized (getSharedLock()) {
      // synchronizing on the shared lock simulates the named lock on this
      // method.
      changeState(HOLDER);
      fallThroughControl.get();
      // clear the state so no one can check the state until it is set again.
      this.state.clear();
    }
  }

  public void blockShutdown(final FutureResult blocker) throws Exception {
    final FutureResult callback = new FutureResult();
    new Thread(this.threadGroup, new Runnable() {
      public void run() {
        try {
          syncBlockShutdown(blocker, callback);
        } catch (Exception e) {
          throw new TCRuntimeException(e);
        }
      }
    }).start();
    callback.get();
  }

  private void syncBlockShutdown(FutureResult blocker, FutureResult callback) throws InterruptedException,
      InvocationTargetException {
    System.out.println("Blocking shutdown...");
    callback.set(new Object());
    blocker.get();
  }

  public void fallThrough() {
    if (!isHolder()) throw new IllegalStateException("Attempt to release a lock without owning it.");
    try {
      fallThroughControl.set(new Object());
      synchronized (this) {
        while (!isEnd())
          wait();
      }
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper cfg) {
    String appClassName = DSOServerRestartTestApp.class.getName();
    cfg.addIncludePattern(appClassName);
    cfg.addRoot(new Root(appClassName, "distributedSharedLockHolder", appClassName + ".distributedSharedLockHolder"),
                true);
    cfg.addWriteAutolock("void " + appClassName + ".setDistributedSharedLock(java.lang.Object)");
    cfg.addReadAutolock("java.lang.Object " + appClassName + ".getSharedLock()");

    cfg.addWriteAutolock("void " + appClassName + ".basicDoWait(..)");
    cfg.addWriteAutolock("void " + appClassName + ".doNotify()");
    cfg.addWriteAutolock("void " + appClassName + ".doNotifyAll()");
    LockDefinition lockDefinition = new LockDefinitionImpl("basicAttemptLock", ConfigLockLevel.WRITE);
    lockDefinition.commit();
    cfg.addLock("void " + appClassName + ".basicAttemptLock()", lockDefinition);

    lockDefinition = new LockDefinitionImpl("blockShutdown", ConfigLockLevel.WRITE);
    lockDefinition.commit();
    cfg.addLock("void " + appClassName + ".syncBlockShutdown(..)", lockDefinition);
  }

}
