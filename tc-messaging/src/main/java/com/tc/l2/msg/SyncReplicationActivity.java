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
import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;


public class SyncReplicationActivity implements OrderedEventContext {
  public enum ActivityType {
    /**
     * An error/default value which should never actually be used.
     */
    INVALID,
    /**
     * Only used locally - called to ensure ordering but should never be replicated to a passive.
     */
    FLUSH_LOCAL_PIPELINE,
    /**
     * Used in the case where an invoke was issued but we don't want to run it on the passive.  It is, therefore, only
     *  run as a placeholder of ordering data (which matters for correctly ordering re-sends upon fail-over).
     */
    ORDERING_PLACEHOLDER,
    CREATE_ENTITY,
    RECONFIGURE_ENTITY,
    INVOKE_ACTION,
    DESTROY_ENTITY,
    FETCH_ENTITY,
    RELEASE_ENTITY,
    
    SYNC_START,  // The SYNC_START message is special in that it tells the passive to setup the synchronization machinery.
    SYNC_BEGIN,
    SYNC_END,
    SYNC_ENTITY_BEGIN,
    SYNC_ENTITY_END,
    SYNC_ENTITY_CONCURRENCY_BEGIN,
    SYNC_ENTITY_CONCURRENCY_PAYLOAD,
    SYNC_ENTITY_CONCURRENCY_END;
  }


  /**
   * This is the unique ID used for a SyncReplicationActivity so that it can be acked by the remote side.
   */
  public static class ActivityID {
    private static final AtomicLong NEXT_ID = new AtomicLong(1L);
    public static ActivityID getNextID() {
      return new ActivityID(NEXT_ID.getAndIncrement());
    }
    public final long id;
    public ActivityID(long id) {
      this.id = id;
    }
    @Override
    public boolean equals(Object obj) {
      boolean isEqual = (this == obj);
      if (!isEqual && (obj instanceof ActivityID)) {
        isEqual = this.id == ((ActivityID)obj).id;
      }
      return isEqual;
    }
    @Override
    public int hashCode() {
      return (int)this.id;
    }
  }


