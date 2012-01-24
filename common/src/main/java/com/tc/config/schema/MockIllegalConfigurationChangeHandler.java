/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.ConfigItem;

/**
 * A mock {@link IllegalConfigurationChangeHandler}, for use in tests.
 */
public class MockIllegalConfigurationChangeHandler implements IllegalConfigurationChangeHandler {

  private int        numChangeFaileds;
  private ConfigItem lastItem;
  private Object     lastOldValue;
  private Object     lastNewValue;

  public MockIllegalConfigurationChangeHandler() {
    reset();
  }

  public void reset() {
    this.numChangeFaileds = 0;
    this.lastItem = null;
    this.lastOldValue = null;
    this.lastNewValue = null;
  }

  public void changeFailed(ConfigItem item, Object oldValue, Object newValue) {
    ++this.numChangeFaileds;
    this.lastItem = item;
    this.lastOldValue = oldValue;
    this.lastNewValue = newValue;
  }

  public ConfigItem getLastItem() {
    return lastItem;
  }

  public Object getLastNewValue() {
    return lastNewValue;
  }

  public Object getLastOldValue() {
    return lastOldValue;
  }

  public int getNumChangeFaileds() {
    return numChangeFaileds;
  }

}
