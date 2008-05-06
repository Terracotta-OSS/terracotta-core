/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.app;

import com.tc.object.config.DSOClientConfigHelper;

/**
 * Creates an application config.
 */
public interface ApplicationConfigBuilder {
  public void visitClassLoaderConfig(DSOClientConfigHelper config);

  public ApplicationConfig newApplicationConfig();
}