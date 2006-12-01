/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

/**
 * A thin wrapper around SettableValue to help with boxing/unboxing int's
 */
public class SettableIntValue extends SettableValue {

  public SettableIntValue() {
    super();
  }

  public void setInt(int value) {
    super.set(new Integer(value));
  }

  public int intValue() {
    return intValue(0);
  }

  public int intValue(int defaultValue) {
    if (isSet()) {
      Integer i = (Integer) value();
      return i.intValue();
    } else {
      return defaultValue;
    }
  }

  public Object clone() {
    SettableIntValue out = new SettableIntValue();
    if (this.isSet()) out.set(this.value());
    return out;
  }
}