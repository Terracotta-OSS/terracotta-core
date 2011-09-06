/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.config.schema.builder.DSOApplicationConfigBuilder;

/**
 * Interface for the DSO Application section of the config file. This is for building config files programmatically.
 */
public interface DSOApplicationConfig {
  // HACK: is also in IStandardDSOClientConfigHelper
  public void addRoot(String rootName, String rootFieldName);

  // HACK: is also in IStandardDSOClientConfigHelper
  public void addIncludePattern(String classPattern);

  // HACK: is also in IStandardDSOClientConfigHelper
  public void addWriteAutolock(String methodPattern);
  
  // HACK: is also in IStandardDSOClientConfigHelper
  public void addReadAutolock(String methodPattern);

  // HACK: is also in IStandardDSOClientConfigHelper
  public void addIncludePattern(String classname, boolean honorTransient);

  public void writeTo(DSOApplicationConfigBuilder appConfigBuilder);
}
