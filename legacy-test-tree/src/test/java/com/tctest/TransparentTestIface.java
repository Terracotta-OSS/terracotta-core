/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tctest.runner.DistributedTestRunnerConfig;
import com.tctest.runner.TransparentAppConfig;

public interface TransparentTestIface {

  TransparentAppConfig getTransparentAppConfig();

  void initializeTestRunner() throws Exception;

  void initializeTestRunner(boolean isMutateValidateTest) throws Exception;

  DistributedTestRunnerConfig getRunnerConfig();

}
