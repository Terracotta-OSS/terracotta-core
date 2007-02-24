/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.object.config.DSOClientConfigHelper;
import com.tctest.runner.DistributedTestRunnerConfig;
import com.tctest.runner.TransparentAppConfig;

public interface TransparentTestIface {

  public TransparentAppConfig getTransparentAppConfig();

  public void initializeTestRunner() throws Exception;

  public TVSConfigurationSetupManagerFactory getConfigFactory();

  public DSOClientConfigHelper getConfigHelper();

  public DistributedTestRunnerConfig getRunnerConfig();

  
}
