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
  public static final int START                = 3; // start replication

  // Factory methods.
  public static ReplicationMessage createStartMessage() {
    return new ReplicationMessage(START);
  }

  public static ReplicationMessage createNoOpMessage(EntityID eid, long version) {
    SyncReplicationActivity activity = SyncReplicationActivity.createNoOpMessage(eid, version);
    return new ReplicationMessage(activity);
  }

  public static ReplicationMessage createReplicatedMessage(EntityDescriptor descriptor, ClientID src, TransactionID tid, TransactionID oldest, SyncReplicationActivity.ActivityType action, byte[] payload, int concurrency, String debugId) {
    SyncReplicationActivity activity = SyncReplicationActivity.createReplicatedMessage(descriptor, src, tid, oldest, action, payload, concurrency, debugId);
    return new ReplicationMessage(activity);
  }

  // Sync-related factory methods (here temporarily while this message type is refactored to permit batching).
  public static ReplicationMessage createStartSyncMessage() {
    SyncReplicationActivity activity = SyncReplicationActivity.createStartSyncMessage();
    return new ReplicationMessage(activity);
  }
  public static ReplicationMessage createEndSyncMessage(byte[] extras) {
    SyncReplicationActivity activity = SyncReplicationActivity.createEndSyncMessage(extras);
    return new ReplicationMessage(activity);
  }
  public static ReplicationMessage createStartEntityMessage(EntityID id, long version, byte[] configPayload, int references) {
 //  repurposed concurrency id to tell passive if entity can be deleted 0 for deletable and 1 for not deletable
    SyncReplicationActivity activity = SyncReplicationActivity.createStartEntityMessage(id, version, configPayload, references);
    return new ReplicationMessage(activity);
  }
  public static ReplicationMessage createEndEntityMessage(EntityID id, long version) {
    SyncReplicationActivity activity = SyncReplicationActivity.createEndEntityMessage(id, version);
    return new ReplicationMessage(activity);
  }
  public static ReplicationMessage createStartEntityKeyMessage(EntityID id, long version, int concurrency) {
    // We can only synchronize positive-number keys.
    Assert.assertTrue(concurrency > 0);
    SyncReplicationActivity activity = SyncReplicationActivity.createStartEntityKeyMessage(id, version, concurrency);
    return new ReplicationMessage(activity);
  }
  public static ReplicationMessage createEndEntityKeyMessage(EntityID id, long version, int concurrency) {
    // We can only synchronize positive-number keys.    
    Assert.assertTrue(concurrency > 0);
    SyncReplicationActivity activity = SyncReplicationActivity.createEndEntityKeyMessage(id, version, concurrency);
    return new ReplicationMessage(activity);
  }
  public static ReplicationMessage createPayloadMessage(EntityID id, long version, int concurrency, byte[] payload, String debugId) {
    // We can only synchronize positive-number keys.
    Assert.assertTrue(concurrency > 0);
    SyncReplicationActivity activity = SyncReplicationActivity.createPayloadMessage(id, version, concurrency, payload, debugId);
    return new ReplicationMessage(activity);
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
    super(activity.action.ordinal() >= SyncReplicationActivity.ActivityType.SYNC_BEGIN.ordinal() ? SYNC : REPLICATE);
    this.activity = activity;
  }

  /**
   * This is a temporary method (until the underlying activities are being managed, directly).
   */
  public void setSingleActivityToNoOp() {
    this.activity = SyncReplicationActivity.createNoOpMessage(this.activity.getEntityID(), this.activity.getEntityDescriptor().getClientSideVersion());
    getType();
    Assert.assertNotNull(this.activity);
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
  
  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    int messageType = getType();
    switch (messageType) {
      case INVALID:
        // This message was not correctly initialized.
        Assert.fail();
        break;
      case START:
     // do nothing, just need the source
        break;
      case REPLICATE:
      case SYNC:
        this.rid = in.readLong();
        this.activity = SyncReplicationActivity.deserializeFrom(in);
        Assert.assertNotNull(this.activity);
        // Make sure that the message type and activity type are consistent.
        if (this.activity.action.ordinal() >= SyncReplicationActivity.ActivityType.SYNC_BEGIN.ordinal()) {
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
      case START:
     // do nothing, just need the source
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
