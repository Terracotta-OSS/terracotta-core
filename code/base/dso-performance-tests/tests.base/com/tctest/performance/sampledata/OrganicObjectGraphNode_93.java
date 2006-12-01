/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_93 extends OrganicObjectGraph {

  private int size = 26;
  private int[] types = new int[] { 3, 3, 1, 0, 0, 2, 0, 2, 3, 1, 2, 3, 0, 1, 2, 1, 2, 0, 1, 2, 3, 2, 1, 1, 2, 3 };

  private double f0;
  private double f1;
  private String f2;
  private int f3;
  private int f4;
  private short f5;
  private int f6;
  private short f7;
  private double f8;
  private String f9;
  private short f10;
  private double f11;
  private int f12;
  private String f13;
  private short f14;
  private String f15;
  private short f16;
  private int f17;
  private String f18;
  private short f19;
  private double f20;
  private short f21;
  private String f22;
  private String f23;
  private short f24;
  private double f25;

  public OrganicObjectGraphNode_93(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_93() {
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
      case 8:
        f8 = value;
      case 11:
        f11 = value;
      case 20:
        f20 = value;
      case 25:
        f25 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 2:
        f2 = value;
      case 9:
        f9 = value;
      case 13:
        f13 = value;
      case 15:
        f15 = value;
      case 18:
        f18 = value;
      case 22:
        f22 = value;
      case 23:
        f23 = value;
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
      case 6:
        f6 = value;
      case 12:
        f12 = value;
      case 17:
        f17 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 5:
        f5 = value;
      case 7:
        f7 = value;
      case 10:
        f10 = value;
      case 14:
        f14 = value;
      case 16:
        f16 = value;
      case 19:
        f19 = value;
      case 21:
        f21 = value;
      case 24:
        f24 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_93)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_93"); return false; }
    OrganicObjectGraphNode_93 obj = (OrganicObjectGraphNode_93) rawObj;
    if (f0 != obj.f0) return false;
    if (f1 != obj.f1) return false;
    if (!("" + f2).equals("" + obj.f2)) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (f5 != obj.f5) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (!("" + f9).equals("" + obj.f9)) return false;
    if (f10 != obj.f10) return false;
    if (f11 != obj.f11) return false;
    if (f12 != obj.f12) return false;
    if (!("" + f13).equals("" + obj.f13)) return false;
    if (f14 != obj.f14) return false;
    if (!("" + f15).equals("" + obj.f15)) return false;
    if (f16 != obj.f16) return false;
    if (f17 != obj.f17) return false;
    if (!("" + f18).equals("" + obj.f18)) return false;
    if (f19 != obj.f19) return false;
    if (f20 != obj.f20) return false;
    if (f21 != obj.f21) return false;
    if (!("" + f22).equals("" + obj.f22)) return false;
    if (!("" + f23).equals("" + obj.f23)) return false;
    if (f24 != obj.f24) return false;
    if (f25 != obj.f25) return false;
    return super.equals(obj);
  }
}
