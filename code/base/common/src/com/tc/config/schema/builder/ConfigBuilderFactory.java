/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.builder;


public interface ConfigBuilderFactory {
  public RootConfigBuilder newRootConfigBuilder();

  public InstrumentedClassConfigBuilder newInstrumentedClassConfigBuilder();

  public LockConfigBuilder newWriteAutoLockConfigBuilder();
  
  public LockConfigBuilder newReadAutoLockConfigBuilder();
}
