/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;

/* Super class contains private members */
public class SubClassD extends NotInstrumented implements Cloneable {

  public int d = 2;

  public synchronized void method1() {
    toString();
  }
  
  public synchronized Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public String toString() {
    return "SubClassD::" + super.toString();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof SubClassD) { return d == ((SubClassD) o).d && super.equals(o); }
    return false;
  }
}
