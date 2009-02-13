/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

/**
 * Unit test for {@link ConfigItemListener}.
 */
public class MockConfigItemListener implements ConfigItemListener {

  private int    numValueChangeds;
  private Object lastOldValue;
  private Object lastNewValue;

  public MockConfigItemListener() {
    reset();
  }

  public void reset() {
    this.numValueChangeds = 0;
    this.lastOldValue = null;
    this.lastNewValue = null;
  }

  public void valueChanged(Object oldValue, Object newValue) {
    ++this.numValueChangeds;
    this.lastOldValue = oldValue;
    this.lastNewValue = newValue;
  }

  public Object getLastNewValue() {
    return lastNewValue;
  }

  public Object getLastOldValue() {
    return lastOldValue;
  }

  public int getNumValueChangeds() {
    return numValueChangeds;
  }

}
