/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.msg.LockRequestMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class RecallBatchContext implements TCSerializable {
  private Collection<ClientServerExchangeLockContext> contexts;
  private LockID                                      lockID;

  public RecallBatchContext() {
    // To make TCSerializable happy
  }

  public RecallBatchContext(Collection<ClientServerExchangeLockContext> lockState, LockID lockID) {
    this.contexts = lockState;
    this.lockID = lockID;
  }

  public void addToMessage(final LockRequestMessage lrm) {
    lrm.addRecallBatchContext(this);
  }

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    LockIDSerializer ls = new LockIDSerializer();
    ls.deserializeFrom(in);
    this.lockID = ls.getLockID();
    int length = in.readInt();
    contexts = new ArrayList<ClientServerExchangeLockContext>();
    for (int i = 0; i < length; i++) {
      contexts.add((ClientServerExchangeLockContext) (new ClientServerExchangeLockContext().deserializeFrom(in)));
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput out) {
    LockIDSerializer ls = new LockIDSerializer(lockID);
    ls.serializeTo(out);
    out.writeInt(contexts.size());
    for (ClientServerExchangeLockContext lockContext : contexts) {
      lockContext.serializeTo(out);
    }
  }

  public Collection<ClientServerExchangeLockContext> getContexts() {
    return contexts;
  }

  public LockID getLockID() {
    return lockID;
  }
}
