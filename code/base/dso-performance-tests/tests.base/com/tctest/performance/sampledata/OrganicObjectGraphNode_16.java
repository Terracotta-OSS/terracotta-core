/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_16 extends OrganicObjectGraph {

  private int size = 27;
  private int[] types = new int[] { 0, 1, 0, 2, 0, 2, 0, 0, 3, 1, 3, 1, 1, 2, 0, 1, 2, 2, 2, 0, 0, 2, 3, 1, 3, 1, 2 };

  private int f0;
  private String f1;
  private int f2;
  private short f3;
  private int f4;
  private short f5;
  private int f6;
  private int f7;
  private double f8;
  private String f9;
  private double f10;
  private String f11;
  private String f12;
  private short f13;
  private int f14;
  private String f15;
  private short f16;
  private short f17;
  private short f18;
  private int f19;
  private int f20;
  private short f21;
  private double f22;
  private String f23;
  private double f24;
  private String f25;
  private short f26;

  public OrganicObjectGraphNode_16(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_16() {
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
      case 2:
        f2 = value;
      case 4:
        f4 = value;
      case 6:
        f6 = value;
      case 7:
        f7 = value;
      case 14:
        f14 = value;
      case 19:
        f19 = value;
      case 20:
        f20 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 1:
        f1 = value;
      case 9:
        f9 = value;
      case 11:
        f11 = value;
      case 12:
        f12 = value;
      case 15:
        f15 = value;
      case 23:
        f23 = value;
      case 25:
        f25 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 3:
        f3 = value;
      case 5:
        f5 = value;
      case 13:
        f13 = value;
      case 16:
        f16 = value;
      case 17:
        f17 = value;
      case 18:
        f18 = value;
      case 21:
        f21 = value;
      case 26:
        f26 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 8:
        f8 = value;
      case 10:
        f10 = value;
      case 22:
        f22 = value;
      case 24:
        f24 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_16)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_16"); return false; }
    OrganicObjectGraphNode_16 obj = (OrganicObjectGraphNode_16) rawObj;
    if (f0 != obj.f0) return false;
    if (!("" + f1).equals("" + obj.f1)) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (f5 != obj.f5) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (!("" + f9).equals("" + obj.f9)) return false;
    if (f10 != obj.f10) return false;
    if (!("" + f11).equals("" + obj.f11)) return false;
    if (!("" + f12).equals("" + obj.f12)) return false;
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
    if (!("" + f23).equals("" + obj.f23)) return false;
    if (f24 != obj.f24) return false;
    if (!("" + f25).equals("" + obj.f25)) return false;
    if (f26 != obj.f26) return false;
    return super.equals(obj);
  }
}
