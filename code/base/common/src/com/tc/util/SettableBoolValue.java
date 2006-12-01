/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

/**
 * a thin wrapper around SettableValue to help with [un]boxing boolean values
 */
public class SettableBoolValue extends SettableValue {

  public SettableBoolValue() {
    super();
  }

  public void setBool(boolean value) {
    super.set(new Boolean(value));
  }

  /**
   * @return set value; false otherwise
   */
  public boolean boolValue() {
    return boolValue(false);
  }

  /**
   * @return set value; defaultValue otherwise
   */
  public boolean boolValue(boolean defaultValue) {
    if (isSet()) {
      Boolean b = (Boolean) value();
      return b.booleanValue();
    } else {
      return defaultValue;
    }
  }

  public Object clone() {
    SettableBoolValue out = new SettableBoolValue();
    if (this.isSet()) out.set(this.value());
    return out;
  }

}