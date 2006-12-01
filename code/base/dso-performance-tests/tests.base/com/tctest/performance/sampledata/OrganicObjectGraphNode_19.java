/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_19 extends OrganicObjectGraph {

  private int size = 14;
  private int[] types = new int[] { 2, 1, 2, 1, 2, 1, 1, 1, 2, 0, 0, 3, 0, 3 };

  private short f0;
  private String f1;
  private short f2;
  private String f3;
  private short f4;
  private String f5;
  private String f6;
  private String f7;
  private short f8;
  private int f9;
  private int f10;
  private double f11;
  private int f12;
  private double f13;

  public OrganicObjectGraphNode_19(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_19() {
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
      case 4:
        f4 = value;
      case 8:
        f8 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 1:
        f1 = value;
      case 3:
        f3 = value;
      case 5:
        f5 = value;
      case 6:
        f6 = value;
      case 7:
        f7 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 9:
        f9 = value;
      case 10:
        f10 = value;
      case 12:
        f12 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 11:
        f11 = value;
      case 13:
        f13 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_19)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_19"); return false; }
    OrganicObjectGraphNode_19 obj = (OrganicObjectGraphNode_19) rawObj;
    if (f0 != obj.f0) return false;
    if (!("" + f1).equals("" + obj.f1)) return false;
    if (f2 != obj.f2) return false;
    if (!("" + f3).equals("" + obj.f3)) return false;
    if (f4 != obj.f4) return false;
    if (!("" + f5).equals("" + obj.f5)) return false;
    if (!("" + f6).equals("" + obj.f6)) return false;
    if (!("" + f7).equals("" + obj.f7)) return false;
    if (f8 != obj.f8) return false;
    if (f9 != obj.f9) return false;
    if (f10 != obj.f10) return false;
    if (f11 != obj.f11) return false;
    if (f12 != obj.f12) return false;
    if (f13 != obj.f13) return false;
    return super.equals(obj);
  }
}
