package com.tc.test.config.builder;

/**
 * @author Ludovic Orban
 */
public class OffHeap {

  private boolean enabled;
  private String maxDataSize;

  public OffHeap() {
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
  
  public OffHeap enabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }
  
  public String getMaxDataSize() {
    return maxDataSize;
  }

  public void setMaxDataSize(String maxDataSize) {
    this.maxDataSize = maxDataSize;
  }

  public OffHeap maxDataSize(String maxDataSize) {
    this.maxDataSize = maxDataSize;
    return this;
  }

}
