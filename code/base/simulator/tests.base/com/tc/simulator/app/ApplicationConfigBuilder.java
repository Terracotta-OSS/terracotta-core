/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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