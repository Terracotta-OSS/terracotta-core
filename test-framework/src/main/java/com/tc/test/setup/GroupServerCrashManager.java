/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.test.setup;

import com.tc.test.config.model.CrashConfig;
import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class GroupServerCrashManager implements Runnable {
  private final GroupServerManager serverManager;
  private int                      crashCount    = 0;
  private volatile boolean         done;
  private final List<Throwable>    errors;
  private final TestConfig         testConfig;
  private final SimpleDateFormat   dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");

  GroupServerCrashManager(TestConfig testConfig, GroupServerManager groupServerManager) throws Exception {
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
    try {
      // Precondition for the loop
      serverManager.waituntilEveryPassiveStandBy();

      while (!done) {
        sleep(getCrashConfig().getServerCrashWaitTimeInSec() * 1000);

        if ((getCrashConfig().getMaxCrashCount() > crashCount) && !done) {
          switch (getCrashConfig().getCrashMode()) {

            case RANDOM_ACTIVE_CRASH:
              debug("about to crash active server");
              serverManager.crashActiveAndWaitForPassiveToTakeOver();
              break;
            case RANDOM_SERVER_CRASH:
              debug("about to crash server");
              serverManager.crashRandomServer();
              break;
            default:
              throw new AssertionError("Unsupported crash mode: " + getCrashConfig().getCrashMode());
          }
          debug("about to restart last crashed server");
          // Re-establish a stable state prior to next iteration
          serverManager.restartLastCrashedServer();
          serverManager.waituntilEveryPassiveStandBy();

          crashCount++;
        }
      }
    } catch (Exception e) {
      debug("Error occured while crashing/restarting server");
      errors.add(e);
      e.printStackTrace();

    }

    debug("ServerCrasher is done: errors[" + errors.size() + "] crashCount[" + crashCount + "]");
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

  void stop() {
    this.done = true;
    debug("Stopping crasher");
  }

  private void debug(String msg) {
    System.out.println("[****** ServerCrasher : " + dateFormatter.format(new Date()) + " '"
                       + Thread.currentThread().getName() + "' '" + serverManager.getGroupData().getGroupName() + "'] "
                       + msg);
  }
}
