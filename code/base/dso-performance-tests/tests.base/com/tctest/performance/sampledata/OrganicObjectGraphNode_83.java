/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_83 extends OrganicObjectGraph {

  private int size = 5;
  private int[] types = new int[] { 1, 1, 1, 1, 2 };

  private String f0;
  private String f1;
  private String f2;
  private String f3;
  private short f4;

  public OrganicObjectGraphNode_83(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_83() {
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
      case 1:
        f1 = value;
      case 2:
        f2 = value;
      case 3:
        f3 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 4:
        f4 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_83)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_83"); return false; }
    OrganicObjectGraphNode_83 obj = (OrganicObjectGraphNode_83) rawObj;
    if (!("" + f0).equals("" + obj.f0)) return false;
    if (!("" + f1).equals("" + obj.f1)) return false;
    if (!("" + f2).equals("" + obj.f2)) return false;
    if (!("" + f3).equals("" + obj.f3)) return false;
    if (f4 != obj.f4) return false;
    return super.equals(obj);
  }
}
