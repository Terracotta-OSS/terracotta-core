/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

/**
 * A mock {@link ConfigItem}, for use in tests.
 */
public class MockConfigItem implements ConfigItem {

  private int                numGetObjects;
  private Object             returnedObject;

  private int                numAddListeners;
  private int                numRemoveListeners;
  private ConfigItemListener lastListener;

  public MockConfigItem() {
    this(null);
  }

  public MockConfigItem(Object value) {
    this.returnedObject = value;

    reset();
  }

  public void reset() {
    this.numGetObjects = 0;
    this.numAddListeners = 0;
    this.numRemoveListeners = 0;
    this.lastListener = null;
  }

  public Object getObject() {
    ++this.numGetObjects;
    return this.returnedObject;
  }

  public void addListener(ConfigItemListener changeListener) {
    ++this.numAddListeners;
    this.lastListener = changeListener;
  }

  public void removeListener(ConfigItemListener changeListener) {
    ++this.numRemoveListeners;
    this.lastListener = changeListener;
  }

  public ConfigItemListener getLastListener() {
    return lastListener;
  }

  public int getNumGetObjects() {
    return numGetObjects;
  }

  public int getNumAddListeners() {
    return numAddListeners;
  }

  public int getNumRemoveListeners() {
    return numRemoveListeners;
  }

  public void setReturnedObject(Object returnedObject) {
    this.returnedObject = returnedObject;
  }

}
