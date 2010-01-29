/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.sequence;

public class ObjectIDSequenceProvider implements ObjectIDSequence {

  private long current;

  public ObjectIDSequenceProvider(long start) {
    this.current = start;
  }

  public synchronized long nextObjectIDBatch(int batchSize) {
    final long start = current;
    current += batchSize;
    return start;
  }

  public void setNextAvailableObjectID(long startID) {
    if (current > startID) { throw new AssertionError("Current value + " + current + " is greater than " + startID); }
    current = startID;
  }

  public long currentObjectIDValue() {
    return this.current;
  }

}
