/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.builder;


public interface AppGroupConfigBuilder {
  
  void setAppGroupName(String name);

  void setNamedClassLoaders(String namedClassLoaders[]);
  
  void setWebApplications(String webApplications[]);
}
