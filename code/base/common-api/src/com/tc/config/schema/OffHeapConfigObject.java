/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

public class OffHeapConfigObject {

  private final boolean enabled;
  private final String  maxDataSize;

  public OffHeapConfigObject(final boolean enabled, final String maxDataSize) {
    this.enabled = enabled;
    this.maxDataSize = maxDataSize;
  }

  public String getMaxDataSize() {
    return this.maxDataSize;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  @Override
  public String toString() {
    return ("OffHeapConfigObject : Enabled : " + isEnabled() + " Max data size : " + getMaxDataSize());
  }
}
