/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
