/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_7 extends OrganicObjectGraph {

  private int size = 9;
  private int[] types = new int[] { 2, 3, 1, 0, 0, 3, 2, 1, 1 };

  private short f0;
  private double f1;
  private String f2;
  private int f3;
  private int f4;
  private double f5;
  private short f6;
  private String f7;
  private String f8;

  public OrganicObjectGraphNode_7(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_7() {
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
      case 5:
        f5 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 2:
        f2 = value;
      case 7:
        f7 = value;
      case 8:
        f8 = value;
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
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_7)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_7"); return false; }
    OrganicObjectGraphNode_7 obj = (OrganicObjectGraphNode_7) rawObj;
    if (f0 != obj.f0) return false;
    if (f1 != obj.f1) return false;
    if (!("" + f2).equals("" + obj.f2)) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (f5 != obj.f5) return false;
    if (f6 != obj.f6) return false;
    if (!("" + f7).equals("" + obj.f7)) return false;
    if (!("" + f8).equals("" + obj.f8)) return false;
    return super.equals(obj);
  }
}
