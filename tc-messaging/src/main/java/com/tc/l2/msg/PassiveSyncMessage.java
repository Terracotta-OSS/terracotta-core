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
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_BEGIN;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_END;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_ENTITY_BEGIN;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_ENTITY_CONCURRENCY_BEGIN;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_ENTITY_CONCURRENCY_END;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_ENTITY_CONCURRENCY_PAYLOAD;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_ENTITY_END;
import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;

/**
 * This is a convenience subclass of ReplicationMessage.
 */
public class PassiveSyncMessage extends ReplicationMessage {
  public static PassiveSyncMessage createStartSyncMessage() {
    return new PassiveSyncMessage(SYNC_BEGIN, EntityID.NULL_ID, NO_VERSION, 0, null);
  }
  public static PassiveSyncMessage createEndSyncMessage() {
    return new PassiveSyncMessage(SYNC_END, EntityID.NULL_ID, NO_VERSION, 0, null);
  }
  public static PassiveSyncMessage createStartEntityMessage(EntityID id, long version, byte[] configPayload) {
    return new PassiveSyncMessage(SYNC_ENTITY_BEGIN, id, version, 0, configPayload);
  }
  public static PassiveSyncMessage createEndEntityMessage(EntityID id, long version) {
    return new PassiveSyncMessage(SYNC_ENTITY_END, id, version, 0, null);
  }
  public static PassiveSyncMessage createStartEntityKeyMessage(EntityID id, long version, int concurrency) {
    // We can only synchronize positive-number keys.
    Assert.assertTrue(concurrency > 0);
    return new PassiveSyncMessage(SYNC_ENTITY_CONCURRENCY_BEGIN, id, version, concurrency, null);
  }
  public static PassiveSyncMessage createEndEntityKeyMessage(EntityID id, long version, int concurrency) {
    // We can only synchronize positive-number keys.    
    Assert.assertTrue(concurrency > 0);
    return new PassiveSyncMessage(SYNC_ENTITY_CONCURRENCY_END, id, version, concurrency, null);
  }
  public static PassiveSyncMessage createPayloadMessage(EntityID id, long version, int concurrency, byte[] payload) {
    // We can only synchronize positive-number keys.
    Assert.assertTrue(concurrency > 0);
    return new PassiveSyncMessage(SYNC_ENTITY_CONCURRENCY_PAYLOAD, id, version, concurrency, payload);
  }

  public static final long NO_VERSION = 0;
  
  public PassiveSyncMessage() {
    super(SYNC);
  }
  
  public PassiveSyncMessage(boolean start) {
    super(SYNC);
    initialize(new EntityDescriptor(EntityID.NULL_ID, ClientInstanceID.NULL_ID, NO_VERSION), 
        ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, start ? SYNC_BEGIN : SYNC_END, null, 0);
  }
  
  public PassiveSyncMessage(EntityID eid, long version, byte[] configPayload) {
    super(SYNC);
    initialize(new EntityDescriptor(eid, ClientInstanceID.NULL_ID, version), 
        ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, 
        configPayload != null ? SYNC_ENTITY_BEGIN : SYNC_ENTITY_END, configPayload, 0);
  }
  
  public PassiveSyncMessage(EntityID eid, long version, int concurrency) {
    super(SYNC);
    initialize(new EntityDescriptor(eid, ClientInstanceID.NULL_ID, version), ClientID.NULL_ID, 
        TransactionID.NULL_ID, TransactionID.NULL_ID, SYNC_ENTITY_CONCURRENCY_END, null, 0);
    this.concurrency = concurrency;
  }

  public PassiveSyncMessage(EntityID eid, long version, int concurrency, byte[] payload) {
    super(SYNC);
    initialize(new EntityDescriptor(eid, ClientInstanceID.NULL_ID, version), 
        ClientID.NULL_ID, null, null, 
        SYNC_ENTITY_CONCURRENCY_PAYLOAD, payload, concurrency);
  }  
  
  public PassiveSyncMessage(ReplicationMessage.ReplicationType type, EntityID id, long version, int concurrency, byte[] payload) {
    super(SYNC);
    initialize(new EntityDescriptor(id, ClientInstanceID.NULL_ID, version), 
        ClientID.NULL_ID, null, null, 
        type, payload, concurrency);
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    super.basicDeserializeFrom(in);
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    super.basicSerializeTo(out);
  }
  
  public EntityID getEntityID() {
    return getEntityDescriptor().getEntityID();
  }
}
