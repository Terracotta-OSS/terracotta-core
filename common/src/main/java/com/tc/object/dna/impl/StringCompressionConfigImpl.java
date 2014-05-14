/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class StringCompressionConfigImpl implements StringCompressionConfig {

  @Override
  public boolean enabled() {
    return TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_ENABLED);
  }

  @Override
  public boolean loggingEnabled() {
    return TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_LOGGING_ENABLED);
  }

  @Override
  public int minSize() {
    return TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_MINSIZE);
  }

}
