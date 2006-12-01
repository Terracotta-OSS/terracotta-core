/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.config.schema.builder.DSOApplicationConfigBuilder;

/**
 * Interface for the DSO Application section of the config file. This is for building config files programmatically.
 */
public interface DSOApplicationConfig {
  public void addRoot(String rootName, String rootFieldName);

  public void addIncludePattern(String classPattern);

  public void addWriteAutolock(String methodPattern);
  
  public void addReadAutolock(String methodPattern);

  public void addIncludePattern(String classname, boolean honorTransient);

  public void writeTo(DSOApplicationConfigBuilder appConfigBuilder);
}
