/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;

public class SubClassA extends SuperClassWithNoFields implements Cloneable {

  public synchronized void method1() {
    super.method1();
  }
  
  public SubClassA getCopy() {
    try {
      return (SubClassA) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public String toString() {
    return "SubClassA::" + super.toString();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof SubClassA) { return super.equals(o); }
    return false;
  }
}
