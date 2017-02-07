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
package com.tc.object;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.util.Assert;

import java.io.IOException;

/**
 * An opaque token representing the connection of a server side resource to a client side fetch.  
 * This connection is bi-modal on the server side.  It either is a type/name coordinate or a fetchid
 * but not both.  fetchid descriptors can only be used for a fetched entity endpoint.  
 * Note that this is only meaningful within the context
 * of an already-known client.  This is of primary interest in the wire protocol along a given connection.
 */
public class EntityDescriptor implements TCSerializable<EntityDescriptor> {
  // We use 0 as the null version since it is invalid (versions are always expected to be > 0).
  public static final long INVALID_VERSION = 0;
  public static final EntityDescriptor NULL_ID = new EntityDescriptor(EntityID.NULL_ID, INVALID_VERSION);

  private final FetchID fetchID;
  private final EntityID entityID;
  private final ClientInstanceID clientInstanceID;
  // The version of the client-side entity implementation.
  private final long clientSideVersion;
  
  
  public static EntityDescriptor createDescriptorForLifecycle(EntityID entityID, long clientSideVersion) {
    return new EntityDescriptor(entityID, clientSideVersion);
  }
  
  public static EntityDescriptor createDescriptorForFetch(EntityID entityID, long clientSideVersion, ClientInstanceID instance) {
    return new EntityDescriptor(entityID, clientSideVersion, instance);
  }
  
  public static EntityDescriptor createDescriptorForInvoke(FetchID fetchID, ClientInstanceID clientInstanceID) {
    return new EntityDescriptor(fetchID, clientInstanceID);
  }
  
  private EntityDescriptor(EntityID entityID, long clientSideVersion) {
    this.fetchID = FetchID.NULL_ID;
    this.entityID = entityID;
    this.clientInstanceID = ClientInstanceID.NULL_ID;
    this.clientSideVersion = clientSideVersion;
  }
  
  private EntityDescriptor(EntityID entityID, long clientSideVersion, ClientInstanceID instance) {
    this.fetchID = FetchID.NULL_ID;
    this.entityID = entityID;
    this.clientInstanceID = instance;
    this.clientSideVersion = clientSideVersion;
  }
  
  private EntityDescriptor(FetchID fetchID, ClientInstanceID clientInstanceID) {
    this.fetchID = fetchID;
    this.entityID = EntityID.NULL_ID;
    this.clientInstanceID = clientInstanceID;
    this.clientSideVersion = -1L;
  }
  
  public boolean isIndexed() {
    return !this.fetchID.isNull();
  }
  
  public FetchID getFetchID() {
    Assert.assertTrue(isIndexed());
    return this.fetchID;
  }
  
  public EntityID getEntityID() {
    Assert.assertFalse(isIndexed());
    return this.entityID;
  }
  
  public ClientInstanceID getClientInstanceID() {
    return this.clientInstanceID;
  }
  
  public long getClientSideVersion() {
    return this.clientSideVersion;
  }
  
  @Override
  public int hashCode() {
    Assert.assertFalse(isIndexed());
    return this.entityID.hashCode() ^ this.clientInstanceID.hashCode();
  }
  
  @Override
  public boolean equals(Object other) {
    Assert.assertFalse(isIndexed());
    boolean doesMatch = (this == other);
    if (!doesMatch && (getClass() == other.getClass()))
    {
      final EntityDescriptor that = (EntityDescriptor) other;
      doesMatch = this.entityID.equals(that.entityID)
          && this.clientInstanceID.equals(that.clientInstanceID);
    }
    return doesMatch;
  }
  
  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(fetchID.toLong());
    if (fetchID.isNull()) {
      this.entityID.serializeTo(serialOutput);
      serialOutput.writeLong(this.clientSideVersion);
    }
    this.clientInstanceID.serializeTo(serialOutput);    
  }

  @Override
  public EntityDescriptor deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    // Note that this case shouldn't be called - uses the receiving instance as a factory and returns another instance.
    return readFrom(serialInput);
  }

  public static EntityDescriptor readFrom(TCByteBufferInput serialInput) throws IOException {
    long fetchID = serialInput.readLong();
    if (fetchID == FetchID.NULL_ID.toLong()) {
      return new EntityDescriptor(EntityID.readFrom(serialInput), serialInput.readLong(), ClientInstanceID.readFrom(serialInput));
    } else {
      return new EntityDescriptor(new FetchID(fetchID), ClientInstanceID.readFrom(serialInput));
    }
  }

  @Override
  public String toString() {
    if (fetchID.isNull()) {
      return "EntityDescriptor{" + "entityID=" + entityID + ", version=" + clientSideVersion + ", instance=" + clientInstanceID + '}';
    } else {
      return "EntityDescriptor{" + "fetchID=" + fetchID + ", instance=" + clientInstanceID + '}';
    }
  }
}
