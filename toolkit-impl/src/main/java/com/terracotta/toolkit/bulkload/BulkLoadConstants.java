/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.bulkload;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;

public final class BulkLoadConstants {

  private final TCProperties  tcProperties;

  public BulkLoadConstants(TCProperties tcProperties) {
    this.tcProperties = tcProperties;
  }

  public boolean isLoggingEnabled() {
    return tcProperties.getBoolean(TCPropertiesConsts.TOOLKIT_BULKLOAD_LOGGING_ENABLED);
     
  }

  public int getBatchedPutsBatchBytes() {
    return tcProperties.getInt(TCPropertiesConsts.TOOLKIT_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE);
  }

  public long getBatchedPutsBatchTimeMillis() {
    return tcProperties.getLong(TCPropertiesConsts.TOOLKIT_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS);
  }

  public int getBatchedPutsThrottlePutsAtByteSize() {
    return tcProperties.getInt(TCPropertiesConsts.TOOLKIT_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE);
  }

}
