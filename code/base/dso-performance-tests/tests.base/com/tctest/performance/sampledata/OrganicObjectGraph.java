/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class OrganicObjectGraph implements Serializable {

  private static final int   INT        = 0;
  private static final int   STRING     = 1;
  private static final int   SHORT      = 2;
  private static final int   DOUBLE     = 3;
  private final int          sequenceNumber;
  private final String       envKey;
  private final List         references = new ArrayList();
  private transient Random   random;
  private int                changeIteration;             // used as random seed
  private OrganicObjectGraph parent;

  public OrganicObjectGraph(int sequenceNumber, String envKey) {
    this.sequenceNumber = sequenceNumber;
    this.envKey = envKey;
  }

  public OrganicObjectGraph() {
    this.sequenceNumber = -1;
    this.envKey = null;
  }

  synchronized void addReference(OrganicObjectGraph ref) {
    references.add(ref);
  }

  public synchronized void mutateRandom(int changes) {
    random = new Random(changeIteration++);
    int changeCount = 0;
    OrganicObjectGraph current = this;
    while (changeCount < changes) {
      if (oneOverTwo()) current = traverse(current);
      else changeCount += update(current, changes, changeCount);
    }
  }

  public synchronized int changeIterationCount() {
    return changeIteration;
  }

  public int sequenceNumber() {
    return sequenceNumber;
  }

  public String envKey() {
    return envKey;
  }

  private int update(OrganicObjectGraph current, int totalChanges, int changeCount) {
    int changes;
    int fieldIndex;
    int fieldType;
    int size = current.getSize();
    do {
      changes = new Long(Math.round(Math.sqrt(getRandom(totalChanges + 1)))).intValue();
    } while (changes == 0);
    if (changes > totalChanges - changeCount) changes = totalChanges - changeCount;

    for (int i = 0; i < changes; i++) {
      fieldIndex = getRandom(size);
      fieldType = current.getType(fieldIndex);
      switch (fieldType) {
        case INT:
          current.setValue(fieldIndex, getRandom(999999999));
          break;
        case STRING:
          current.setValue(fieldIndex, getRandom(999999999) + "_STR");
          break;
        case SHORT:
          current.setValue(fieldIndex, Short.parseShort(getRandom(9999) + ""));
          break;
        case DOUBLE:
          current.setValue(fieldIndex, random.nextDouble());
          break;
        default:
          break;
      }
    }
    return changes;
  }

  private OrganicObjectGraph traverse(OrganicObjectGraph current) {
    if (!twoOverThree() && (current.parent != null)) {
      return current.parent;
    } else if (references.size() > 0) {
      int index = getRandom(references.size());
      return (OrganicObjectGraph) references.get(index);
    } else if (current.parent != null) {
      int index = getRandom(current.parent.references.size());
      return (OrganicObjectGraph) current.parent.references.get(index);
    } else {
      return this;
    }
  }

  private boolean twoOverThree() {
    return (getRandom(3) > 1) ? true : false;
  }

  private boolean oneOverTwo() {
    return (getRandom(2) == 1) ? true : false;
  }

  protected synchronized void setParent(OrganicObjectGraph parent) {
    this.parent = parent;
  }

  protected abstract int getSize();

  protected abstract int getType(int index);

  protected void setValue(int index, int value) {
    throw new RuntimeException("No concrete implementation available.");
  }

  protected void setValue(int index, String value) {
    throw new RuntimeException("No concrete implementation available.");
  }

  protected void setValue(int index, short value) {
    throw new RuntimeException("No concrete implementation available.");
  }

  protected void setValue(int index, double value) {
    throw new RuntimeException("No concrete implementation available.");
  }

  private int getRandom(int bound) {
    return new Long(Math.round(Math.floor(bound * random.nextDouble()))).intValue();
  }

  public String toString() {
    return String.valueOf(getSize()) + "\n" + toString(1);
  }

  public String toString(int level) {
    String indent = "";
    for (int i = 0; i < level; i++) {
      indent += "  ";
    }
    String hierarchy = "";
    for (int i = 0; i < references.size(); i++) {
      hierarchy += ((OrganicObjectGraph) references.get(i)).toString(level++);
    }
    return indent + String.valueOf(getSize()) + "\n" + hierarchy;
  }

  public boolean equals(Object rawObj) {
    OrganicObjectGraph obj = (OrganicObjectGraph) rawObj;
    OrganicObjectGraph thisChild;
    OrganicObjectGraph objChild;
    for (int i = 0; i < obj.references.size(); i++) {
      thisChild = (OrganicObjectGraph) references.get(i);
      objChild = (OrganicObjectGraph) obj.references.get(i);
      if (thisChild == null || objChild == null) return false;
      if (!thisChild.equals(objChild)) return false;
    }
    return true;
  }
}
