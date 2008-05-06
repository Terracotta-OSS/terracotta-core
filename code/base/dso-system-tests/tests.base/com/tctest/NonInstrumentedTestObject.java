/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import java.awt.Color;

/*
 * This class is used for testing non-instrumented objects.
 */
public class NonInstrumentedTestObject {
  private long[]   longArray   = new long[2];
  private Object[] objectArray = new Object[2];
  private long     longValue   = Long.MIN_VALUE;
  private Color    color       = new Color(100, true);

  public NonInstrumentedTestObject() {
    super();
  }

  public NonInstrumentedTestObject(long longValue) {
    this.longValue = longValue;
  }

  public long getLongValue() {
    return longValue;
  }

  public void setLongValue(long longValue) {
    this.longValue = longValue;
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public long[] getLongArray() {
    return longArray;
  }

  public void setLongArray(long[] longArray) {
    this.longArray = longArray;
  }

  public Object[] getObjectArray() {
    return objectArray;
  }

  public void setObjectArray(Object[] objectArray) {
    this.objectArray = objectArray;
  }
}
