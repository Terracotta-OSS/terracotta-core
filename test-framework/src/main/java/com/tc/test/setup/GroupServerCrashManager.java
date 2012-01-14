/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.setup;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import com.tc.test.config.model.CrashConfig;
import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;

public class GroupServerCrashManager implements Runnable {
  private static boolean           DEBUG      = true;
  private final GroupServerManager serverManager;
  private int                      crashCount = 0;
  private volatile boolean         done;
  private final List<Throwable>    errors;
  private final TestConfig         testConfig;
  private final long               seed;

  public GroupServerCrashManager(TestConfig testConfig, GroupServerManager groupServerManager) throws Exception {
    this.serverManager = groupServerManager;
    this.testConfig = testConfig;
    this.errors = new ArrayList<Throwable>();
    SecureRandom srandom = SecureRandom.getInstance("SHA1PRNG");
    seed = srandom.nextLong();
    System.out.println("***** Random number generator seed=[" + seed + "]");

  }

  public void run() {
    if (getCrashConfig().getCrashMode().equals(ServerCrashMode.NO_CRASH)) {
      // Nothing to be done break
      return;
    }
    while (!done) {
      try {
        Thread.sleep(getCrashConfig().getServerCrashWaitTimeInSec() * 1000);
      } catch (InterruptedException e) {
        errors.add(e);
      }

      if ((getCrashConfig().getMaxCrashCount() > crashCount) && !done) {
        try {

          if (getCrashConfig().getCrashMode().equals(ServerCrashMode.RANDOM_ACTIVE_CRASH)) {
            debugPrintln("***** ServerCrasher:  about to crash active servers in all groups  threadID=["
                         + Thread.currentThread().getName() + "]");
            serverManager.crashActiveAndWaitForPassiveToTakeOver();
            debugPrintln("***** ServerCrasher:  about to restart last crashed servers in all groups threadID=["
                         + Thread.currentThread().getName() + "]");
            serverManager.restartLastCrashedServer();
          } else if (getCrashConfig().getCrashMode().equals(ServerCrashMode.RANDOM_SERVER_CRASH)) {
            debugPrintln("***** ServerCrasher:  about to crash server  threadID=[" + Thread.currentThread().getName()
                         + "]");
            serverManager.crashRandomServer();
            debugPrintln("***** ServerCrasher:  about to restart crashed server threadID=["
                         + Thread.currentThread().getName() + "]");
            serverManager.restartLastCrashedServer();
          }

          crashCount++;
        } catch (Exception e) {
          debugPrintln("***** ServerCrasher:  error occured while crashing/restarting server  threadID=["
                       + Thread.currentThread().getName() + "]");

          e.printStackTrace();

          break;
        }
      } else {
        debugPrintln("***** ServerCrasher is done: errors[" + errors.size() + "] crashCount[" + crashCount + "]");
        break;
      }
    }
  }

  private CrashConfig getCrashConfig() {
    return testConfig.getCrashConfig();
  }

  public void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  public synchronized void stop() {
    this.done = true;
  }

}
