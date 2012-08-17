/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;

import com.tc.test.jmx.TestHandlerMBean;

import java.util.concurrent.TimeUnit;

public class ServerCrasher implements Runnable {

  private final long             crashAtSystemNanos;
  private final TestHandlerMBean testBean;
  private Thread                 thread;
  private volatile boolean       success = false;

  public static ServerCrasher crashServerAfterMillis(TestHandlerMBean testBean, long crashAfterMillis) {
    ServerCrasher serverCrasher = new ServerCrasher(testBean, crashAfterMillis);
    serverCrasher.startCrasherThread();
    return serverCrasher;
  }

  private ServerCrasher(TestHandlerMBean testBean, long crashAfterMillis) {
    this.testBean = testBean;
    crashAtSystemNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(crashAfterMillis);
  }

  public void join() throws InterruptedException {
    thread.join();
  }

  public boolean isSuccess() {
    return success;
  }

  private Thread startCrasherThread() {
    thread = new Thread(this, "crash server at system nanos: " + this.crashAtSystemNanos + " thread");
    thread.start();
    return thread;
  }

  @Override
  public void run() {
    long remainingMillis = getRemainingMillis();
    while (remainingMillis > 0) {
      try {
        ClientBase.debug("[server crasher] sleeping for " + remainingMillis + " millis...");
        Thread.sleep(remainingMillis);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      remainingMillis = getRemainingMillis();
    }

    ClientBase.debug("[server crasher] crashing active server...");
    try {
      testBean.crashActiveAndWaitForPassiveToTakeOver(0);
      ClientBase.debug("[server crasher] crashed active server.");
      success = true;
    } catch (Exception e) {
      ClientBase.debug("[server crasher] FAILED to crash active server.");
      e.printStackTrace();
    }
  }

  private long getRemainingMillis() {
    return Math.max(0, TimeUnit.NANOSECONDS.toMillis(crashAtSystemNanos - System.nanoTime()));
  }

}