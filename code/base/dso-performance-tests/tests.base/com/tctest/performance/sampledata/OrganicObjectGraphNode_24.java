/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_24 extends OrganicObjectGraph {

  private int size = 15;
  private int[] types = new int[] { 2, 0, 2, 3, 0, 2, 3, 0, 3, 0, 3, 2, 1, 2, 1 };

  private short f0;
  private int f1;
  private short f2;
  private double f3;
  private int f4;
  private short f5;
  private double f6;
  private int f7;
  private double f8;
  private int f9;
  private double f10;
  private short f11;
  private String f12;
  private short f13;
  private String f14;

  public OrganicObjectGraphNode_24(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_24() {
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
      case 2:
        f2 = value;
      case 5:
        f5 = value;
      case 11:
        f11 = value;
      case 13:
        f13 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 1:
        f1 = value;
      case 4:
        f4 = value;
      case 7:
        f7 = value;
      case 9:
        f9 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 3:
        f3 = value;
      case 6:
        f6 = value;
      case 8:
        f8 = value;
      case 10:
        f10 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 12:
        f12 = value;
      case 14:
        f14 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_24)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_24"); return false; }
    OrganicObjectGraphNode_24 obj = (OrganicObjectGraphNode_24) rawObj;
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
    if (f10 != obj.f10) return false;
    if (f11 != obj.f11) return false;
    if (!("" + f12).equals("" + obj.f12)) return false;
    if (f13 != obj.f13) return false;
    if (!("" + f14).equals("" + obj.f14)) return false;
    return super.equals(obj);
  }
}
