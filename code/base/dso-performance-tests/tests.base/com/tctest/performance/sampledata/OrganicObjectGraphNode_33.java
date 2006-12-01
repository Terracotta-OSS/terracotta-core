/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_33 extends OrganicObjectGraph {

  private int size = 24;
  private int[] types = new int[] { 1, 1, 3, 3, 2, 3, 1, 3, 2, 3, 2, 3, 2, 2, 1, 1, 3, 1, 3, 1, 1, 0, 1, 0 };

  private String f0;
  private String f1;
  private double f2;
  private double f3;
  private short f4;
  private double f5;
  private String f6;
  private double f7;
  private short f8;
  private double f9;
  private short f10;
  private double f11;
  private short f12;
  private short f13;
  private String f14;
  private String f15;
  private double f16;
  private String f17;
  private double f18;
  private String f19;
  private String f20;
  private int f21;
  private String f22;
  private int f23;

  public OrganicObjectGraphNode_33(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_33() {
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
      case 1:
        f1 = value;
      case 6:
        f6 = value;
      case 14:
        f14 = value;
      case 15:
        f15 = value;
      case 17:
        f17 = value;
      case 19:
        f19 = value;
      case 20:
        f20 = value;
      case 22:
        f22 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 2:
        f2 = value;
      case 3:
        f3 = value;
      case 5:
        f5 = value;
      case 7:
        f7 = value;
      case 9:
        f9 = value;
      case 11:
        f11 = value;
      case 16:
        f16 = value;
      case 18:
        f18 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 4:
        f4 = value;
      case 8:
        f8 = value;
      case 10:
        f10 = value;
      case 12:
        f12 = value;
      case 13:
        f13 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 21:
        f21 = value;
      case 23:
        f23 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_33)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_33"); return false; }
    OrganicObjectGraphNode_33 obj = (OrganicObjectGraphNode_33) rawObj;
    if (!("" + f0).equals("" + obj.f0)) return false;
    if (!("" + f1).equals("" + obj.f1)) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (f5 != obj.f5) return false;
    if (!("" + f6).equals("" + obj.f6)) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (f9 != obj.f9) return false;
    if (f10 != obj.f10) return false;
    if (f11 != obj.f11) return false;
    if (f12 != obj.f12) return false;
    if (f13 != obj.f13) return false;
    if (!("" + f14).equals("" + obj.f14)) return false;
    if (!("" + f15).equals("" + obj.f15)) return false;
    if (f16 != obj.f16) return false;
    if (!("" + f17).equals("" + obj.f17)) return false;
    if (f18 != obj.f18) return false;
    if (!("" + f19).equals("" + obj.f19)) return false;
    if (!("" + f20).equals("" + obj.f20)) return false;
    if (f21 != obj.f21) return false;
    if (!("" + f22).equals("" + obj.f22)) return false;
    if (f23 != obj.f23) return false;
    return super.equals(obj);
  }
}
