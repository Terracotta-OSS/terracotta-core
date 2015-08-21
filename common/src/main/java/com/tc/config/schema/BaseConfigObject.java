/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.util.Assert;

/**
 * A base class for all new config objects.
 */
public class BaseConfigObject implements Config {

  protected final ConfigContext context;

  public BaseConfigObject(ConfigContext context) {
    Assert.assertNotNull(context);
    this.context = context;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " around bean:\n" + context.bean();
  }

  @Override
  public Object getBean() {
    return this.context.bean();
  }
  
}
