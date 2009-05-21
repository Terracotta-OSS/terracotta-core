/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.exception.ImplementMe;
import com.tc.object.TCObject;
import com.tc.object.bytecode.TransparentAccess;

import java.util.Map;

public class UnInstrumentedObject implements TransparentAccess {
  private long                 longValue;
  private boolean              booleanValue;
  private UnInstrumentedObject objectValue;
  private UnInstrumentedObject objectValue1;
  private UnInstrumentedObject objectValue2;
  private final Object               object      = new Object();
  public Object                lock        = new Object();
  private boolean              protect     = false;
  private final Object         protectLock = new Object();

  public UnInstrumentedObject(boolean sub) {
    this.longValue = System.currentTimeMillis();
    this.booleanValue = System.currentTimeMillis() % 2 == 0;
    if (sub) {
      this.objectValue = new UnInstrumentedObject(false);
      this.objectValue1 = new UnInstrumentedObject(false);
      this.objectValue2 = new UnInstrumentedObject(false);
    }
  }

  public void setValues() {
    this.longValue = System.currentTimeMillis();
    this.booleanValue = System.currentTimeMillis() % 2 == 0;

    this.objectValue = new UnInstrumentedObject(false);
    this.objectValue1 = new UnInstrumentedObject(false);
    this.objectValue2 = new UnInstrumentedObject(false);
  }

  public void accessValues() {
    if (object == null) {
      System.out.println("blah");
    }

    myMethod(longValue, booleanValue, objectValue, objectValue1, objectValue2);
    if (objectValue != null) {
      synchronized (getLock()) {
        objectValue.accessValues();
      }
      synchronized (getLock()) {
        objectValue1.accessValues();
      }
      synchronized (getLock()) {
        objectValue2.accessValues();
      }
    }
  }

  // public void accessValues() {
  // if (object == null) {
  // System.out.println("blah");
  // }
  //
  // myMethod(longValue, booleanValue, objectValue, objectValue1, objectValue2);
  // if (objectValue != null) {
  // makeProtected();
  // objectValue.accessValues();
  // makeUnProtected();
  //
  // makeProtected();
  // objectValue1.accessValues();
  // makeUnProtected();
  //
  // makeProtected();
  // objectValue2.accessValues();
  // makeUnProtected();
  // }
  // }

  public void makeUnProtected() {
    protect = false;

  }

  public void makeProtected() {
    protect = true;
    synchronized (protectLock) {
      protectLock.notify();
    }
  }

  public boolean isProtected() {
    return this.protect;
  }

  public Object getLock() {
    return lock;
  }

  private void myMethod(long longValue2, boolean booleanValue2, UnInstrumentedObject o, UnInstrumentedObject o1,
                        UnInstrumentedObject o2) {
    // doNothing
  }

  public void __tc_getallfields(Map map) {
    //
  }

  public void __tc_setfield(String name, Object value) {
    //
  }

  public void __tc_managed(TCObject t) {
    //
  }

  public TCObject __tc_managed() {
    return null;
  }

  public Object __tc_getmanagedfield(String name) {
    return null;
  }

  public void __tc_setmanagedfield(String name, Object value) {
    //
  }

  public Object __tc_getfield(String name) {
    throw new ImplementMe();
  }
}
