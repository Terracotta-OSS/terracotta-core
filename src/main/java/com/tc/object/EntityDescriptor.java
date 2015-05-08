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
  public static final EntityDescriptor NULL_ID = new EntityDescriptor(EntityID.NULL_ID, ClientInstanceID.NULL_ID);


  private final EntityID entityID;
  private final ClientInstanceID clientInstanceID;
  
  public EntityDescriptor(EntityID entityID, ClientInstanceID clientInstanceID) {
    this.entityID = entityID;
    this.clientInstanceID = clientInstanceID;
  }
  
  public EntityID getEntityID() {
    return this.entityID;
  }
  
  public ClientInstanceID getClientInstanceID() {
    return this.clientInstanceID;
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
    // TODO:  Serialize the clientInstanceID once we are passing it over the wire.
  }

  @Override
  public EntityDescriptor deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    // Note that this case shouldn't be called - uses the receiving instance as a factory and returns another instance.
    return readFrom(serialInput);
  }

  public static EntityDescriptor readFrom(TCByteBufferInput serialInput) throws IOException {
    // TODO:  read the clientInstanceID from serialInput once we are passing it over the wire.
    return new EntityDescriptor(EntityID.readFrom(serialInput), ClientInstanceID.NULL_ID);
  }
}
