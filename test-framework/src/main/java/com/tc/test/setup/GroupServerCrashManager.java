/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.setup;

import com.tc.test.config.model.CrashConfig;
import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GroupServerCrashManager implements Runnable {
  private final GroupServerManager serverManager;
  private int                      crashCount    = 0;
  private volatile boolean         done;
  private final List<Throwable>    errors;
  private final TestConfig         testConfig;
  private final SimpleDateFormat   dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");

  public GroupServerCrashManager(TestConfig testConfig, GroupServerManager groupServerManager) throws Exception {
    this.serverManager = groupServerManager;
    this.testConfig = testConfig;
    this.errors = new ArrayList<Throwable>();
  }

  @Override
  public void run() {
    if (getCrashConfig().getCrashMode().equals(ServerCrashMode.NO_CRASH)) {
      // Nothing to be done break
      return;
    }

    long delayInSeconds = getCrashConfig().getInitialDelayInSeconds();
    if (delayInSeconds > 0) {
      debug("Sleeping for initial delay seconds before starting to crash servers - " + delayInSeconds);
      sleep(delayInSeconds * 1000);
    }

    while (!done) {
      sleep(getCrashConfig().getServerCrashWaitTimeInSec() * 1000);

      if ((getCrashConfig().getMaxCrashCount() > crashCount) && !done) {
        try {

          if (getCrashConfig().getCrashMode().equals(ServerCrashMode.RANDOM_ACTIVE_CRASH)) {
            debug("about to crash active server");
            serverManager.crashActiveAndWaitForPassiveToTakeOver();
            debug("about to restart last crashed server");
            serverManager.restartLastCrashedServer();
          } else if (getCrashConfig().getCrashMode().equals(ServerCrashMode.RANDOM_SERVER_CRASH)) {
            debug("about to crash server");
            serverManager.crashRandomServer();
            debug("about to restart last crashed server");
            serverManager.restartLastCrashedServer();
          }

          crashCount++;
        } catch (Exception e) {
          debug("Error occured while crashing/restarting server");

          e.printStackTrace();

          break;
        }
      } else {
        debug("ServerCrasher is done: errors[" + errors.size() + "] crashCount[" + crashCount + "]");
        break;
      }
    }
  }

  private void sleep(long timeMillis) {
    try {
      Thread.sleep(timeMillis);
    } catch (InterruptedException e) {
      errors.add(e);
    }
  }

  private CrashConfig getCrashConfig() {
    return testConfig.getCrashConfig();
  }

  public void stop() {
    this.done = true;
    debug("Stopping crasher");
  }

  public void debug(String msg) {
    System.out.println("[****** ServerCrasher : " + dateFormatter.format(new Date()) + " '"
                       + Thread.currentThread().getName() + "' '" + serverManager.getGroupData().getGroupName() + "'] "
                       + msg);
  }
}
