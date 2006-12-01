/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_41 extends OrganicObjectGraph {

  private int size = 12;
  private int[] types = new int[] { 1, 3, 1, 0, 3, 0, 3, 2, 2, 1, 1, 0 };

  private String f0;
  private double f1;
  private String f2;
  private int f3;
  private double f4;
  private int f5;
  private double f6;
  private short f7;
  private short f8;
  private String f9;
  private String f10;
  private int f11;

  public OrganicObjectGraphNode_41(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_41() {
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
      case 2:
        f2 = value;
      case 9:
        f9 = value;
      case 10:
        f10 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 1:
        f1 = value;
      case 4:
        f4 = value;
      case 6:
        f6 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 3:
        f3 = value;
      case 5:
        f5 = value;
      case 11:
        f11 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 7:
        f7 = value;
      case 8:
        f8 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_41)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_41"); return false; }
    OrganicObjectGraphNode_41 obj = (OrganicObjectGraphNode_41) rawObj;
    if (!("" + f0).equals("" + obj.f0)) return false;
    if (f1 != obj.f1) return false;
    if (!("" + f2).equals("" + obj.f2)) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (f5 != obj.f5) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (!("" + f9).equals("" + obj.f9)) return false;
    if (!("" + f10).equals("" + obj.f10)) return false;
    if (f11 != obj.f11) return false;
    return super.equals(obj);
  }
}
