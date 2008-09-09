/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.MultipleServerManager;
import com.tc.test.ProcessInfo;
import com.tc.test.TestConfigObject;
import com.tctest.runner.DistributedTestRunner;
import com.tctest.runner.DistributedTestRunnerConfig;
import com.tctest.runner.TransparentAppConfig;

public abstract class MultipleServersTransparentTestBase extends TransparentTestBase {

  /**
   * The server manager which currently takes care of active-passive and active-active tests
   */
  protected MultipleServerManager multipleServerManager;

  public void initializeTestRunner(boolean isMutateValidateTest, TransparentAppConfig transparentAppCfg,
                                   DistributedTestRunnerConfig runnerCfg) throws Exception {
    if (!isMultipleServerTest()) {
      super.initializeTestRunner(isMutateValidateTest, transparentAppCfg, runnerCfg);
      return;
    }
    runner = new DistributedTestRunner(runnerCfg, configFactory(), configHelper(), getApplicationClass(),
                                       getOptionalAttributes(), getApplicationConfigBuilder().newApplicationConfig(),
                                       false, isMutateValidateTest, isMultipleServerTest(), multipleServerManager,
                                       transparentAppCfg);
  }

  protected boolean canRun() {
    return super.canRun() || (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE))
           || (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_ACTIVE));
  }

  public final boolean isMultipleServerTest() {
    return TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE.equals(mode())
           || TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_ACTIVE.equals(mode());
  }

  public void test() throws Exception {
    if (isMultipleServerTest()) runMultipleServersTest();
    super.test();
  }

  protected abstract void runMultipleServersTest() throws Exception;

  protected void dumpServers() throws Exception {
    if (multipleServerManager != null) {
      multipleServerManager.dumpAllServers(pid, getThreadDumpCount(), getThreadDumpInterval());
      pid = multipleServerManager.getPid();
    }
    super.dumpServers();
  }

  protected void tearDown() throws Exception {
    if (controlledCrashMode && isMultipleServerTest()) {
      System.out.println("Currently running java processes: " + ProcessInfo.ps_grep_java());
      multipleServerManager.stopAllServers();
    }
    super.tearDown();
  }
}
