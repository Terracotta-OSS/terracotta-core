/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_21 extends OrganicObjectGraph {

  private int size = 21;
  private int[] types = new int[] { 1, 0, 1, 1, 1, 0, 3, 0, 0, 2, 0, 3, 1, 2, 3, 0, 0, 2, 0, 1, 1 };

  private String f0;
  private int f1;
  private String f2;
  private String f3;
  private String f4;
  private int f5;
  private double f6;
  private int f7;
  private int f8;
  private short f9;
  private int f10;
  private double f11;
  private String f12;
  private short f13;
  private double f14;
  private int f15;
  private int f16;
  private short f17;
  private int f18;
  private String f19;
  private String f20;

  public OrganicObjectGraphNode_21(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_21() {
    super();
  }

  protected int getSize() {
    return size;
  }

  protected int getType(int index) {
    return types[index];
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 0:
        f0 = value;
      case 2:
        f2 = value;
      case 3:
        f3 = value;
      case 4:
        f4 = value;
      case 12:
        f12 = value;
      case 19:
        f19 = value;
      case 20:
        f20 = value;
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
      case 7:
        f7 = value;
      case 8:
        f8 = value;
      case 10:
        f10 = value;
      case 15:
        f15 = value;
      case 16:
        f16 = value;
      case 18:
        f18 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 6:
        f6 = value;
      case 11:
        f11 = value;
      case 14:
        f14 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 9:
        f9 = value;
      case 13:
        f13 = value;
      case 17:
        f17 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_21)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_21"); return false; }
    OrganicObjectGraphNode_21 obj = (OrganicObjectGraphNode_21) rawObj;
    if (!("" + f0).equals("" + obj.f0)) return false;
    if (f1 != obj.f1) return false;
    if (!("" + f2).equals("" + obj.f2)) return false;
    if (!("" + f3).equals("" + obj.f3)) return false;
    if (!("" + f4).equals("" + obj.f4)) return false;
    if (f5 != obj.f5) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (f9 != obj.f9) return false;
    if (f10 != obj.f10) return false;
    if (f11 != obj.f11) return false;
    if (!("" + f12).equals("" + obj.f12)) return false;
    if (f13 != obj.f13) return false;
    if (f14 != obj.f14) return false;
    if (f15 != obj.f15) return false;
    if (f16 != obj.f16) return false;
    if (f17 != obj.f17) return false;
    if (f18 != obj.f18) return false;
    if (!("" + f19).equals("" + obj.f19)) return false;
    if (!("" + f20).equals("" + obj.f20)) return false;
    return super.equals(obj);
  }
}
