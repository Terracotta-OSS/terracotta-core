/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_17 extends OrganicObjectGraph {

  private int size = 2;
  private int[] types = new int[] { 2, 3 };

  private short f0;
  private double f1;

  public OrganicObjectGraphNode_17(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_17() {
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
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 1:
        f1 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_17)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_17"); return false; }
    OrganicObjectGraphNode_17 obj = (OrganicObjectGraphNode_17) rawObj;
    if (f0 != obj.f0) return false;
    if (f1 != obj.f1) return false;
    return super.equals(obj);
  }
}
