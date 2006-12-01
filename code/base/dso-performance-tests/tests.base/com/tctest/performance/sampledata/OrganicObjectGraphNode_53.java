/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_53 extends OrganicObjectGraph {

  private int size = 23;
  private int[] types = new int[] { 0, 0, 2, 0, 2, 1, 3, 3, 3, 1, 0, 3, 0, 0, 0, 0, 1, 3, 2, 3, 0, 1, 3 };

  private int f0;
  private int f1;
  private short f2;
  private int f3;
  private short f4;
  private String f5;
  private double f6;
  private double f7;
  private double f8;
  private String f9;
  private int f10;
  private double f11;
  private int f12;
  private int f13;
  private int f14;
  private int f15;
  private String f16;
  private double f17;
  private short f18;
  private double f19;
  private int f20;
  private String f21;
  private double f22;

  public OrganicObjectGraphNode_53(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_53() {
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
      case 1:
        f1 = value;
      case 3:
        f3 = value;
      case 10:
        f10 = value;
      case 12:
        f12 = value;
      case 13:
        f13 = value;
      case 14:
        f14 = value;
      case 15:
        f15 = value;
      case 20:
        f20 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 2:
        f2 = value;
      case 4:
        f4 = value;
      case 18:
        f18 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 5:
        f5 = value;
      case 9:
        f9 = value;
      case 16:
        f16 = value;
      case 21:
        f21 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 6:
        f6 = value;
      case 7:
        f7 = value;
      case 8:
        f8 = value;
      case 11:
        f11 = value;
      case 17:
        f17 = value;
      case 19:
        f19 = value;
      case 22:
        f22 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_53)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_53"); return false; }
    OrganicObjectGraphNode_53 obj = (OrganicObjectGraphNode_53) rawObj;
    if (f0 != obj.f0) return false;
    if (f1 != obj.f1) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (!("" + f5).equals("" + obj.f5)) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (!("" + f9).equals("" + obj.f9)) return false;
    if (f10 != obj.f10) return false;
    if (f11 != obj.f11) return false;
    if (f12 != obj.f12) return false;
    if (f13 != obj.f13) return false;
    if (f14 != obj.f14) return false;
    if (f15 != obj.f15) return false;
    if (!("" + f16).equals("" + obj.f16)) return false;
    if (f17 != obj.f17) return false;
    if (f18 != obj.f18) return false;
    if (f19 != obj.f19) return false;
    if (f20 != obj.f20) return false;
    if (!("" + f21).equals("" + obj.f21)) return false;
    if (f22 != obj.f22) return false;
    return super.equals(obj);
  }
}
