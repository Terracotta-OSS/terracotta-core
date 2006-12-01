/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_48 extends OrganicObjectGraph {

  private int size = 45;
  private int[] types = new int[] { 3, 3, 2, 0, 0, 1, 2, 1, 2, 0, 0, 3, 1, 1, 3, 0, 0, 1, 1, 0, 3, 2, 0, 1, 0, 3, 1, 2, 2, 2, 1, 2, 2, 0, 1, 3, 1, 1, 3, 0, 1, 0, 1, 3, 1 };

  private double f0;
  private double f1;
  private short f2;
  private int f3;
  private int f4;
  private String f5;
  private short f6;
  private String f7;
  private short f8;
  private int f9;
  private int f10;
  private double f11;
  private String f12;
  private String f13;
  private double f14;
  private int f15;
  private int f16;
  private String f17;
  private String f18;
  private int f19;
  private double f20;
  private short f21;
  private int f22;
  private String f23;
  private int f24;
  private double f25;
  private String f26;
  private short f27;
  private short f28;
  private short f29;
  private String f30;
  private short f31;
  private short f32;
  private int f33;
  private String f34;
  private double f35;
  private String f36;
  private String f37;
  private double f38;
  private int f39;
  private String f40;
  private int f41;
  private String f42;
  private double f43;
  private String f44;

  public OrganicObjectGraphNode_48(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_48() {
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
      case 1:
        f1 = value;
      case 11:
        f11 = value;
      case 14:
        f14 = value;
      case 20:
        f20 = value;
      case 25:
        f25 = value;
      case 35:
        f35 = value;
      case 38:
        f38 = value;
      case 43:
        f43 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 2:
        f2 = value;
      case 6:
        f6 = value;
      case 8:
        f8 = value;
      case 21:
        f21 = value;
      case 27:
        f27 = value;
      case 28:
        f28 = value;
      case 29:
        f29 = value;
      case 31:
        f31 = value;
      case 32:
        f32 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 3:
        f3 = value;
      case 4:
        f4 = value;
      case 9:
        f9 = value;
      case 10:
        f10 = value;
      case 15:
        f15 = value;
      case 16:
        f16 = value;
      case 19:
        f19 = value;
      case 22:
        f22 = value;
      case 24:
        f24 = value;
      case 33:
        f33 = value;
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
      case 5:
        f5 = value;
      case 7:
        f7 = value;
      case 12:
        f12 = value;
      case 13:
        f13 = value;
      case 17:
        f17 = value;
      case 18:
        f18 = value;
      case 23:
        f23 = value;
      case 26:
        f26 = value;
      case 30:
        f30 = value;
      case 34:
        f34 = value;
      case 36:
        f36 = value;
      case 37:
        f37 = value;
      case 40:
        f40 = value;
      case 42:
        f42 = value;
      case 44:
        f44 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_48)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_48"); return false; }
    OrganicObjectGraphNode_48 obj = (OrganicObjectGraphNode_48) rawObj;
    if (f0 != obj.f0) return false;
    if (f1 != obj.f1) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (!("" + f5).equals("" + obj.f5)) return false;
    if (f6 != obj.f6) return false;
    if (!("" + f7).equals("" + obj.f7)) return false;
    if (f8 != obj.f8) return false;
    if (f9 != obj.f9) return false;
    if (f10 != obj.f10) return false;
    if (f11 != obj.f11) return false;
    if (!("" + f12).equals("" + obj.f12)) return false;
    if (!("" + f13).equals("" + obj.f13)) return false;
    if (f14 != obj.f14) return false;
    if (f15 != obj.f15) return false;
    if (f16 != obj.f16) return false;
    if (!("" + f17).equals("" + obj.f17)) return false;
    if (!("" + f18).equals("" + obj.f18)) return false;
    if (f19 != obj.f19) return false;
    if (f20 != obj.f20) return false;
    if (f21 != obj.f21) return false;
    if (f22 != obj.f22) return false;
    if (!("" + f23).equals("" + obj.f23)) return false;
    if (f24 != obj.f24) return false;
    if (f25 != obj.f25) return false;
    if (!("" + f26).equals("" + obj.f26)) return false;
    if (f27 != obj.f27) return false;
    if (f28 != obj.f28) return false;
    if (f29 != obj.f29) return false;
    if (!("" + f30).equals("" + obj.f30)) return false;
    if (f31 != obj.f31) return false;
    if (f32 != obj.f32) return false;
    if (f33 != obj.f33) return false;
    if (!("" + f34).equals("" + obj.f34)) return false;
    if (f35 != obj.f35) return false;
    if (!("" + f36).equals("" + obj.f36)) return false;
    if (!("" + f37).equals("" + obj.f37)) return false;
    if (f38 != obj.f38) return false;
    if (f39 != obj.f39) return false;
    if (!("" + f40).equals("" + obj.f40)) return false;
    if (f41 != obj.f41) return false;
    if (!("" + f42).equals("" + obj.f42)) return false;
    if (f43 != obj.f43) return false;
    if (!("" + f44).equals("" + obj.f44)) return false;
    return super.equals(obj);
  }
}
