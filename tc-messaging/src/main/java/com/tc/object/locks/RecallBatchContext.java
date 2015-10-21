/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.msg.LockRequestMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class RecallBatchContext implements TCSerializable<RecallBatchContext> {
  private Collection<ClientServerExchangeLockContext> contexts;
  private LockID                                      lockID;

  public RecallBatchContext() {
    // To make TCSerializable happy
  }

  public RecallBatchContext(Collection<ClientServerExchangeLockContext> lockState, LockID lockID) {
    this.contexts = lockState;
    this.lockID = lockID;
  }

  public void addToMessage(LockRequestMessage lrm) {
    lrm.addRecallBatchContext(this);
  }

  @Override
  public RecallBatchContext deserializeFrom(TCByteBufferInput in) throws IOException {
    LockIDSerializer ls = new LockIDSerializer();
    ls.deserializeFrom(in);
    this.lockID = ls.getLockID();
    int length = in.readInt();
    contexts = new ArrayList<ClientServerExchangeLockContext>();
    for (int i = 0; i < length; i++) {
      contexts.add((new ClientServerExchangeLockContext().deserializeFrom(in)));
    }
    return this;
  }

  @Override
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
