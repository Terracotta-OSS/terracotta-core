/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

public class ActivePassiveServerCrasher implements Runnable {

  private final ActivePassiveServerManager serverManger;
  private final long                       serverCrashWaitTimeInSec;

  private Object                           lock = new Object();
  private boolean                          testIsRunning;

  public ActivePassiveServerCrasher(ActivePassiveServerManager serverManager, long serverCrashWaitTimeInSec) {
    this.serverManger = serverManager;
    this.serverCrashWaitTimeInSec = serverCrashWaitTimeInSec;
    testIsRunning = true;
  }

  public void run() {
    boolean isRunning;
    while (true) {
      synchronized (lock) {
        isRunning = testIsRunning;
      }
      if (isRunning && serverManger.getErrors().size() == 0) {
        try {
          Thread.sleep(serverCrashWaitTimeInSec * 1000);
          serverManger.crashActive();
        } catch (Exception e) {
          serverManger.storeErrors(e);
        }
      } else {
        break;
      }
      synchronized (lock) {
        isRunning = testIsRunning;
      }
      if (isRunning && serverManger.getErrors().size() == 0) {
        try {
          serverManger.restartLastCrashedServer();
        } catch (Exception e) {
          serverManger.storeErrors(e);
        }
      } else {
        break;
      }
    }
  }

  public void stop() {
    synchronized (lock) {
      testIsRunning = false;
    }
  }
}
