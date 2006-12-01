/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_11 extends OrganicObjectGraph {

  private int size = 7;
  private int[] types = new int[] { 3, 1, 2, 2, 2, 1, 0 };

  private double f0;
  private String f1;
  private short f2;
  private short f3;
  private short f4;
  private String f5;
  private int f6;

  public OrganicObjectGraphNode_11(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_11() {
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
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 1:
        f1 = value;
      case 5:
        f5 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 2:
        f2 = value;
      case 3:
        f3 = value;
      case 4:
        f4 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 6:
        f6 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_11)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_11"); return false; }
    OrganicObjectGraphNode_11 obj = (OrganicObjectGraphNode_11) rawObj;
    if (f0 != obj.f0) return false;
    if (!("" + f1).equals("" + obj.f1)) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (!("" + f5).equals("" + obj.f5)) return false;
    if (f6 != obj.f6) return false;
    return super.equals(obj);
  }
}
