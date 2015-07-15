/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.ClientInstanceID;

import java.io.IOException;

/**
 * An opaque token representing a specific client-side entity instance.  Note that this is only meaningful within the context
 * of an already-known client.  This is of primary interest in the wire protocol along a given connection.
 */
public class EntityDescriptor implements TCSerializable<EntityDescriptor> {
  // We use 0 as the null version since it is invalid (versions are always expected to be > 0).
  public static final long INVALID_VERSION = 0;
  public static final EntityDescriptor NULL_ID = new EntityDescriptor(EntityID.NULL_ID, ClientInstanceID.NULL_ID, INVALID_VERSION);


  private final EntityID entityID;
  private final ClientInstanceID clientInstanceID;
  // The version of the client-side entity implementation.
  private final long clientSideVersion;
  
  public EntityDescriptor(EntityID entityID, ClientInstanceID clientInstanceID, long clientSideVersion) {
    this.entityID = entityID;
    this.clientInstanceID = clientInstanceID;
    this.clientSideVersion = clientSideVersion;
  }
  
  public EntityID getEntityID() {
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
    return this.entityID.hashCode() ^ this.clientInstanceID.hashCode();
  }
  
  @Override
  public boolean equals(Object other) {
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
    this.entityID.serializeTo(serialOutput);
    this.clientInstanceID.serializeTo(serialOutput);
    serialOutput.writeLong(this.clientSideVersion);
  }

  @Override
  public EntityDescriptor deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    // Note that this case shouldn't be called - uses the receiving instance as a factory and returns another instance.
    return readFrom(serialInput);
  }

  public static EntityDescriptor readFrom(TCByteBufferInput serialInput) throws IOException {
    return new EntityDescriptor(EntityID.readFrom(serialInput), ClientInstanceID.readFrom(serialInput), serialInput.readLong());
  }
}
