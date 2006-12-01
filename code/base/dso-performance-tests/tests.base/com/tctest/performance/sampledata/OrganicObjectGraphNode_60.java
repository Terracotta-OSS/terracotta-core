/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_60 extends OrganicObjectGraph {

  private int size = 43;
  private int[] types = new int[] { 0, 2, 3, 3, 0, 0, 2, 3, 3, 1, 0, 0, 1, 2, 1, 0, 3, 1, 2, 3, 3, 3, 0, 1, 0, 1, 0, 2, 2, 0, 0, 1, 1, 0, 3, 2, 0, 0, 3, 3, 1, 3, 0 };

  private int f0;
  private short f1;
  private double f2;
  private double f3;
  private int f4;
  private int f5;
  private short f6;
  private double f7;
  private double f8;
  private String f9;
  private int f10;
  private int f11;
  private String f12;
  private short f13;
  private String f14;
  private int f15;
  private double f16;
  private String f17;
  private short f18;
  private double f19;
  private double f20;
  private double f21;
  private int f22;
  private String f23;
  private int f24;
  private String f25;
  private int f26;
  private short f27;
  private short f28;
  private int f29;
  private int f30;
  private String f31;
  private String f32;
  private int f33;
  private double f34;
  private short f35;
  private int f36;
  private int f37;
  private double f38;
  private double f39;
  private String f40;
  private double f41;
  private int f42;

  public OrganicObjectGraphNode_60(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_60() {
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
      case 10:
        f10 = value;
      case 11:
        f11 = value;
      case 15:
        f15 = value;
      case 22:
        f22 = value;
      case 24:
        f24 = value;
      case 26:
        f26 = value;
      case 29:
        f29 = value;
      case 30:
        f30 = value;
      case 33:
        f33 = value;
      case 36:
        f36 = value;
      case 37:
        f37 = value;
      case 42:
        f42 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 1:
        f1 = value;
      case 6:
        f6 = value;
      case 13:
        f13 = value;
      case 18:
        f18 = value;
      case 27:
        f27 = value;
      case 28:
        f28 = value;
      case 35:
        f35 = value;
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
      case 7:
        f7 = value;
      case 8:
        f8 = value;
      case 16:
        f16 = value;
      case 19:
        f19 = value;
      case 20:
        f20 = value;
      case 21:
        f21 = value;
      case 34:
        f34 = value;
      case 38:
        f38 = value;
      case 39:
        f39 = value;
      case 41:
        f41 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 9:
        f9 = value;
      case 12:
        f12 = value;
      case 14:
        f14 = value;
      case 17:
        f17 = value;
      case 23:
        f23 = value;
      case 25:
        f25 = value;
      case 31:
        f31 = value;
      case 32:
        f32 = value;
      case 40:
        f40 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_60)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_60"); return false; }
    OrganicObjectGraphNode_60 obj = (OrganicObjectGraphNode_60) rawObj;
    if (f0 != obj.f0) return false;
    if (f1 != obj.f1) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (f5 != obj.f5) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (!("" + f9).equals("" + obj.f9)) return false;
    if (f10 != obj.f10) return false;
    if (f11 != obj.f11) return false;
    if (!("" + f12).equals("" + obj.f12)) return false;
    if (f13 != obj.f13) return false;
    if (!("" + f14).equals("" + obj.f14)) return false;
    if (f15 != obj.f15) return false;
    if (f16 != obj.f16) return false;
    if (!("" + f17).equals("" + obj.f17)) return false;
    if (f18 != obj.f18) return false;
    if (f19 != obj.f19) return false;
    if (f20 != obj.f20) return false;
    if (f21 != obj.f21) return false;
    if (f22 != obj.f22) return false;
    if (!("" + f23).equals("" + obj.f23)) return false;
    if (f24 != obj.f24) return false;
    if (!("" + f25).equals("" + obj.f25)) return false;
    if (f26 != obj.f26) return false;
    if (f27 != obj.f27) return false;
    if (f28 != obj.f28) return false;
    if (f29 != obj.f29) return false;
    if (f30 != obj.f30) return false;
    if (!("" + f31).equals("" + obj.f31)) return false;
    if (!("" + f32).equals("" + obj.f32)) return false;
    if (f33 != obj.f33) return false;
    if (f34 != obj.f34) return false;
    if (f35 != obj.f35) return false;
    if (f36 != obj.f36) return false;
    if (f37 != obj.f37) return false;
    if (f38 != obj.f38) return false;
    if (f39 != obj.f39) return false;
    if (!("" + f40).equals("" + obj.f40)) return false;
    if (f41 != obj.f41) return false;
    if (f42 != obj.f42) return false;
    return super.equals(obj);
  }
}
