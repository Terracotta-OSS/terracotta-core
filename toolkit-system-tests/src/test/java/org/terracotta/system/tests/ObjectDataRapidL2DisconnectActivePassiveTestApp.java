/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.util.concurrent.ThreadUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;

import junit.framework.Assert;

/**
 * DEV-4047: In the AA world, an Object Lookup can happen before the Object actually gets create at the server. Server
 * shouldn't crash on those scenarios
 */
public class ObjectDataRapidL2DisconnectActivePassiveTestApp extends ClientBase {

  private static final int    SIZE        = 10000;
  private static final long   TIME_TO_RUN = 10 * 60 * 1000;

  private final List<Integer> array;
  private final ReadWriteLock lock;
  private final AtomicBoolean passed      = new AtomicBoolean(true);

  public ObjectDataRapidL2DisconnectActivePassiveTestApp(String[] args) {
    super(args);
    this.array = getClusteringToolkit().getList("test list", null);
    this.lock = getClusteringToolkit().getReadWriteLock("test lock");
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    startPassiveCrasherThread();
    final long startTime = System.currentTimeMillis();
    lock.writeLock().lock();
    try {
      for (int j = 0; j < SIZE; j++) {
        array.add(new Integer(j));

        if (j != 0 && j % 100 == 0) {
          System.out.println("Loop count : " + j);
          ThreadUtil.reallySleep(1000);
        }
      }
    } finally {
      lock.writeLock().unlock();
    }

    // spin cpu so that stage threads for L2ObjectSyncSendHandler gets slow
    // look at DEV-6499 for more details
    while (System.currentTimeMillis() - startTime < TIME_TO_RUN) {
      //
    }

    Assert.assertTrue(passed.get());
  }

  private void startPassiveCrasherThread() {
    Thread th = new Thread(new Runnable() {

      @Override
      public void run() {
        while (true) {
          try {
            Thread.sleep(5 * 1000);
            getTestControlMbean().crashAllPassiveServers(0);
            Thread.sleep(5 * 1000);
            getTestControlMbean().reastartLastCrashedServer(0);
          } catch (Exception e) {
            e.printStackTrace();
            passed.set(false);
          }
        }

      }
    }, "passive crasher thread");

    th.start();

  }
}
