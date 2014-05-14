/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bytes;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class TCByteBufferFactoryConfigImpl implements TCByteBufferFactoryConfig {

  @Override
  public boolean isDisabled() {
    return !(TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.TC_BYTEBUFFER_POOLING_ENABLED));
  }

  @Override
  public int getPoolMaxBufCount() {
    return (TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.TC_BYTEBUFFER_THREADLOCAL_POOL_MAXCOUNT, 2000));
  }

  @Override
  public int getCommonPoolMaxBufCount() {
    return (TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.TC_BYTEBUFFER_COMMON_POOL_MAXCOUNT, 3000));
  }

}
