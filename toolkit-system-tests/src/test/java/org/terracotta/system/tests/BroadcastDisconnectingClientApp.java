/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;

public class BroadcastDisconnectingClientApp extends ClientBase {

  private final long       timeout;
  private static final int LIST_MAX = 250;
  private final Random     random   = new Random();

  public BroadcastDisconnectingClientApp(String[] args) {
    super(args);
    this.timeout = Integer.getInteger("longDuration");
  }

  @Override
  public void test(Toolkit toolkit) throws Exception {
    List list = toolkit.getList("testList", null);
    ReadWriteLock lock = toolkit.getReadWriteLock("testLock");
    log("Starting client with duration " + timeout);

    BroadcastDisconnectingClientApp.Stopwatch stopwatch = new Stopwatch().start();
    while (stopwatch.getElapsedTime() < timeout) {

      lock.writeLock().lock();
      try {
        if (list.size() >= LIST_MAX) {
          list.remove(0);
        }
        list.add(getDataString());
      } finally {
        lock.writeLock().unlock();
      }
    }
    log("Client completed");
  }

  private String getDataString() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 100; i++) {
      sb.append(random.nextLong());
    }
    return sb.toString();
  }

  static DateFormat formatter = new SimpleDateFormat("hh:mm:ss,S");

  private static void log(String message) {
    System.err.println(Thread.currentThread().getName() + " :: "
                       + formatter.format(new Date(System.currentTimeMillis())) + " : " + message);
  }

  private static class Stopwatch {
    private long       startTime = -1;
    private final long stopTime  = -1;
    private boolean    running   = false;

    public BroadcastDisconnectingClientApp.Stopwatch start() {
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