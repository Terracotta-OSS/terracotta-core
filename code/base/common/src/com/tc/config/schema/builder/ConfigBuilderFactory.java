/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema.builder;


public interface ConfigBuilderFactory {
  public RootConfigBuilder newRootConfigBuilder();

  public InstrumentedClassConfigBuilder newInstrumentedClassConfigBuilder();

  public LockConfigBuilder newWriteAutoLockConfigBuilder();
  
  public LockConfigBuilder newReadAutoLockConfigBuilder();
}
