/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class InstrumentedObject {
  private long               longValue;
  private boolean            booleanValue;
  private InstrumentedObject objectValue;
  private InstrumentedObject objectValue1;
  private InstrumentedObject objectValue2;

  public InstrumentedObject(boolean sub) {
    this.longValue = System.currentTimeMillis();
    this.booleanValue = System.currentTimeMillis() % 2 == 0;
    if (sub) {
      this.objectValue = new InstrumentedObject(false);
      this.objectValue1 = new InstrumentedObject(false);
      this.objectValue2 = new InstrumentedObject(false);
    }
  }

  public void accessValues() {
    myMethod(longValue, booleanValue, objectValue, objectValue1, objectValue2);
    if (objectValue != null) {
      objectValue.accessValues();
      objectValue1.accessValues();
      objectValue2.accessValues();
    }
  }

  public void setValues() {
    this.longValue = System.currentTimeMillis();
    this.booleanValue = System.currentTimeMillis() % 2 == 0;
    this.objectValue = new InstrumentedObject(false);
    this.objectValue1 = new InstrumentedObject(false);
    this.objectValue2 = new InstrumentedObject(false);
  }

  private void myMethod(long longValue2, boolean booleanValue2, InstrumentedObject value, InstrumentedObject value1,
                        InstrumentedObject value2) {
    // doNothing

  }
}