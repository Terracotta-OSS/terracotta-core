/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_12 extends OrganicObjectGraph {

  private int size = 10;
  private int[] types = new int[] { 1, 3, 2, 3, 2, 1, 1, 2, 0, 0 };

  private String f0;
  private double f1;
  private short f2;
  private double f3;
  private short f4;
  private String f5;
  private String f6;
  private short f7;
  private int f8;
  private int f9;

  public OrganicObjectGraphNode_12(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_12() {
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
      case 5:
        f5 = value;
      case 6:
        f6 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 1:
        f1 = value;
      case 3:
        f3 = value;
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
      case 7:
        f7 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 8:
        f8 = value;
      case 9:
        f9 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_12)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_12"); return false; }
    OrganicObjectGraphNode_12 obj = (OrganicObjectGraphNode_12) rawObj;
    if (!("" + f0).equals("" + obj.f0)) return false;
    if (f1 != obj.f1) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (!("" + f5).equals("" + obj.f5)) return false;
    if (!("" + f6).equals("" + obj.f6)) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (f9 != obj.f9) return false;
    return super.equals(obj);
  }
}
