/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bytes;

public interface TCByteBufferFactoryConfig {
  boolean isDisabled();

  int getPoolMaxBufCount();

  int getCommonPoolMaxBufCount();
}
