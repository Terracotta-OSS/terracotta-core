/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_32 extends OrganicObjectGraph {

  private int size = 19;
  private int[] types = new int[] { 0, 2, 1, 2, 2, 1, 1, 0, 0, 1, 1, 1, 3, 3, 0, 1, 3, 3, 1 };

  private int f0;
  private short f1;
  private String f2;
  private short f3;
  private short f4;
  private String f5;
  private String f6;
  private int f7;
  private int f8;
  private String f9;
  private String f10;
  private String f11;
  private double f12;
  private double f13;
  private int f14;
  private String f15;
  private double f16;
  private double f17;
  private String f18;

  public OrganicObjectGraphNode_32(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_32() {
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
      case 7:
        f7 = value;
      case 8:
        f8 = value;
      case 14:
        f14 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 1:
        f1 = value;
      case 3:
        f3 = value;
      case 4:
        f4 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 2:
        f2 = value;
      case 5:
        f5 = value;
      case 6:
        f6 = value;
      case 9:
        f9 = value;
      case 10:
        f10 = value;
      case 11:
        f11 = value;
      case 15:
        f15 = value;
      case 18:
        f18 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 12:
        f12 = value;
      case 13:
        f13 = value;
      case 16:
        f16 = value;
      case 17:
        f17 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_32)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_32"); return false; }
    OrganicObjectGraphNode_32 obj = (OrganicObjectGraphNode_32) rawObj;
    if (f0 != obj.f0) return false;
    if (f1 != obj.f1) return false;
    if (!("" + f2).equals("" + obj.f2)) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (!("" + f5).equals("" + obj.f5)) return false;
    if (!("" + f6).equals("" + obj.f6)) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (!("" + f9).equals("" + obj.f9)) return false;
    if (!("" + f10).equals("" + obj.f10)) return false;
    if (!("" + f11).equals("" + obj.f11)) return false;
    if (f12 != obj.f12) return false;
    if (f13 != obj.f13) return false;
    if (f14 != obj.f14) return false;
    if (!("" + f15).equals("" + obj.f15)) return false;
    if (f16 != obj.f16) return false;
    if (f17 != obj.f17) return false;
    if (!("" + f18).equals("" + obj.f18)) return false;
    return super.equals(obj);
  }
}
