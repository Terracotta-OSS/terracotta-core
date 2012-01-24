/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.builder;



public interface DSOApplicationConfigBuilder {

  public void setInstrumentedClasses(String value);

  public void setInstrumentedClasses(InstrumentedClassConfigBuilder[] value);

  public void setDistributedMethods(String value);

  public void setDistributedMethods(String[] value);

  public void setAdditionalBootJarClasses(String value);

  public void setAdditionalBootJarClasses(String[] value);
  
  public void setAppGroups(AppGroupConfigBuilder[] value);

  public void setRoots(String value);

  public void setRoots(RootConfigBuilder[] value);

  public void setTransientFields(String value);

  public void setTransientFields(String[] value);

  public void setLocks(String value);

  public void setLocks(LockConfigBuilder[] value);
  
  public void setWebApplications(WebApplicationConfigBuilder[] value);

}
