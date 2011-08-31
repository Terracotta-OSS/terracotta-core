/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.builder;


public interface AppGroupConfigBuilder {
  
  void setAppGroupName(String name);

  void setNamedClassLoaders(String namedClassLoaders[]);
  
  void setWebApplications(String webApplications[]);
}
