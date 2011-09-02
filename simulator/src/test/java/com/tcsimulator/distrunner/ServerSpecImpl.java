/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator.distrunner;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.simulator.distrunner.ArgParser;

import java.util.ArrayList;
import java.util.List;

public class ServerSpecImpl implements ServerSpec {

  private final String hostname;
  private final String testHome;
  private final int    hashCode;
  private final List   jvmOpts;
  private final int    cache;
  private final int    jmxPort;
  private final int    dsoPort;
  private final int    type;

  public ServerSpecImpl(String hostname, String testHome, int cache, int jmxPort, int dsoPort, List jvmOpts, int type) {
    if (hostname == null) throw new AssertionError();
    if (testHome == null) throw new AssertionError();
    this.hostname = hostname;
    this.testHome = testHome;
    this.cache = cache;
    this.jmxPort = jmxPort;
    this.dsoPort = dsoPort;
    this.jvmOpts = jvmOpts;
    this.type = type;
    this.hashCode = new HashCodeBuilder(17, 37).append(hostname).append(testHome).append(cache).append(jmxPort)
        .append(dsoPort).append(jvmOpts).append(type).toHashCode();
  }

  public boolean isNull() {
    return false;
  }

  public String getHostName() {
    return this.hostname;
  }

  public String getTestHome() {
    return this.testHome;
  }

  public boolean equals(Object o) {
    if (o instanceof ServerSpecImpl) {
      ServerSpecImpl cmp = (ServerSpecImpl) o;
      return hostname.equals(cmp.hostname) && testHome.equals(cmp.testHome) && cache == cmp.cache
             && jmxPort == cmp.jmxPort && dsoPort == cmp.dsoPort && jvmOpts.equals(cmp.jvmOpts);
    }
    return false;
  }

  public int hashCode() {
    return this.hashCode;
  }

  public String toString() {
    return ArgParser.getArgumentForServerSpec(this);
  }

  public List getJvmOpts() {
    return new ArrayList(jvmOpts);
  }

  public int getCache() {
    return cache;
  }

  public int getJmxPort() {
    return jmxPort;
  }

  public int getDsoPort() {
    return dsoPort;
  }

  public ServerSpec copy() {
    return new ServerSpecImpl(hostname, testHome, cache, jmxPort, dsoPort, new ArrayList(jvmOpts), type);
  }

  public int getType() {
    return type;
  }

}
