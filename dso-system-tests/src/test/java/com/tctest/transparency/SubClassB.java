/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;

public class SubClassB extends SuperClassWithFields implements Cloneable {

  public synchronized void method1() {
    i = 10;
    super.method1();
  }

  public SubClassB getCopy() {
    try {
      return (SubClassB) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
  
  public String toString() {
    return "SubClassB::" + super.toString();
  }
  
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof SubClassB) { return super.equals(o); }
    return false;
  }
}
