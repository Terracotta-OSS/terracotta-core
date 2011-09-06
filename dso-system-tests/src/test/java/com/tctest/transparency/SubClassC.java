/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;

public class SubClassC extends SubClassB {

  int j = 9;
  
  public synchronized void method1() {
    i = 20;
    super.method1();
  }
  
  public SubClassB getCopy() {
    return super.getCopy();
  }
  
  public Object clone() {
    try {
      // This is insane, but this is test code so ...
      SubClassC c = (SubClassC) super.clone();
      c.i++;
      c.j--;
      return c;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
  
  public synchronized void checkedUnCheckedSetsAndGets() {
    //this should be unchecked 
    int newj = j + 100;
    int newi = i + 1000;
    j = newj;
    i = newi;
    
    // This should be checked
    SubClassD d = new SubClassD();
    d.d = d.d ++;
  }

  public String toString() {
    return "SubClassC::" + super.toString();
  }
  
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof SubClassC) { return j == ((SubClassC)o).j && super.equals(o); }
    return false;
  }
}
