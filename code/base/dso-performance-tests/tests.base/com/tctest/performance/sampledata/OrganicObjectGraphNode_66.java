/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_66 extends OrganicObjectGraph {

  private int size = 1;
  private int[] types = new int[] { 2 };

  private short f0;

  public OrganicObjectGraphNode_66(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_66() {
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

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_66)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_66"); return false; }
    OrganicObjectGraphNode_66 obj = (OrganicObjectGraphNode_66) rawObj;
    if (f0 != obj.f0) return false;
    return super.equals(obj);
  }
}
