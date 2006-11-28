/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
