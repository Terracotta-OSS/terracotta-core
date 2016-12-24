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

import com.tc.async.api.OrderedEventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.util.Assert;

import java.io.IOException;


public class ReplicationMessage extends AbstractGroupMessage implements OrderedEventContext {
//  message types  
  public static final int INVALID               = 0; // Invalid message type
  public static final int REPLICATE               = 1; // Sent to replicate a request on the passive
  public static final int SYNC               = 2; // Sent as part of a sync sequence

  // Factory methods.
  public static ReplicationMessage createActivityContainer(SyncReplicationActivity activity) {
    Assert.assertNotNull(activity);
    return new ReplicationMessage(activity);
  }

  public static ReplicationMessage createLocalContainer(SyncReplicationActivity activity) {
    Assert.assertNotNull(activity);
    ReplicationMessage message = new ReplicationMessage(activity);
    message.didCreateLocally = false;
    return message;
  }


  private SyncReplicationActivity activity;
  long rid = 0;
  // We will keep a flag to track whether this message is outgoing (created here and being sent to the network) or incoming
  //  (created elsewhere and decoded here) to ensure that it is being used correctly.
  // (Note that this check can be removed in the future - it is mostly to validate during refactoring and buffering
  //  implementation).
  private boolean didCreateLocally;
  
  public ReplicationMessage() {
    super(INVALID);
    this.didCreateLocally = false;
  }
  
  protected ReplicationMessage(int type) {
    super(type);
    this.didCreateLocally = false;
  }
  
//  a true replicated message
  private ReplicationMessage(SyncReplicationActivity activity) {
    super(activity.action.ordinal() >= SyncReplicationActivity.ActivityType.SYNC_START.ordinal() ? SYNC : REPLICATE);
    this.activity = activity;
    this.didCreateLocally = true;
  }
  
  public void setReplicationID(long rid) {
    this.rid = rid;
  }

  @Override
  public long getSequenceID() {
    return rid;
  }

  public SyncReplicationActivity getActivity() {
    // If this was created locally, we shouldn't be reaching into it to read the underlying activity - this is for the
    //  receiving side, only.
    Assert.assertFalse(this.didCreateLocally);
    return this.activity;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    int messageType = getType();
    switch (messageType) {
      case INVALID:
        // This message was not correctly initialized.
        Assert.fail();
        break;
      case REPLICATE:
      case SYNC:
        this.rid = in.readLong();
        this.activity = SyncReplicationActivity.deserializeFrom(in);
        Assert.assertNotNull(this.activity);
        // Make sure that the message type and activity type are consistent.
        if (this.activity.action.ordinal() >= SyncReplicationActivity.ActivityType.SYNC_START.ordinal()) {
          Assert.assertTrue(this.activity.action, SYNC == messageType);
        } else {
          Assert.assertTrue(this.activity.action, REPLICATE == messageType);
        }
        break;
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    int messageType = getType();
    switch (messageType) {
      case INVALID:
        // This message was not correctly initialized.
        Assert.fail();
        break;
      case REPLICATE:
      case SYNC:
        out.writeLong(rid);
        this.activity.serializeTo(out);
        break;
    }
  }
  
  public String getDebugId() {
    return this.getType() + " " + ((this.activity != null) ? (this.activity.debugId.length() == 0 ? this.activity.action : this.activity.debugId) : "");
  }

  @Override
  public String toString() {
    return "ReplicationMessage{rid=" + rid + ", activity=" + this.activity + "}";
  }
}
