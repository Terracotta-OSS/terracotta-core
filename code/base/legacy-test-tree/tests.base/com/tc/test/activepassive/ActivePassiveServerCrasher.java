/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

public class ActivePassiveServerCrasher implements Runnable {

  private final ActivePassiveServerManager serverManger;
  private final long                       serverCrashWaitTimeInSec;

  private SynchronizedBoolean              testIsRunning;

  public ActivePassiveServerCrasher(ActivePassiveServerManager serverManager, long serverCrashWaitTimeInSec) {
    this.serverManger = serverManager;
    this.serverCrashWaitTimeInSec = serverCrashWaitTimeInSec;
    testIsRunning = new SynchronizedBoolean(true);
  }

  public void run() {
    while (testIsRunning.get()) {
      try {
        Thread.sleep(serverCrashWaitTimeInSec * 1000);
        serverManger.crashActive();
        serverManger.restartLastCrashedServer();
      } catch (Exception e) {
        serverManger.storeErrors(e);
      }
    }
  }

  public void stop() {
    testIsRunning.set(false);
  }
}
