/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
