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
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ReplicationMessageAck extends AbstractGroupMessage {
  //message types  
  public static final int INVALID               = 0; // Sent to replicate a request on the passive
  public static final int START_SYNC                = 4; // Sent from the passive when it wants the active to start passive sync.
  public static final int BATCH                = 5; // Sent from the passive to ack a batch of messages.

  // Factory methods.
  public static ReplicationMessageAck createSyncRequestMessage() {
    return new ReplicationMessageAck(START_SYNC);
  }

  public static ReplicationMessageAck createBatchAck() {
    return new ReplicationMessageAck(BATCH);
  }


  private List<Tuple> batch;

  public ReplicationMessageAck() {
    super(INVALID);
  }

//  this type requests passive sync from the active  
  private ReplicationMessageAck(int type) {
    super(type);
    if (BATCH == type) {
      this.batch = new ArrayList<Tuple>();
    }
  }

  // Note that this does change the instance, so synchronized would be required if it were being called by multiple threads.
  // However, due to other races in how the using code decides to stop changing a message, it makes more sense for them to serialize on that level.
  public void addAck(MessageID respondTo, ReplicationResultCode result) {
    Assert.assertTrue(BATCH == this.getType());
    Tuple tuple = new Tuple(respondTo, result);
    this.batch.add(tuple);
  }

  public List<Tuple> getBatch() {
    return this.batch;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    if (BATCH == this.getType()) {
      int batchSize = in.readInt();
      // We should never send an empty message.
      Assert.assertTrue(batchSize > 0);
      this.batch = new ArrayList<Tuple>();
      for (int i = 0; i < batchSize; ++i) {
        MessageID respondTo = new MessageID(in.readLong());
        ReplicationResultCode result = ReplicationResultCode.decode(in.readInt());
        this.batch.add(new Tuple(respondTo, result));
      }
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    if (BATCH == this.getType()) {
      int size = this.batch.size();
      // We should never send an empty message.
      Assert.assertTrue(size > 0);
      out.writeInt(size);
      for (Tuple tuple : this.batch) {
        out.writeLong(tuple.respondTo.toLong());
        out.writeInt(tuple.result.code());
      }
    }
  }

  /**
   * The respondTo is the message to which we are responding.  The result determines if this is a RECEIVED, SUCCESS, or FAIL.
   */
  public static class Tuple {
    public final MessageID respondTo;
    public final ReplicationResultCode result;
    
    public Tuple(MessageID respondTo, ReplicationResultCode result) {
      this.respondTo = respondTo;
      this.result = result;
    }
  }
}
