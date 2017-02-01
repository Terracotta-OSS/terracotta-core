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
     * Only used locally - called to remove a ManagedEntity from entity manager after destroy
     */
    LOCAL_ENTITY_GC,
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
    
    /**
     * The SYNC_START message is special in that it tells the passive to setup the synchronization machinery.
     */
    SYNC_START,
    
    /**
     * It is the SYNC_BEGIN message which actually contains the information to describe all the entities which will be
     *  synchronized, in order, and how to create them.
     * This is to preserve entity initialization order:  since new creates can happen during sync, and depend on
     *  entities which haven't yet been synced, we create all the entities at the start, leading them initialized but
     *  not synced until we eventually get around to synchronizing them, later.
     */
    SYNC_BEGIN,
    SYNC_END,
    
    /**
     * Tells the passive that the active is next going to begin syncing this entity.
     * NOTE:  This entity should have already been created in SYNC_BEGIN.
     */
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
  public static SyncReplicationActivity createFlushLocalPipelineMessage(EntityID eid, long version, boolean forDestroy) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, new EntityDescriptor(eid, ClientInstanceID.NULL_ID, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, forDestroy ? ActivityType.LOCAL_ENTITY_GC : ActivityType.FLUSH_LOCAL_PIPELINE, null, 0, referenceCount, "");
  }

  public static SyncReplicationActivity createOrderingPlaceholder(EntityDescriptor descriptor, ClientID src, TransactionID tid, TransactionID oldest, String debugId) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, descriptor, src, tid, oldest, ActivityType.ORDERING_PLACEHOLDER, null, 0, referenceCount, debugId);
  }

  public static SyncReplicationActivity createReplicatedMessage(EntityDescriptor descriptor, ClientID src, TransactionID tid, TransactionID oldest, ActivityType action, byte[] payload, int concurrency, String debugId) {
    // We shouldn't be using this helper for any of the specialized activity types.
    Assert.assertTrue(ActivityType.LOCAL_ENTITY_GC != action);
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
    return new SyncReplicationActivity(ActivityID.getNextID(), null, descriptor, src, tid, oldest, action, payload, concurrency, referenceCount, debugId);
  }

  public static SyncReplicationActivity createStartSyncMessage(SyncReplicationActivity.EntityCreationTuple[] tuplesForCreation) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), tuplesForCreation, EntityDescriptor.NULL_ID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_BEGIN, null, 0, referenceCount, "");
  }

  public static SyncReplicationActivity createEndSyncMessage(byte[] extras) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, EntityDescriptor.NULL_ID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_END, extras, 0, referenceCount, "");
  }

  public static SyncReplicationActivity createStartEntityMessage(EntityID id, long version, byte[] configPayload, int references) {
    return new SyncReplicationActivity(ActivityID.getNextID(), null, descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_BEGIN, configPayload, 0, references, "");
  }

  public static SyncReplicationActivity createEndEntityMessage(EntityID id, long version) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_END, null, 0, referenceCount, "");
  }

  public static SyncReplicationActivity createStartEntityKeyMessage(EntityID id, long version, int concurrency) {
    // We can only synchronize positive-number keys.
    Assert.assertTrue(concurrency > 0);
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN, null, concurrency, referenceCount, "");
  }

  public static SyncReplicationActivity createEndEntityKeyMessage(EntityID id, long version, int concurrency) {
    // We can only synchronize positive-number keys.    
    Assert.assertTrue(concurrency > 0);
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_CONCURRENCY_END, null, concurrency, referenceCount, "");
  }

  public static SyncReplicationActivity createPayloadMessage(EntityID id, long version, int concurrency, byte[] payload, String debugId) {
    // We can only synchronize positive-number keys.
    Assert.assertTrue(concurrency > 0);
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, descriptorWithoutClient(id, version), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD, payload, concurrency, referenceCount, debugId);
  }

  public static SyncReplicationActivity createStartMessage() {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, EntityDescriptor.NULL_ID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_START, null, 0, referenceCount, "");
  }

  private static EntityDescriptor descriptorWithoutClient(EntityID id, long version) {
    return new EntityDescriptor(id, ClientInstanceID.NULL_ID, version);
  }


  private final ActivityID id;
  final ActivityType action;
  private final EntityCreationTuple[] entitiesForSyncStart;
  final EntityDescriptor descriptor;
  final ClientID src;
  final TransactionID tid;
  final TransactionID oldest;

  final byte[] payload;
  final int concurrency;
  // NOTE:  referenceCount is only used by SYNC_ENTITY_BEGIN.
  final int referenceCount;

  final String debugId;

  private SyncReplicationActivity(ActivityID id, EntityCreationTuple[] entitiesForSyncStart, EntityDescriptor descriptor, ClientID src, TransactionID tid, TransactionID oldest, ActivityType action, byte[] payload, int concurrency, int referenceCount, String debugId) {
    Assert.assertNotNull(id);
    Assert.assertNotNull(action);
    if (ActivityType.SYNC_BEGIN == action) {
      Assert.assertNotNull(entitiesForSyncStart);
    } else {
      Assert.assertNull(entitiesForSyncStart);
    }
    Assert.assertNotNull(descriptor);
    Assert.assertNotNull(src);
    Assert.assertNotNull(tid);
    Assert.assertNotNull(oldest);
    
    this.id = id;
    this.action = action;
    this.entitiesForSyncStart = entitiesForSyncStart;
    this.descriptor = descriptor;
    this.src = src;
    this.tid = tid;
    this.oldest = oldest;
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

  public EntityCreationTuple[] getEntitiesToCreateForSync() {
    Assert.assertTrue(ActivityType.SYNC_BEGIN == this.action);
    return this.entitiesForSyncStart;
  }

  public byte[] getExtendedData() {
    Assert.assertTrue(ActivityType.SYNC_BEGIN != this.action);
    return payload;
  }
  
  public ClientID getSource() {
    Assert.assertTrue(ActivityType.SYNC_BEGIN != this.action);
    return src;
  }
  
  public TransactionID getTransactionID() {
    Assert.assertTrue(ActivityType.SYNC_BEGIN != this.action);
    return tid;
  }
  
  public TransactionID getOldestTransactionOnClient() {
    Assert.assertTrue(ActivityType.SYNC_BEGIN != this.action);
    return oldest;
  }

  public EntityDescriptor getEntityDescriptor() {
    return descriptor;
  }
  
  public EntityID getEntityID() {
    Assert.assertNotNull(this.descriptor);
    return descriptor.getEntityID();
  }
  
  public int getConcurrency() {
    // SYNC_ENTITY_BEGIN contains reference-count, not concurrency.
    Assert.assertFalse(ActivityType.SYNC_ENTITY_BEGIN == this.action);
    Assert.assertTrue(ActivityType.SYNC_BEGIN != this.action);
    return this.concurrency;
  }

  public int getReferenceCount() {
    // SYNC_ENTITY_BEGIN is the only case which has a reference count.
    Assert.assertTrue(ActivityType.SYNC_ENTITY_BEGIN == this.action);
    Assert.assertTrue(ActivityType.SYNC_BEGIN != this.action);
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
    // We should NOT be serializing local flush activities.
    Assert.assertTrue(ActivityType.LOCAL_ENTITY_GC != this.action);
        
    out.writeLong(this.id.id);
    out.writeInt(this.action.ordinal());
    
    // We take very different paths depending on our type.
    if (ActivityType.SYNC_BEGIN == this.action) {
      out.writeInt(this.entitiesForSyncStart.length);
      for (int i = 0; i < this.entitiesForSyncStart.length; ++i) {
        this.entitiesForSyncStart[i].serializeTo(out);
      }
    } else {
      this.descriptor.serializeTo(out);
      int sourceNodeType = this.src.getNodeType();
      Assert.assertTrue(NodeID.CLIENT_NODE_TYPE == sourceNodeType);
      out.write(sourceNodeType);
      this.src.serializeTo(out);
      out.writeLong(tid.toLong());
      out.writeLong(oldest.toLong());
      
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
    }
    String debugIdToWrite = (this.debugId != null) ? this.debugId : "";
    out.writeString(debugIdToWrite);
  }

  public static SyncReplicationActivity deserializeFrom(TCByteBufferInput in) throws IOException {
    ActivityID activityID = new ActivityID(in.readLong());
    ActivityType action = ActivityType.values()[in.readInt()];
    
    // We take very different paths depending on our type.
    EntityCreationTuple[] entitiesForSyncStart = null;
    EntityDescriptor descriptor = EntityDescriptor.NULL_ID;
    ClientID source = ClientID.NULL_ID;
    TransactionID tid = TransactionID.NULL_ID;
    TransactionID oldest = TransactionID.NULL_ID;
    byte[] payload = null;
    int concurrency = 0;
    int referenceCount = 0;
    if (ActivityType.SYNC_BEGIN == action) {
      int arraySize = in.readInt();
      entitiesForSyncStart = new EntityCreationTuple[arraySize];
      for (int i = 0; i < arraySize; ++i) {
        entitiesForSyncStart[i] = EntityCreationTuple.deserializeFrom(in);
      }
    } else {
      descriptor = EntityDescriptor.readFrom(in);
      int sourceNodeType = in.read();
      Assert.assertTrue(NodeID.CLIENT_NODE_TYPE == sourceNodeType);
      source =  new ClientID().deserializeFrom(in);
      tid = new TransactionID(in.readLong());
      oldest = new TransactionID(in.readLong());
      
      int length = in.readInt();
      payload = new byte[length];
      in.readFully(payload);
      
      // Note that we only pass concurrency key or reference count in certain cases.
      concurrency = 0;
      referenceCount = 0;
      if (ActivityType.SYNC_ENTITY_BEGIN == action) {
        referenceCount = in.readInt();
      } else {
        concurrency = in.readInt();
      }
    }
    String debug = in.readString();
    return new SyncReplicationActivity(activityID, entitiesForSyncStart, descriptor, source, tid, oldest, action, payload, concurrency, referenceCount, debug);
  }

  @Override
  public String toString() {
    return "SyncReplicationActivity{activityID=" + this.id + ", entityID=" + descriptor.getEntityID() + ", src=" + src + ", tid=" + tid + ", oldest=" + oldest + ", action=" + action + ", concurrency=" + concurrency + ", debug=" + debugId + '}';
  }


  public static class EntityCreationTuple {
    public final EntityID id;
    public final long version;
    public final byte[] configPayload;
    public final boolean canDelete;
    
    
    public EntityCreationTuple(EntityID id, long version, byte[] configPayload, boolean canDelete) {
      this.id = id;
      this.version = version;
      this.configPayload = configPayload;
      this.canDelete = canDelete;
    }
    
    public void serializeTo(TCByteBufferOutput out) {
      this.id.serializeTo(out);
      out.writeLong(this.version);
      if (null != this.configPayload) {
        out.writeInt(this.configPayload.length);
        out.write(this.configPayload);
      } else {
        out.writeInt(0);
      }
      out.writeBoolean(this.canDelete);
    }
    public static EntityCreationTuple deserializeFrom(TCByteBufferInput in) throws IOException {
      EntityID id = EntityID.readFrom(in);
      long version = in.readLong();
      int configLength = in.readInt();
      byte[] configPayload = new byte[configLength];
      in.readFully(configPayload);
      boolean canDelete = in.readBoolean();
      return new EntityCreationTuple(id, version, configPayload, canDelete);
    }
  }
}
