/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_3 extends OrganicObjectGraph {

  private int size = 25;
  private int[] types = new int[] { 2, 1, 1, 2, 1, 0, 2, 2, 2, 0, 1, 3, 0, 0, 1, 3, 1, 1, 2, 1, 1, 2, 2, 3, 2 };

  private short f0;
  private String f1;
  private String f2;
  private short f3;
  private String f4;
  private int f5;
  private short f6;
  private short f7;
  private short f8;
  private int f9;
  private String f10;
  private double f11;
  private int f12;
  private int f13;
  private String f14;
  private double f15;
  private String f16;
  private String f17;
  private short f18;
  private String f19;
  private String f20;
  private short f21;
  private short f22;
  private double f23;
  private short f24;

  public OrganicObjectGraphNode_3(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_3() {
    super();
  }

  protected int getSize() {
    return size;
  }

  protected int getType(int index) {
    return types[index];
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 0:
        f0 = value;
      case 3:
        f3 = value;
      case 6:
        f6 = value;
      case 7:
        f7 = value;
      case 8:
        f8 = value;
      case 18:
        f18 = value;
      case 21:
        f21 = value;
      case 22:
        f22 = value;
      case 24:
        f24 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 1:
        f1 = value;
      case 2:
        f2 = value;
      case 4:
        f4 = value;
      case 10:
        f10 = value;
      case 14:
        f14 = value;
      case 16:
        f16 = value;
      case 17:
        f17 = value;
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
      case 5:
        f5 = value;
      case 9:
        f9 = value;
      case 12:
        f12 = value;
      case 13:
        f13 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 11:
        f11 = value;
      case 15:
        f15 = value;
      case 23:
        f23 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_3)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_3"); return false; }
    OrganicObjectGraphNode_3 obj = (OrganicObjectGraphNode_3) rawObj;
    if (f0 != obj.f0) return false;
    if (!("" + f1).equals("" + obj.f1)) return false;
    if (!("" + f2).equals("" + obj.f2)) return false;
    if (f3 != obj.f3) return false;
    if (!("" + f4).equals("" + obj.f4)) return false;
    if (f5 != obj.f5) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (f9 != obj.f9) return false;
    if (!("" + f10).equals("" + obj.f10)) return false;
    if (f11 != obj.f11) return false;
    if (f12 != obj.f12) return false;
    if (f13 != obj.f13) return false;
    if (!("" + f14).equals("" + obj.f14)) return false;
    if (f15 != obj.f15) return false;
    if (!("" + f16).equals("" + obj.f16)) return false;
    if (!("" + f17).equals("" + obj.f17)) return false;
    if (f18 != obj.f18) return false;
    if (!("" + f19).equals("" + obj.f19)) return false;
    if (!("" + f20).equals("" + obj.f20)) return false;
    if (f21 != obj.f21) return false;
    if (f22 != obj.f22) return false;
    if (f23 != obj.f23) return false;
    if (f24 != obj.f24) return false;
    return super.equals(obj);
  }
}
