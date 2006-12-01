/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_63 extends OrganicObjectGraph {

  private int size = 47;
  private int[] types = new int[] { 1, 0, 0, 0, 3, 3, 1, 2, 1, 1, 2, 1, 3, 3, 0, 1, 3, 0, 2, 3, 1, 3, 0, 2, 0, 2, 0, 1, 1, 0, 3, 1, 2, 2, 2, 3, 3, 0, 2, 1, 2, 3, 1, 3, 1, 3, 0 };

  private String f0;
  private int f1;
  private int f2;
  private int f3;
  private double f4;
  private double f5;
  private String f6;
  private short f7;
  private String f8;
  private String f9;
  private short f10;
  private String f11;
  private double f12;
  private double f13;
  private int f14;
  private String f15;
  private double f16;
  private int f17;
  private short f18;
  private double f19;
  private String f20;
  private double f21;
  private int f22;
  private short f23;
  private int f24;
  private short f25;
  private int f26;
  private String f27;
  private String f28;
  private int f29;
  private double f30;
  private String f31;
  private short f32;
  private short f33;
  private short f34;
  private double f35;
  private double f36;
  private int f37;
  private short f38;
  private String f39;
  private short f40;
  private double f41;
  private String f42;
  private double f43;
  private String f44;
  private double f45;
  private int f46;

  public OrganicObjectGraphNode_63(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_63() {
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
      case 6:
        f6 = value;
      case 8:
        f8 = value;
      case 9:
        f9 = value;
      case 11:
        f11 = value;
      case 15:
        f15 = value;
      case 20:
        f20 = value;
      case 27:
        f27 = value;
      case 28:
        f28 = value;
      case 31:
        f31 = value;
      case 39:
        f39 = value;
      case 42:
        f42 = value;
      case 44:
        f44 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 1:
        f1 = value;
      case 2:
        f2 = value;
      case 3:
        f3 = value;
      case 14:
        f14 = value;
      case 17:
        f17 = value;
      case 22:
        f22 = value;
      case 24:
        f24 = value;
      case 26:
        f26 = value;
      case 29:
        f29 = value;
      case 37:
        f37 = value;
      case 46:
        f46 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 4:
        f4 = value;
      case 5:
        f5 = value;
      case 12:
        f12 = value;
      case 13:
        f13 = value;
      case 16:
        f16 = value;
      case 19:
        f19 = value;
      case 21:
        f21 = value;
      case 30:
        f30 = value;
      case 35:
        f35 = value;
      case 36:
        f36 = value;
      case 41:
        f41 = value;
      case 43:
        f43 = value;
      case 45:
        f45 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 7:
        f7 = value;
      case 10:
        f10 = value;
      case 18:
        f18 = value;
      case 23:
        f23 = value;
      case 25:
        f25 = value;
      case 32:
        f32 = value;
      case 33:
        f33 = value;
      case 34:
        f34 = value;
      case 38:
        f38 = value;
      case 40:
        f40 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_63)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_63"); return false; }
    OrganicObjectGraphNode_63 obj = (OrganicObjectGraphNode_63) rawObj;
    if (!("" + f0).equals("" + obj.f0)) return false;
    if (f1 != obj.f1) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (f5 != obj.f5) return false;
    if (!("" + f6).equals("" + obj.f6)) return false;
    if (f7 != obj.f7) return false;
    if (!("" + f8).equals("" + obj.f8)) return false;
    if (!("" + f9).equals("" + obj.f9)) return false;
    if (f10 != obj.f10) return false;
    if (!("" + f11).equals("" + obj.f11)) return false;
    if (f12 != obj.f12) return false;
    if (f13 != obj.f13) return false;
    if (f14 != obj.f14) return false;
    if (!("" + f15).equals("" + obj.f15)) return false;
    if (f16 != obj.f16) return false;
    if (f17 != obj.f17) return false;
    if (f18 != obj.f18) return false;
    if (f19 != obj.f19) return false;
    if (!("" + f20).equals("" + obj.f20)) return false;
    if (f21 != obj.f21) return false;
    if (f22 != obj.f22) return false;
    if (f23 != obj.f23) return false;
    if (f24 != obj.f24) return false;
    if (f25 != obj.f25) return false;
    if (f26 != obj.f26) return false;
    if (!("" + f27).equals("" + obj.f27)) return false;
    if (!("" + f28).equals("" + obj.f28)) return false;
    if (f29 != obj.f29) return false;
    if (f30 != obj.f30) return false;
    if (!("" + f31).equals("" + obj.f31)) return false;
    if (f32 != obj.f32) return false;
    if (f33 != obj.f33) return false;
    if (f34 != obj.f34) return false;
    if (f35 != obj.f35) return false;
    if (f36 != obj.f36) return false;
    if (f37 != obj.f37) return false;
    if (f38 != obj.f38) return false;
    if (!("" + f39).equals("" + obj.f39)) return false;
    if (f40 != obj.f40) return false;
    if (f41 != obj.f41) return false;
    if (!("" + f42).equals("" + obj.f42)) return false;
    if (f43 != obj.f43) return false;
    if (!("" + f44).equals("" + obj.f44)) return false;
    if (f45 != obj.f45) return false;
    if (f46 != obj.f46) return false;
    return super.equals(obj);
  }
}
