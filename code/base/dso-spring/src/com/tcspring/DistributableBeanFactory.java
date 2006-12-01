/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import com.tc.object.TCClass;

import java.util.List;
import java.util.Map;

/**
 * Mixin interface to encapsulate all state information for each <code>BeanFactory</code> instance.
 * 
 * @author Jonas Bon&#233;r
 * @author Eugene Kuleshov
 */
public interface DistributableBeanFactory {

  boolean isClustered();

  String getAppName();

  String getId();

  List getLocations();

  List getSpringConfigHelpers();

  // initialization
  void addLocation(String location);

  void registerBeanDefinitions(Map beanMap);

  // configuration details
  boolean isDistributedEvent(String className);

  boolean isDistributedBean(String beanName);

  boolean isDistributedField(String beanName, String name);

  Object getBeanFromSingletonCache(Object beanId);

  Object removeBeanFromSingletonCache(Object beanId);

  Object virtualizeSingletonBean(Object beanId, Object localInstance);
  
  boolean isDistributed(Object bean);

  void copyTransientFields(String beanName, Object sourceBean, Object targetBean, 
                           Class targetClass, TCClass tcClass) throws IllegalAccessException;

}
