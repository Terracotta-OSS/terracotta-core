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
import com.tc.net.ClientID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
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
    return message;
  }


  private SyncReplicationActivity activity;
  
  long rid = 0;
  
  public ReplicationMessage() {
    super(INVALID);
  }
  
  protected ReplicationMessage(int type) {
    super(type);
  }
  
//  a true replicated message
  private ReplicationMessage(SyncReplicationActivity activity) {
    super(activity.action.ordinal() >= SyncReplicationActivity.ActivityType.SYNC_START.ordinal() ? SYNC : REPLICATE);
    this.activity = activity;
  }
  
  public void setReplicationID(long rid) {
    this.rid = rid;
  }

  @Override
  public long getSequenceID() {
    return rid;
  }
  
  public long getVersion() {
    return this.activity.descriptor.getClientSideVersion();
  }
  
  public SyncReplicationActivity.ActivityType getReplicationType() {
    // One can only ask what type of replication activity this is if it is a sync or replication activity.
    Assert.assertNotNull(this.activity);
    return this.activity.action;
  }

  public byte[] getExtendedData() {
    return this.activity.payload;
  }
  
  public ClientID getSource() {
    return this.activity.src;
  }
  
  public TransactionID getTransactionID() {
    return this.activity.tid;
  }
  
  public TransactionID getOldestTransactionOnClient() {
    return this.activity.oldest;
  }

  public EntityDescriptor getEntityDescriptor() {
    return this.activity.descriptor;
  }
  
  public EntityID getEntityID() {
    return this.activity.descriptor == null ? EntityID.NULL_ID : this.activity.descriptor.getEntityID();
  }
  
  public int getConcurrency() {
    return this.activity.concurrency;
  }
  
  /**
   * NOTE:  This is temporary while SyncReplicationActivity is further decoupled from ReplicationMessage.
   * 
   * @return The ActivityID of the underlying activity (fails with NPE if not wrapping an activity).
   */
  public SyncReplicationActivity.ActivityID getActivityID() {
    return this.activity.getActivityID();
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
