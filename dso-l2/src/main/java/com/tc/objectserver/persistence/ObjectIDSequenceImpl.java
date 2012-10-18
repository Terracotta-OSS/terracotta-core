/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.util.sequence.MutableSequence;
import com.tc.util.sequence.ObjectIDSequence;

/**
 *
 * @author mscott
 */
public class ObjectIDSequenceImpl implements ObjectIDSequence {
  private final MutableSequence sequence;

  public ObjectIDSequenceImpl(MutableSequence sequence) {
    this.sequence = sequence;
  }

  @Override
  public long nextObjectIDBatch(int batchSize) {
    return sequence.nextBatch(batchSize);
  }

  @Override
  public void setNextAvailableObjectID(long startID) {
    sequence.setNext(startID);
  }

  @Override
  public long currentObjectIDValue() {
    return sequence.current();
  }
}
