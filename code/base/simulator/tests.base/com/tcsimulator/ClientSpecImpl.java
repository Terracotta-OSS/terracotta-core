/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.simulator.distrunner.ArgParser;

import java.util.ArrayList;
import java.util.List;

public class ClientSpecImpl implements ClientSpec {

  private final int    hashCode;
  private final String hostname;
  private final String testHome;
  private final int    vmCount;
  private final int    executionCount;
  private final List   jvmOpts;

  public ClientSpecImpl(String hostname, String testHome, int vmCount, int executionCount, List jvmOpts) {
    if (hostname == null) throw new AssertionError();
    if (testHome == null) throw new AssertionError();
    this.hostname = hostname;
    this.testHome = testHome;
    this.vmCount = vmCount;
    this.executionCount = executionCount;
    this.jvmOpts = jvmOpts;
    this.hashCode = new HashCodeBuilder(17, 37).append(hostname).append(testHome).append(vmCount)
        .append(executionCount).append(jvmOpts).toHashCode();
  }

  public String getHostName() {
    return hostname;
  }

  public String getTestHome() {
    return testHome;
  }

  public int getVMCount() {
    return vmCount;
  }

  public int getExecutionCount() {
    return executionCount;
  }

  public List getJvmOpts() {
    return new ArrayList(jvmOpts);
  }

  public boolean equals(Object o) {
    if (o instanceof ClientSpecImpl) {
      ClientSpecImpl cmp = (ClientSpecImpl) o;
      return hostname.equals(cmp.hostname) && testHome.equals(cmp.testHome) && vmCount == cmp.vmCount
             && executionCount == cmp.executionCount && jvmOpts.equals(cmp.jvmOpts);
    }
    return false;
  }

  public int hashCode() {
    return hashCode;
  }

  public String toString() {
    return ArgParser.getArgumentForClientSpec(this);
  }

  public ClientSpec copy() {
    return new ClientSpecImpl(this.hostname, this.testHome, this.vmCount, this.executionCount, new ArrayList(jvmOpts));
  }
}
