/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.distrunner;

import com.tcsimulator.ClientSpec;
import com.tcsimulator.ClientSpecImpl;
import com.tcsimulator.distrunner.ServerSpec;
import com.tcsimulator.distrunner.ServerSpecImpl;

import java.util.List;

public class SpecFactoryImpl implements SpecFactory {

  public ClientSpec newClientSpec(String hostname, String testHome, int vmCount, int executionCount, List jvmOpts) {
    return new ClientSpecImpl(hostname, testHome, vmCount, executionCount, jvmOpts);
  }

  public ServerSpec newServerSpec(String host, String path, int cache, int jmxPort, int dsoPort, List jvmOpts, int type) {
    return new ServerSpecImpl(host, path, cache, jmxPort, dsoPort, jvmOpts, type);
  }
}
