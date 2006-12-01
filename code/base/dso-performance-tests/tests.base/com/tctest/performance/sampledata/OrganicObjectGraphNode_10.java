/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_10 extends OrganicObjectGraph {

  private int size = 10;
  private int[] types = new int[] { 3, 0, 3, 1, 2, 0, 0, 3, 1, 0 };

  private double f0;
  private int f1;
  private double f2;
  private String f3;
  private short f4;
  private int f5;
  private int f6;
  private double f7;
  private String f8;
  private int f9;

  public OrganicObjectGraphNode_10(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_10() {
    super();
  }

  protected int getSize() {
    return size;
  }

  protected int getType(int index) {
    return types[index];
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 0:
        f0 = value;
      case 2:
        f2 = value;
      case 7:
        f7 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 1:
        f1 = value;
      case 5:
        f5 = value;
      case 6:
        f6 = value;
      case 9:
        f9 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 3:
        f3 = value;
      case 8:
        f8 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 4:
        f4 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_10)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_10"); return false; }
    OrganicObjectGraphNode_10 obj = (OrganicObjectGraphNode_10) rawObj;
    if (f0 != obj.f0) return false;
    if (f1 != obj.f1) return false;
    if (f2 != obj.f2) return false;
    if (!("" + f3).equals("" + obj.f3)) return false;
    if (f4 != obj.f4) return false;
    if (f5 != obj.f5) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (!("" + f8).equals("" + obj.f8)) return false;
    if (f9 != obj.f9) return false;
    return super.equals(obj);
  }
}
