/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

import com.tc.config.schema.builder.ConfigBuilderFactory;
import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;

public class ConfigBuilderFactoryImpl implements ConfigBuilderFactory {

  public RootConfigBuilder newRootConfigBuilder() {
    RootConfigBuilder rv = new RootConfigBuilderImpl();
    return rv;
  }

  public InstrumentedClassConfigBuilder newInstrumentedClassConfigBuilder() {
    return new InstrumentedClassConfigBuilderImpl();
  }

  public LockConfigBuilder newWriteAutoLockConfigBuilder() {
    LockConfigBuilder builder =  new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    builder.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
    return builder;
  }

  public LockConfigBuilder newReadAutoLockConfigBuilder() {
    LockConfigBuilder builder =  new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    builder.setLockLevel(LockConfigBuilder.LEVEL_READ);
    return builder;
  }

}
