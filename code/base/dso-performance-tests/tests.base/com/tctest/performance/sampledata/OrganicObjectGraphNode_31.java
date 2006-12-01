/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_31 extends OrganicObjectGraph {

  private int size = 32;
  private int[] types = new int[] { 3, 2, 0, 0, 0, 2, 2, 0, 3, 0, 1, 3, 3, 3, 0, 1, 3, 0, 2, 0, 2, 3, 3, 0, 3, 3, 2, 3, 3, 0, 3, 3 };

  private double f0;
  private short f1;
  private int f2;
  private int f3;
  private int f4;
  private short f5;
  private short f6;
  private int f7;
  private double f8;
  private int f9;
  private String f10;
  private double f11;
  private double f12;
  private double f13;
  private int f14;
  private String f15;
  private double f16;
  private int f17;
  private short f18;
  private int f19;
  private short f20;
  private double f21;
  private double f22;
  private int f23;
  private double f24;
  private double f25;
  private short f26;
  private double f27;
  private double f28;
  private int f29;
  private double f30;
  private double f31;

  public OrganicObjectGraphNode_31(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_31() {
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
      case 8:
        f8 = value;
      case 11:
        f11 = value;
      case 12:
        f12 = value;
      case 13:
        f13 = value;
      case 16:
        f16 = value;
      case 21:
        f21 = value;
      case 22:
        f22 = value;
      case 24:
        f24 = value;
      case 25:
        f25 = value;
      case 27:
        f27 = value;
      case 28:
        f28 = value;
      case 30:
        f30 = value;
      case 31:
        f31 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 1:
        f1 = value;
      case 5:
        f5 = value;
      case 6:
        f6 = value;
      case 18:
        f18 = value;
      case 20:
        f20 = value;
      case 26:
        f26 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 2:
        f2 = value;
      case 3:
        f3 = value;
      case 4:
        f4 = value;
      case 7:
        f7 = value;
      case 9:
        f9 = value;
      case 14:
        f14 = value;
      case 17:
        f17 = value;
      case 19:
        f19 = value;
      case 23:
        f23 = value;
      case 29:
        f29 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 10:
        f10 = value;
      case 15:
        f15 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_31)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_31"); return false; }
    OrganicObjectGraphNode_31 obj = (OrganicObjectGraphNode_31) rawObj;
    if (f0 != obj.f0) return false;
    if (f1 != obj.f1) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (f5 != obj.f5) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (f9 != obj.f9) return false;
    if (!("" + f10).equals("" + obj.f10)) return false;
    if (f11 != obj.f11) return false;
    if (f12 != obj.f12) return false;
    if (f13 != obj.f13) return false;
    if (f14 != obj.f14) return false;
    if (!("" + f15).equals("" + obj.f15)) return false;
    if (f16 != obj.f16) return false;
    if (f17 != obj.f17) return false;
    if (f18 != obj.f18) return false;
    if (f19 != obj.f19) return false;
    if (f20 != obj.f20) return false;
    if (f21 != obj.f21) return false;
    if (f22 != obj.f22) return false;
    if (f23 != obj.f23) return false;
    if (f24 != obj.f24) return false;
    if (f25 != obj.f25) return false;
    if (f26 != obj.f26) return false;
    if (f27 != obj.f27) return false;
    if (f28 != obj.f28) return false;
    if (f29 != obj.f29) return false;
    if (f30 != obj.f30) return false;
    if (f31 != obj.f31) return false;
    return super.equals(obj);
  }
}
