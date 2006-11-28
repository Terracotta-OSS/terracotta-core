/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.transparency;

public class SuperClassWithNoFields {

  public synchronized void method1() {
    System.err.println(this);
  }

  public String toString() {
    return "SuperClassWithNoFields";
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof SuperClassWithNoFields) { return true; }
    return false;
  }
}
