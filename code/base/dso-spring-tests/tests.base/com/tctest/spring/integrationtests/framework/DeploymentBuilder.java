/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tctest.spring.integrationtests.framework;

import java.util.Map;


public interface DeploymentBuilder {

  public DeploymentBuilder addDirectoryOrJARContainingClass(Class type);
  public DeploymentBuilder addDirectoryOrJARContainingClassOfSelectedVersion(Class type, String[] variantNames);
  public DeploymentBuilder addDirectoryContainingResource(String resource);
  public DeploymentBuilder addResource(String location, String includes, String prefix);

  public DeploymentBuilder addContextParameter(String name, String value);
  public DeploymentBuilder addListener(Class listenerName);
  public DeploymentBuilder addServlet(String name, String mapping, Class servletClass, Map params, boolean loadOnStartup);
  public DeploymentBuilder setDispatcherServlet(String name, String mapping, Class servletClass, Map params, boolean loadOnStartup);
  public DeploymentBuilder addTaglib(String uri, String location);

  public DeploymentBuilder addBeanDefinitionFile(String beanDefinition);

  public DeploymentBuilder addRemoteService(String remoteName, String beanName, Class interfaceType);
  public DeploymentBuilder addRemoteService(Class exporterType, String remoteName, String beanName, Class interfaceType);
  public DeploymentBuilder addRemoteService(String beanName, Class interfaceType);
  public DeploymentBuilder addRemoteServiceBlock(String serviceBlock);

  public Deployment makeDeployment() throws Exception;

  public void setParentApplicationContextRef(String locatorFactorySelector, String parentContextKey);
  
}
