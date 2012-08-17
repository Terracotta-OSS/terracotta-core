/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;

import junit.framework.Assert;

public class ClientGCLockHeldClient extends ClientBase {

  private static final int MINUTES_TEST_RUN = 10;

  private Lock             lockObj;
  private final Random     random           = new Random();

  public ClientGCLockHeldClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit) throws Exception {
    lockObj = toolkit.getReadWriteLock("testlock").writeLock();
    getBarrierForAllClients().await();
    List list = toolkit.getList("testList", null);
    Lock listLock = toolkit.getReadWriteLock("listlock").writeLock();
    // start a thread that holds a lock forever.
    RunForeverThread thread = new RunForeverThread();
    thread.start();

    Stopwatch stopwatch = new Stopwatch().start();
    while (stopwatch.getElapsedTime() < (1000 * 60 * MINUTES_TEST_RUN)) {
      String randomString = getRandomString();
      listLock.lock();
      try {
        list.add(randomString);
      } finally {
        listLock.unlock();
      }

      listLock.lock();
      try {
        list.remove(randomString);
      } finally {
        listLock.unlock();
      }
    }
    getBarrierForAllClients().await();
    Assert.assertTrue(list.size() == 0);
  }

  private String getRandomString() {
    return "random" + random.nextLong();
  }

  static DateFormat formatter = new SimpleDateFormat("hh:mm:ss,S");

  private static void log(String message) {
    System.err.println(Thread.currentThread().getName() + " :: "
                       + formatter.format(new Date(System.currentTimeMillis())) + " : " + message);
  }

  private class RunForeverThread extends Thread {
    public void holdLock() {
      synchronized (lockObj) {
        try {
          Thread.currentThread().join();
          log("should not reach here, this thread should hold the lock forever");
          throw new AssertionError("should not reach here, this thread should hold the lock forever");
        } catch (InterruptedException e) {
          log("error occurred holding lock");
          throw new AssertionError(e);
        }
      }
    }

    @Override
    public void run() {
      holdLock();
    }

  }

  private static class Stopwatch {
    private long       startTime = -1;
    private final long stopTime  = -1;
    private boolean    running   = false;

    public Stopwatch start() {
      startTime = System.currentTimeMillis();
      running = true;
      return this;
    }

    public long getElapsedTime() {
      if (startTime == -1) { return 0; }
      if (running) {
        return System.currentTimeMillis() - startTime;
      } else {
        return stopTime - startTime;
      }
    }

  }

}
