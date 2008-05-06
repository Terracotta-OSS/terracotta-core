/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ContainerSpec {

  private final String testHome;
  private final int    executionCount;
  private final String vmName;
  private final List   jvmOpts;

  public ContainerSpec(String vmName, String testHome, int executionCount, List jvmOpts) {
    this.vmName = vmName;
    this.testHome = testHome;
    this.executionCount = executionCount;
    this.jvmOpts = jvmOpts;
  }

  public ContainerSpec copy() {
    List jvmOptsCopy = new ArrayList();
    jvmOptsCopy.addAll(jvmOpts);
    return new ContainerSpec(vmName, testHome, executionCount, jvmOptsCopy);
  }

  public String getTestHome() {
    return testHome;
  }

  public int getExecutionCount() {
    return executionCount;
  }

  public String getVmName() {
    return vmName;
  }

  public List getJvmOpts() {
    return jvmOpts;
  }

  public String toString() {
    StringBuffer jopts = new StringBuffer();
    for (Iterator i = jvmOpts.iterator(); i.hasNext();) {
      jopts.append((String) i.next());
    }
    return "vmName: " + vmName + ", testHome: " + testHome + ", executionCount: " + executionCount + ", jvmOpts: "
           + jopts.toString();
  }
}
