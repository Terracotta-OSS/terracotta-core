/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import java.util.List;
import java.util.Map;

public interface DSOSpringConfigHelper {

  String getRootName();
  
  boolean isLocationInfoEnabled();
  
  // accessors
  boolean isMatchingApplication(String applicationName);

  boolean isMatchingConfig(String configPath);

  boolean isDistributedEvent(String className);

  boolean isDistributedBean(String beanName);

  boolean isDistributedField(String beanName, String fieldName);

  boolean isFastProxyEnabled();

  /**
   * Returns <code>Map</code> of <code>String</code> bean name to <code>Set</code> of the excluded fields.
   */
  Map getDistributedBeans();

  /**
   * Returns <code>List</code> of <code>String</code> expressions for distributed event types.
   */
  List getDistributedEvents();

  // mutators
  void addApplicationNamePattern(String pattern);

  void addConfigPattern(String pattern);

  void addDistributedEvent(String expression);

  void addBean(String name);

  void excludeField(String beanName, String fieldName);

  void setFastProxyEnabled(boolean b);

  void setRootName(String rootName);

  void setLocationInfoEnabled(boolean locationInfoEnabled);
}
