/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx.optimistic;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.change.TCChangeBuffer;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;

public class DNAToChangeBufferBridge implements DNA {//, DNACursor {
  private final OptimisticTransactionManager txManager;
  private final TCChangeBuffer buffer;
  private final TCObject       tcObject;

  public DNAToChangeBufferBridge(OptimisticTransactionManager txManager, TCChangeBuffer buffer) {
    this.txManager = txManager;
    this.buffer = buffer;
    this.tcObject = buffer.getTCObject();
  }

  public long getVersion() {
    return tcObject.getVersion();
  }

  public boolean hasLength() {
    return tcObject.getTCClass().isIndexed();
  }

  public int getArraySize() {
    throw new ImplementMe();

  }

  public boolean isDelta() {
    return true;
  }

  public String getTypeName() {
    return tcObject.getTCClass().getName();
  }

  public ObjectID getObjectID() throws DNAException {
    return tcObject.getObjectID();
  }

  public ObjectID getParentObjectID() throws DNAException {
    throw new ImplementMe();
  }

  public DNACursor getCursor() {
    return buffer.getDNACursor(txManager);
  }

  public String getDefiningLoaderDescription() {
    throw new ImplementMe();
  }

}
