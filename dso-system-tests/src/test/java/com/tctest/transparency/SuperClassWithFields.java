/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;

public class SuperClassWithFields {

  protected int i;

  public synchronized void method1() {
    System.err.println(this + " = " + i);
  }

  public String toString() {
    return "SuperClassWithFields";
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof SuperClassWithFields) { return this.i == ((SuperClassWithFields) o).i; }
    return false;
  }

}
