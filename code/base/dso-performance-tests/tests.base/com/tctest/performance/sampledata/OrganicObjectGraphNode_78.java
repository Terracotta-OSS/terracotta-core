/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_78 extends OrganicObjectGraph {

  private int size = 13;
  private int[] types = new int[] { 0, 1, 2, 2, 0, 0, 2, 3, 0, 0, 3, 2, 3 };

  private int f0;
  private String f1;
  private short f2;
  private short f3;
  private int f4;
  private int f5;
  private short f6;
  private double f7;
  private int f8;
  private int f9;
  private double f10;
  private short f11;
  private double f12;

  public OrganicObjectGraphNode_78(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_78() {
    super();
  }

  protected int getSize() {
    return size;
  }

  protected int getType(int index) {
    return types[index];
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 0:
        f0 = value;
      case 4:
        f4 = value;
      case 5:
        f5 = value;
      case 8:
        f8 = value;
      case 9:
        f9 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 1:
        f1 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 2:
        f2 = value;
      case 3:
        f3 = value;
      case 6:
        f6 = value;
      case 11:
        f11 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 7:
        f7 = value;
      case 10:
        f10 = value;
      case 12:
        f12 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_78)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_78"); return false; }
    OrganicObjectGraphNode_78 obj = (OrganicObjectGraphNode_78) rawObj;
    if (f0 != obj.f0) return false;
    if (!("" + f1).equals("" + obj.f1)) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (f5 != obj.f5) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (f9 != obj.f9) return false;
    if (f10 != obj.f10) return false;
    if (f11 != obj.f11) return false;
    if (f12 != obj.f12) return false;
    return super.equals(obj);
  }
}
