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
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.EnumSet;
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
  public static SyncReplicationActivity createFlushLocalPipelineMessage(FetchID fetch, boolean forDestroy) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, EntityID.NULL_ID, EntityDescriptor.INVALID_VERSION, fetch, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, forDestroy ? ActivityType.LOCAL_ENTITY_GC : ActivityType.FLUSH_LOCAL_PIPELINE, null, 0, referenceCount, null);
  }

  public static SyncReplicationActivity createOrderingPlaceholder(FetchID fetch, ClientID src, TransactionID tid, TransactionID oldest, String debugId) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, EntityID.NULL_ID, EntityDescriptor.INVALID_VERSION, fetch, src, tid, oldest, ActivityType.ORDERING_PLACEHOLDER, null, 0, referenceCount, debugId);
  }
  
  private static EnumSet<ActivityType> lifecycle = EnumSet.of(
      ActivityType.CREATE_ENTITY, 
      ActivityType.DESTROY_ENTITY,
      ActivityType.FETCH_ENTITY,
      ActivityType.RELEASE_ENTITY,
      ActivityType.RECONFIGURE_ENTITY);
      
  
  public static SyncReplicationActivity createLifecycleMessage(EntityID eid, long version, FetchID fetch, ClientID src, TransactionID tid, TransactionID oldest, ActivityType action, byte[] payload) {
    Assert.assertTrue(lifecycle.contains(action));
    return new SyncReplicationActivity(ActivityID.getNextID(),null,eid,version,fetch,src,tid,oldest,action,payload,0,0,null);
  }

  public static SyncReplicationActivity createInvokeMessage(FetchID fetchID, ClientID src, TransactionID tid, TransactionID oldest, ActivityType action, byte[] payload, int concurrency, String debugId) {
    // We shouldn't be using this helper for any of the specialized activity types.
    Assert.assertTrue(ActivityType.INVOKE_ACTION == action);
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, EntityID.NULL_ID, EntityDescriptor.INVALID_VERSION, fetchID, src, tid, oldest, action, payload, concurrency, referenceCount, debugId);
  }

  public static SyncReplicationActivity createStartSyncMessage(SyncReplicationActivity.EntityCreationTuple[] tuplesForCreation) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), tuplesForCreation, EntityID.NULL_ID, 0L, FetchID.NULL_ID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_BEGIN, null, 0, referenceCount, null);
  }

  public static SyncReplicationActivity createEndSyncMessage(byte[] extras) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, EntityID.NULL_ID, 0L, FetchID.NULL_ID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_END, extras, 0, referenceCount, null);
  }

  public static SyncReplicationActivity createStartEntityMessage(EntityID id, long version, FetchID fetchID, byte[] configPayload, int references) {
    return new SyncReplicationActivity(ActivityID.getNextID(), null, id, version, fetchID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_BEGIN, configPayload, 0, references, null);
  }

  public static SyncReplicationActivity createEndEntityMessage(EntityID id, long version, FetchID fetchID) {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, id, version, fetchID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_END, null, 0, referenceCount, null);
  }

  public static SyncReplicationActivity createStartEntityKeyMessage(EntityID id, long version, FetchID fetchID, int concurrency) {
    // We can only synchronize positive-number keys.
    Assert.assertTrue(concurrency > 0);
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, id, version, fetchID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN, null, concurrency, referenceCount, null);
  }

  public static SyncReplicationActivity createEndEntityKeyMessage(EntityID id, long version, FetchID fetchID, int concurrency) {
    // We can only synchronize positive-number keys.    
    Assert.assertTrue(concurrency > 0);
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, id, version, fetchID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_CONCURRENCY_END, null, concurrency, referenceCount, null);
  }

  public static SyncReplicationActivity createPayloadMessage(EntityID id, long version, FetchID fetchID, int concurrency, byte[] payload, String debugId) {
    // We can only synchronize positive-number keys.
    Assert.assertTrue(concurrency > 0);
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, id, version, fetchID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD, payload, concurrency, referenceCount, debugId);
  }

  public static SyncReplicationActivity createStartMessage() {
    int referenceCount = 0;
    return new SyncReplicationActivity(ActivityID.getNextID(), null, EntityID.NULL_ID, 0L, FetchID.NULL_ID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, ActivityType.SYNC_START, null, 0, referenceCount, null);
  }

  private final ActivityID id;
  final ActivityType action;
  private final EntityCreationTuple[] entitiesForSyncStart;
  final EntityID entityID;
  final long version;
  final ClientID src;
  final TransactionID tid;
  final TransactionID oldest;

  final byte[] payload;
  final int concurrency;
  // NOTE:  referenceCount is only used by SYNC_ENTITY_BEGIN.
  final int referenceCount;
  
  final FetchID fetchID;

  final String debugId;

  private SyncReplicationActivity(ActivityID id, EntityCreationTuple[] entitiesForSyncStart, EntityID entity, long version, FetchID fetch, ClientID src, TransactionID tid, TransactionID oldest, ActivityType action, byte[] payload, int concurrency, int referenceCount, String debugId) {
    Assert.assertNotNull(id);
    Assert.assertNotNull(action);
    if (ActivityType.SYNC_BEGIN == action) {
      Assert.assertNotNull(entitiesForSyncStart);
    } else {
      Assert.assertNull(entitiesForSyncStart);
    }

    Assert.assertNotNull(src);
    Assert.assertNotNull(tid);
    Assert.assertNotNull(oldest);
    
    this.id = id;
    this.action = action;
    this.entitiesForSyncStart = entitiesForSyncStart;
    this.entityID = entity;
    this.version = version;
    this.fetchID = fetch;
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
  
  public EntityID getEntityID() {
    return entityID;
  }

  public long getVersion() {
    return version;
  }

  public ClientID getSrc() {
    return src;
  }

  public FetchID getFetchID() {
    return fetchID;
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
      this.entityID.serializeTo(out);
      out.writeLong(version);
      out.writeLong(fetchID.toLong());
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
      if (this.debugId != null) {
        byte[] data = this.debugId.getBytes();
        out.writeInt(data.length);
        out.write(data);
      } else {
        out.writeInt(0);
      }
    }
  }

  public static SyncReplicationActivity deserializeFrom(TCByteBufferInput in) throws IOException {
    ActivityID activityID = new ActivityID(in.readLong());
    ActivityType action = ActivityType.values()[in.readInt()];
    
    // We take very different paths depending on our type.
    EntityCreationTuple[] entitiesForSyncStart = null;
    EntityID entityID = EntityID.NULL_ID;
    long version = 0L;
    FetchID fetchID = FetchID.NULL_ID;
    ClientID source = ClientID.NULL_ID;
    TransactionID tid = TransactionID.NULL_ID;
    TransactionID oldest = TransactionID.NULL_ID;
    byte[] payload = null;
    int concurrency = 0;
    int referenceCount = 0;
    String debug = null;
    if (ActivityType.SYNC_BEGIN == action) {
      int arraySize = in.readInt();
      entitiesForSyncStart = new EntityCreationTuple[arraySize];
      for (int i = 0; i < arraySize; ++i) {
        entitiesForSyncStart[i] = EntityCreationTuple.deserializeFrom(in);
      }
    } else {
      entityID = EntityID.readFrom(in);
      version = in.readLong();
      fetchID = new FetchID(in.readLong());
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
      int dlen = in.readInt();
      if (dlen > 0) {
        byte[] data = new byte[dlen];
        in.read(data);
        debug = new String(data);
      }
    }
    return new SyncReplicationActivity(activityID, entitiesForSyncStart, entityID, version, fetchID, source, tid, oldest, action, payload, concurrency, referenceCount, debug);
  }

  @Override
  public String toString() {
    return "SyncReplicationActivity{activityID=" + this.id + ", entityID=" + entityID + ", version=" + version + ", fetchID=" + fetchID + ", src=" + src + ", tid=" + tid + ", oldest=" + oldest + ", action=" + action + ", concurrency=" + concurrency + ", debug=" + debugId + '}';
  }


  public static class EntityCreationTuple {
    public final EntityID id;
    public final long version;
    public final long consumerID;
    public final byte[] configPayload;
    public final boolean canDelete;
    
    
    public EntityCreationTuple(EntityID id, long version, long consumerID, byte[] configPayload, boolean canDelete) {
      this.id = id;
      this.version = version;
      this.consumerID = consumerID;
      this.configPayload = configPayload;
      this.canDelete = canDelete;
    }
    
    public void serializeTo(TCByteBufferOutput out) {
      this.id.serializeTo(out);
      out.writeLong(this.version);
      out.writeLong(this.consumerID);
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
      long consumerID = in.readLong();
      int configLength = in.readInt();
      byte[] configPayload = new byte[configLength];
      in.readFully(configPayload);
      boolean canDelete = in.readBoolean();
      return new EntityCreationTuple(id, version, consumerID, configPayload, canDelete);
    }
  }
}
