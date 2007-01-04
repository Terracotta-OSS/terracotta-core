/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import java.util.List;
import java.util.Map;

/**
 * Mixin interface to encapsulate all state information for each <code>BeanFactory</code> instance.
 * 
 * @author Jonas Bon&#233;r
 * @author Eugene Kuleshov
 */
public interface DistributableBeanFactory {

  public static final String PROTOTYPE = "prototype";

  public static final String SINGLETON = "singleton";
  
  
  boolean isClustered();

  String getAppName();

  String getId();

  List getLocations();

  List getSpringConfigHelpers();

  
  // configuration details
  boolean isDistributedEvent(String className);

  boolean isDistributedBean(String beanName);

  boolean isDistributedField(String beanName, String name);

  boolean isDistributedSingleton(String beanName);
  
  boolean isDistributedScoped(String beanName);
  

  // initialization
  void addLocation(String location);
  
  /**
   * Register bean definitions
   * 
   * @param beanMap map of <code>String</code> bean names to <code>AbstractBeanDefinition</code>.
   */
  void registerBeanDefinitions(Map beanMap);
  
  
  // runtime
  
  BeanContainer getBeanContainer(ComplexBeanId beanId);

  BeanContainer putBeanContainer(ComplexBeanId beanId, BeanContainer container);

  BeanContainer removeBeanContainer(ComplexBeanId beanId);
  
  void initializeBean(ComplexBeanId beanId, Object bean, BeanContainer container);

}
