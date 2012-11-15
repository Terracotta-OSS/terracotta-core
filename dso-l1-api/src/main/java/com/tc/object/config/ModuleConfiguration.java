/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

public interface ModuleConfiguration {

  void addWriteAutolock(String expression);

  void addReadAutolock(String expression);

  void addDistributedMethod(String expression);

  public boolean addTunneledMBeanDomain(String tunneledMBeanDomain);
}
