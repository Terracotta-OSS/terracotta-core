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
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;


public class SyncReplicationActivity {
  public enum ActivityType {
    NOOP,
    CREATE_ENTITY,
    RECONFIGURE_ENTITY,
    INVOKE_ACTION,
    DESTROY_ENTITY,
    FETCH_ENTITY,
    RELEASE_ENTITY,
    
    SYNC_BEGIN,
    SYNC_END,
    SYNC_ENTITY_BEGIN,
    SYNC_ENTITY_END,
    SYNC_ENTITY_CONCURRENCY_BEGIN,
    SYNC_ENTITY_CONCURRENCY_PAYLOAD,
    SYNC_ENTITY_CONCURRENCY_END;
  }
  
  // Factory methods.
  public static SyncReplicationActivity createNoOpMessage(EntityID eid, long version) {
    return new SyncReplicationActivity(new EntityDescriptor(eid, ClientInstanceID.NULL_ID, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.NOOP, null, 0, "");
  }

  public static SyncReplicationActivity createReplicatedMessage(EntityDescriptor descriptor, ClientID src, TransactionID tid, TransactionID oldest, ActivityType action, byte[] payload, int concurrency, String debugId) {
    return new SyncReplicationActivity(descriptor, src, tid, oldest, action, payload, concurrency, debugId);
  }

  public static SyncReplicationActivity createStartSyncMessage() {
    return new SyncReplicationActivity(EntityDescriptor.NULL_ID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_BEGIN, null, 0, "");
  }

  public static SyncReplicationActivity createEndSyncMessage(byte[] extras) {
    return new SyncReplicationActivity(EntityDescriptor.NULL_ID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_END, extras, 0, "");
  }

  public static SyncReplicationActivity createStartEntityMessage(EntityID id, long version, byte[] configPayload, int references) {
 //  repurposed concurrency id to tell passive if entity can be deleted 0 for deletable and 1 for not deletable
    return new SyncReplicationActivity(descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_BEGIN, configPayload, references, "");
  }

  public static SyncReplicationActivity createEndEntityMessage(EntityID id, long version) {
    return new SyncReplicationActivity(descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_END, null, 0, "");
  }

  public static SyncReplicationActivity createStartEntityKeyMessage(EntityID id, long version, int concurrency) {
    // We can only synchronize positive-number keys.
    Assert.assertTrue(concurrency > 0);
    return new SyncReplicationActivity(descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN, null, concurrency, "");
  }

  public static SyncReplicationActivity createEndEntityKeyMessage(EntityID id, long version, int concurrency) {
    // We can only synchronize positive-number keys.    
    Assert.assertTrue(concurrency > 0);
    return new SyncReplicationActivity(descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_CONCURRENCY_END, null, concurrency, "");
  }

  public static SyncReplicationActivity createPayloadMessage(EntityID id, long version, int concurrency, byte[] payload, String debugId) {
    // We can only synchronize positive-number keys.
    Assert.assertTrue(concurrency > 0);
    return new SyncReplicationActivity(descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD, payload, concurrency, debugId);
  }

  private static EntityDescriptor descriptorWithoutClient(EntityID id, long version) {
    return new EntityDescriptor(id, ClientInstanceID.NULL_ID, version);
  }


  EntityDescriptor descriptor;
  ClientID src;
  TransactionID tid;
  TransactionID oldest;

  ActivityType action;
  byte[] payload;
  int concurrency;

  String debugId;

  private SyncReplicationActivity(EntityDescriptor descriptor, ClientID src, TransactionID tid, TransactionID oldest, ActivityType action, byte[] payload, int concurrency, String debugId) {
    Assert.assertNotNull(descriptor);
    Assert.assertNotNull(src);
    Assert.assertNotNull(tid);
    Assert.assertNotNull(oldest);
    Assert.assertNotNull(action);
    
    this.descriptor = descriptor;
    this.src = src;
    this.tid = tid;
    this.oldest = oldest;
    this.action = action;
    this.payload = payload;
    this.concurrency = concurrency;
    this.debugId = debugId;
  }

  public byte[] getExtendedData() {
    return payload;
  }
  
  public ClientID getSource() {
    return src;
  }
  
  public TransactionID getTransactionID() {
    return tid;
  }
  
  public TransactionID getOldestTransactionOnClient() {
    return oldest;
  }

  public EntityDescriptor getEntityDescriptor() {
    return descriptor;
  }
  
  public EntityID getEntityID() {
    return descriptor == null ? EntityID.NULL_ID : descriptor.getEntityID();
  }
  
  public int getConcurrency() {
    return this.concurrency;
  }

  protected void serializeTo(TCByteBufferOutput out) {
    this.descriptor.serializeTo(out);
    int sourceNodeType = this.src.getNodeType();
    Assert.assertTrue(NodeID.CLIENT_NODE_TYPE == sourceNodeType);
    out.write(sourceNodeType);
    this.src.serializeTo(out);
    out.writeLong(tid.toLong());
    out.writeLong(oldest.toLong());
    
    out.writeInt(this.action.ordinal());
    if (payload != null && this.action != ActivityType.NOOP) {
      out.writeInt(payload.length);
      out.write(payload);
    } else {
      out.writeInt(0);
    }
    out.writeInt(concurrency);
    if (debugId == null) {
      debugId = "";
    }
    out.writeString(debugId);
  }

  public static SyncReplicationActivity deserializeFrom(TCByteBufferInput in) throws IOException {
    EntityDescriptor descriptor = EntityDescriptor.readFrom(in);
    int sourceNodeType = in.read();
    Assert.assertTrue(NodeID.CLIENT_NODE_TYPE == sourceNodeType);
    ClientID source =  new ClientID().deserializeFrom(in);
    TransactionID tid = new TransactionID(in.readLong());
    TransactionID oldest = new TransactionID(in.readLong());
    
    ActivityType action = ActivityType.values()[in.readInt()];
    int length = in.readInt();
    byte[] payload = new byte[length];
    in.readFully(payload);
    int concurrency = in.readInt();
    String debug = in.readString();
    return new SyncReplicationActivity(descriptor, source, tid, oldest, action, payload, concurrency, debug);
  }

  @Override
  public String toString() {
    return "SyncReplicationActivity{id=" + descriptor.getEntityID() + ", src=" + src + ", tid=" + tid + ", oldest=" + oldest + ", action=" + action + ", concurrency=" + concurrency + ", debug=" + debugId + '}';
  }
}