  // Factory methods.
  public static SyncReplicationActivity createFlushLocalPipelineMessage(EntityID eid, long version) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), new EntityDescriptor(eid, ClientInstanceID.NULL_ID, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.FLUSH_LOCAL_PIPELINE, null, 0, referenceCount, "");
  }

  public static SyncReplicationActivity createOrderingPlaceholder(EntityDescriptor descriptor, ClientID src, TransactionID tid, TransactionID oldest, String debugId) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), descriptor, src, tid, oldest, ActivityType.ORDERING_PLACEHOLDER, null, 0, referenceCount, debugId);
  }

  public static SyncReplicationActivity createReplicatedMessage(EntityDescriptor descriptor, ClientID src, TransactionID tid, TransactionID oldest, ActivityType action, byte[] payload, int concurrency, String debugId) {
    // We shouldn't be using this helper for any of the specialized activity types.
    Assert.assertTrue(ActivityType.FLUSH_LOCAL_PIPELINE != action);
    Assert.assertTrue(ActivityType.ORDERING_PLACEHOLDER != action);
    Assert.assertTrue(ActivityType.SYNC_BEGIN != action);
    Assert.assertTrue(ActivityType.SYNC_END != action);
    Assert.assertTrue(ActivityType.SYNC_ENTITY_BEGIN != action);
    Assert.assertTrue(ActivityType.SYNC_ENTITY_END != action);
    Assert.assertTrue(ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN != action);
    Assert.assertTrue(ActivityType.SYNC_ENTITY_CONCURRENCY_END != action);
    Assert.assertTrue(ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD != action);
    Assert.assertTrue(ActivityType.SYNC_START != action);
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), descriptor, src, tid, oldest, action, payload, concurrency, referenceCount, debugId);
  }

  public static SyncReplicationActivity createStartSyncMessage() {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), EntityDescriptor.NULL_ID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_BEGIN, null, 0, referenceCount, "");
  }

  public static SyncReplicationActivity createEndSyncMessage(byte[] extras) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), EntityDescriptor.NULL_ID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_END, extras, 0, referenceCount, "");
  }

  public static SyncReplicationActivity createStartEntityMessage(EntityID id, long version, byte[] configPayload, int references) {
    return new SyncReplicationActivity(ActivityID.getNextID(), descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_BEGIN, configPayload, 0, references, "");
  }

  public static SyncReplicationActivity createEndEntityMessage(EntityID id, long version) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_END, null, 0, referenceCount, "");
  }

  public static SyncReplicationActivity createStartEntityKeyMessage(EntityID id, long version, int concurrency) {
    // We can only synchronize positive-number keys.
    Assert.assertTrue(concurrency > 0);
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN, null, concurrency, referenceCount, "");
  }

  public static SyncReplicationActivity createEndEntityKeyMessage(EntityID id, long version, int concurrency) {
    // We can only synchronize positive-number keys.    
    Assert.assertTrue(concurrency > 0);
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_CONCURRENCY_END, null, concurrency, referenceCount, "");
  }

  public static SyncReplicationActivity createPayloadMessage(EntityID id, long version, int concurrency, byte[] payload, String debugId) {
    // We can only synchronize positive-number keys.
    Assert.assertTrue(concurrency > 0);
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD, payload, concurrency, referenceCount, debugId);
  }

  public static SyncReplicationActivity createStartMessage() {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), EntityDescriptor.NULL_ID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_START, null, 0, referenceCount, "");
  }

  private static EntityDescriptor descriptorWithoutClient(EntityID id, long version) {
    return new EntityDescriptor(id, ClientInstanceID.NULL_ID, version);
  }


  private final ActivityID id;
  private final EntityDescriptor descriptor;
  private final ClientID src;
  private final TransactionID tid;
  private final TransactionID oldest;

  private final ActivityType action;
  private final byte[] payload;
  private final int concurrency;
  // NOTE:  referenceCount is only used by SYNC_ENTITY_BEGIN.
  private final int referenceCount;

  private final String debugId;

  private SyncReplicationActivity(ActivityID id, EntityDescriptor descriptor, ClientID src, TransactionID tid, TransactionID oldest, ActivityType action, byte[] payload, int concurrency, int referenceCount, String debugId) {
    Assert.assertNotNull(id);
    Assert.assertNotNull(descriptor);
    Assert.assertNotNull(src);
    Assert.assertNotNull(tid);
    Assert.assertNotNull(oldest);
    Assert.assertNotNull(action);
    
    this.id = id;
    this.descriptor = descriptor;
    this.src = src;
    this.tid = tid;
    this.oldest = oldest;
    this.action = action;
    this.payload = payload;
    this.concurrency = concurrency;
    this.referenceCount = referenceCount;
    this.debugId = debugId;
  }

  @Override
  public long getSequenceID() {
    return this.id.id;
  }

  public ActivityID getActivityID() {
    return this.id;
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
    // SYNC_ENTITY_BEGIN contains reference-count, not concurrency.
    Assert.assertTrue(ActivityType.SYNC_ENTITY_BEGIN != this.action);
    return this.concurrency;
  }

  public int getReferenceCount() {
    // SYNC_ENTITY_BEGIN is the only case which has a reference count.
    Assert.assertTrue(ActivityType.SYNC_ENTITY_BEGIN == this.action);
    return this.referenceCount;
  }

  public ActivityType getActivityType() {
    return this.action;
  }

  public String getDebugID() {
    return this.debugId;
  }

  public boolean isSyncActivity() {
    return (this.action.ordinal() >= ActivityType.SYNC_START.ordinal());
  }

  protected void serializeTo(TCByteBufferOutput out) {
    // This activity better be valid.
    Assert.assertTrue(ActivityType.INVALID != this.action);
    // We should NOT be serializing local flush activities.
    Assert.assertTrue(ActivityType.FLUSH_LOCAL_PIPELINE != this.action);
    
    out.writeLong(this.id.id);
    this.descriptor.serializeTo(out);
    int sourceNodeType = this.src.getNodeType();
    Assert.assertTrue(NodeID.CLIENT_NODE_TYPE == sourceNodeType);
    out.write(sourceNodeType);
    this.src.serializeTo(out);
    out.writeLong(tid.toLong());
    out.writeLong(oldest.toLong());
    
    out.writeInt(this.action.ordinal());
    if (payload != null) {
      out.writeInt(payload.length);
      out.write(payload);
    } else {
      out.writeInt(0);
    }
    // Note that we only pass concurrency key or reference count in certain cases.
    if (ActivityType.SYNC_ENTITY_BEGIN == this.action) {
      out.writeInt(this.referenceCount);
    } else {
      out.writeInt(this.concurrency);
    }
    String debugIdToWrite = (this.debugId != null) ? this.debugId : "";
    out.writeString(debugIdToWrite);
  }

  public static SyncReplicationActivity deserializeFrom(TCByteBufferInput in) throws IOException {
    ActivityID activityID = new ActivityID(in.readLong());
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
    
    // Note that we only pass concurrency key or reference count in certain cases.
    int concurrency = 0;
    int referenceCount = 0;
    if (ActivityType.SYNC_ENTITY_BEGIN == action) {
      referenceCount = in.readInt();
    } else {
      concurrency = in.readInt();
    }
    
    String debug = in.readString();
    return new SyncReplicationActivity(activityID, descriptor, source, tid, oldest, action, payload, concurrency, referenceCount, debug);
  }

  @Override
  public String toString() {
    return "SyncReplicationActivity{activityID=" + this.id + ", entityID=" + descriptor.getEntityID() + ", src=" + src + ", tid=" + tid + ", oldest=" + oldest + ", action=" + action + ", concurrency=" + concurrency + ", debug=" + debugId + '}';
  }
}
