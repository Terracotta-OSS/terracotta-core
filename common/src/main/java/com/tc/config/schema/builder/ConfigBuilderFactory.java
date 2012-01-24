/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.builder;


public interface ConfigBuilderFactory {
  public RootConfigBuilder newRootConfigBuilder();

  public InstrumentedClassConfigBuilder newInstrumentedClassConfigBuilder();

  public LockConfigBuilder newWriteAutoLockConfigBuilder();
  
  public LockConfigBuilder newReadAutoLockConfigBuilder();
}
