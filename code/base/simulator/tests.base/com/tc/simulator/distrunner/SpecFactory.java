/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.distrunner;

import com.tcsimulator.ClientSpec;
import com.tcsimulator.distrunner.ServerSpec;

import java.util.List;

public interface SpecFactory {

  public ClientSpec newClientSpec(String hostname, String testHome, int vmCount, int executionCount, List jvmOpts);

  public ServerSpec newServerSpec(String host, String path, int cache, int jmxPort, int dsoPort, List jvmOpts, int type);
}
