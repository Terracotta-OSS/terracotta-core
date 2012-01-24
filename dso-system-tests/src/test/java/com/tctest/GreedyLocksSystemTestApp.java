/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.builtin.ArrayList;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.runner.AbstractTransparentApp;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;

public class GreedyLocksSystemTestApp extends AbstractTransparentApp {

  public static final int     NODE_COUNT      = 3;
  public static final int     EXECUTION_COUNT = 2;
  public static final int     ITERATION_COUNT = 1;

  private final List          locks           = new ArrayList();
  private final CyclicBarrier barrier         = new CyclicBarrier(NODE_COUNT * EXECUTION_COUNT);
  private String              name;
  private static int          id              = 0;
  private static int          count           = 5;

  public GreedyLocksSystemTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = GreedyLocksSystemTestApp.class.getName();
    String lockClass = GreedyLocksSystemTestApp.LockObject.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(lockClass);

    String setIDMethodExpression = "* " + testClass + "*.setID(..)";

    String readMethodExpression = "* " + lockClass + "*.read(..)";
    String writeCountMethodExpression = "* " + lockClass + "*.getWriteCount(..)";
    String timeoutCountMethodExpression = "* " + lockClass + "*.getTimeoutCount(..)";
    String notifyCountMethodExpression = "* " + lockClass + "*.getNotifyCount(..)";
    String writeMethodExpression = "* " + lockClass + "*.write(..)";
    String waitNotifyMethodExpression = "* " + lockClass + "*.waitAndNotify(..)";

    config.addWriteAutolock(setIDMethodExpression);
    config.addReadAutolock(readMethodExpression);
    config.addReadAutolock(writeCountMethodExpression);
    config.addReadAutolock(timeoutCountMethodExpression);
    config.addReadAutolock(notifyCountMethodExpression);
    config.addWriteAutolock(writeMethodExpression);
    config.addWriteAutolock(waitNotifyMethodExpression);
    spec.addRoot("locks", "locks");
    spec.addRoot("barrier", "barrier");
  }

  public void run() {
    name = Thread.currentThread().getName();

    setID();

    try {
      barrier.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (BrokenBarrierException e) {
      e.printStackTrace();
    }

    for (int i = id - 1; i < locks.size(); i++) {
      LockObject lock = (LockObject) locks.get(i);
      int read = 0, write = 0;

      // READ loop
      for (int j = 0; j < count; j++) {
        read = lock.read();
      }
      println(lock + ":: After Read loop :: " + read);

      // Write loop
      for (int j = 0; j < count; j++) {
        write = lock.write();
      }
      println(lock + ": After Write loop :: " + write);

      // Wait notify
      for (int j = 0; j < count; j++) {
        lock.waitAndNotify(toString());
      }
      println(lock + ": Timeouts :: " + lock.getTimeoutCount());
      println(lock + ": Notifies :: " + lock.getNotifyCount());

      // READ, WRITE and Wait Notify
      for (int j = 0; j < count; j++) {
        read = lock.read();
        write = lock.write();
        lock.waitAndNotify(toString());
      }
      println(lock + ":: Reads :: " + read);
      println(lock + ":: Writes :: " + write);
      println(lock + ": Timeouts :: " + lock.getTimeoutCount());
      println(lock + ": Notifies :: " + lock.getNotifyCount());
    }
  }

  private void println(String string) {
    System.out.println(toString() + string);
  }

  @Override
  public String toString() {
    return "Client(" + id + ")::" + name + "::";
  }

  private void setID() {
    synchronized (locks) {
      if (id == 0) {
        // first thread
        id = locks.size() + 1;
        locks.add(new LockObject(locks, id));
        // count *= id;
      }
    }
  }

  private static class LockObject {

    private final List locks;
    private final int  lockID;

    int                writeCount    = 0;
    int                waitCount     = 0;
    int                timeoutCount  = 0;
    int                notifiedCount = 0;

    LockObject(List list, int id) {
      this.locks = list;
      this.lockID = id;
    }

    synchronized int read() {
      int c = writeCount;
      for (int i = 0; i < locks.size(); i++) {
        LockObject lock = (LockObject) locks.get(i);
        if (lock != this) {
          c += lock.getWriteCount();
        }
      }
      return c;
    }

    synchronized void waitAndNotify(String name) {
      waitCount++;
      try {
        long start = System.currentTimeMillis();

        wait(500);
        if ((System.currentTimeMillis() - start) >= 500) {
          timeoutCount++;
        } else {
          notifiedCount++;
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      waitCount--;
      switch (waitCount % 3) {
        case 0:
          break;
        case 1:
          notify();
          break;
        case 2:
          notifyAll();
      }
    }

    int getWriteCount() {
      return writeCount;
    }

    int getTimeoutCount() {
      return timeoutCount;
    }

    int getNotifyCount() {
      return notifiedCount;
    }

    synchronized int write() {
      int c = ++writeCount;
      for (int i = 0; i < locks.size(); i++) {
        LockObject lock = (LockObject) locks.get(i);
        if (lock != this) {
          c += lock.getWriteCount();
        }
      }
      return c;
    }

    int getID() {
      return lockID;
    }

    @Override
    public String toString() {
      return "LockObject(" + getID() + ")";
    }
  }

}
