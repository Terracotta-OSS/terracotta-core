/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_99 extends OrganicObjectGraph {

  private int size = 18;
  private int[] types = new int[] { 0, 2, 1, 3, 1, 0, 0, 0, 3, 2, 3, 1, 1, 1, 0, 3, 1, 0 };

  private int f0;
  private short f1;
  private String f2;
  private double f3;
  private String f4;
  private int f5;
  private int f6;
  private int f7;
  private double f8;
  private short f9;
  private double f10;
  private String f11;
  private String f12;
  private String f13;
  private int f14;
  private double f15;
  private String f16;
  private int f17;

  public OrganicObjectGraphNode_99(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_99() {
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
      case 5:
        f5 = value;
      case 6:
        f6 = value;
      case 7:
        f7 = value;
      case 14:
        f14 = value;
      case 17:
        f17 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 1:
        f1 = value;
      case 9:
        f9 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 2:
        f2 = value;
      case 4:
        f4 = value;
      case 11:
        f11 = value;
      case 12:
        f12 = value;
      case 13:
        f13 = value;
      case 16:
        f16 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 3:
        f3 = value;
      case 8:
        f8 = value;
      case 10:
        f10 = value;
      case 15:
        f15 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_99)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_99"); return false; }
    OrganicObjectGraphNode_99 obj = (OrganicObjectGraphNode_99) rawObj;
    if (f0 != obj.f0) return false;
    if (f1 != obj.f1) return false;
    if (!("" + f2).equals("" + obj.f2)) return false;
    if (f3 != obj.f3) return false;
    if (!("" + f4).equals("" + obj.f4)) return false;
    if (f5 != obj.f5) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (f9 != obj.f9) return false;
    if (f10 != obj.f10) return false;
    if (!("" + f11).equals("" + obj.f11)) return false;
    if (!("" + f12).equals("" + obj.f12)) return false;
    if (!("" + f13).equals("" + obj.f13)) return false;
    if (f14 != obj.f14) return false;
    if (f15 != obj.f15) return false;
    if (!("" + f16).equals("" + obj.f16)) return false;
    if (f17 != obj.f17) return false;
    return super.equals(obj);
  }
}
