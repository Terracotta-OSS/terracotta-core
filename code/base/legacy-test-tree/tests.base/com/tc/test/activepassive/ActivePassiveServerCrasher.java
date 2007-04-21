/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

public class ActivePassiveServerCrasher implements Runnable {
  private static boolean                   DEBUG      = false;
  private final ActivePassiveServerManager serverManger;
  private final long                       serverCrashWaitTimeInSec;
  private final int                        maxCrashCount;

  private Object                           lock       = new Object();
  private boolean                          testIsRunning;
  private int                              crashCount = 0;

  public ActivePassiveServerCrasher(ActivePassiveServerManager serverManager, long serverCrashWaitTimeInSec,
                                    int maxCrashCount) {
    this.serverManger = serverManager;
    this.serverCrashWaitTimeInSec = serverCrashWaitTimeInSec;
    this.maxCrashCount = maxCrashCount;
    testIsRunning = true;
  }

  private boolean shouldRun() {
    synchronized (lock) {
      debugPrintln("maxCrashCount=[" + maxCrashCount + "] crashCount=[" + crashCount + "] testIsRunning=["
                   + testIsRunning + "] errors=[" + serverManger.getErrors().size() + "]");
      if ((maxCrashCount - crashCount) > 0 && testIsRunning && serverManger.getErrors().size() == 0) { return true; }
    }
    return false;
  }

  public void run() {
    while (true) {
      if (shouldRun()) {
        try {
          Thread.sleep(serverCrashWaitTimeInSec * 1000);

          debugPrintln("***** ActivePassiveServerCrasher:  about to crash active  threadID=["
                       + Thread.currentThread().getName() + "]");

          serverManger.crashActive();
        } catch (Exception e) {
          debugPrintln("***** ActivePassiveServerCrasher:  error occured while crashing active  threadID=["
                       + Thread.currentThread().getName() + "]");

          e.printStackTrace();

          serverManger.storeErrors(e);
        }
      } else {
        debugPrintln("***** ActivePassiveServerCrasher:  break 1");
        break;
      }
      if (shouldRun()) {
        try {
          debugPrintln("***** ActivePassiveServerCrasher:  about to restart crashed server threadID=["
                       + Thread.currentThread().getName() + "]");

          serverManger.restartLastCrashedServer();
          crashCount++;
        } catch (Exception e) {
          debugPrintln("***** ActivePassiveServerCrasher:  error occured while restarting crashed server  threadID=["
                       + Thread.currentThread().getName() + "]");

          serverManger.storeErrors(e);
        }
      } else {
        debugPrintln("***** ActivePassiveServerCrasher:  break 2");
        break;
      }
    }
  }

  public void stop() {
    synchronized (lock) {
      testIsRunning = false;
    }
  }

  public void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }
}
