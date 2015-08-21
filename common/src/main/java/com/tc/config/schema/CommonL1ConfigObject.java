/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;


import com.tc.config.schema.context.ConfigContext;
import com.tc.util.Assert;

import java.io.File;

/**
 * The standard implementation of {@link CommonL1Config}.
 */
public class CommonL1ConfigObject implements CommonL1Config {

  public CommonL1ConfigObject() {
  }

  @Override
  public File logsPath() {
    //TODO fix this, client path is now null clients should have their own configuration
    return null;
  }

  @Override
  public Object getBean() {
    //TODO getting bean returns null
    return null;
  }
}
