/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import java.io.IOException;


/**
 * Used as part of the ClientHandshakeMessage, in the case of reconnect, to state a pre-existing client-entity reference
 * which must be re-established before the reconnect can be considered complete.
 */
public class ClientEntityReferenceContext implements TCSerializable<ClientEntityReferenceContext> {
  private EntityID entityID;
  private long entityVersion;
  private ClientInstanceID clientInstanceID;

  public ClientEntityReferenceContext() {
    // to make TCSerializable happy
  }

  public ClientEntityReferenceContext(EntityID entityID, long entityVersion, ClientInstanceID clientInstanceID) {
    this.entityID = entityID;
    this.entityVersion = entityVersion;
    this.clientInstanceID = clientInstanceID;
  }

  public EntityID getEntityID() {
    return this.entityID;
  }
  
  public long getEntityVersion() {
    return this.entityVersion;
  }
  
  public EntityDescriptor getEntityDescriptor() {
    return new EntityDescriptor(this.entityID, this.clientInstanceID, this.entityVersion);
  }

  @Override
  public boolean equals(Object o) {
    boolean isEqual = (this == o);
    if (!isEqual && (o instanceof ClientEntityReferenceContext)) {
      ClientEntityReferenceContext other = (ClientEntityReferenceContext) o;
      isEqual = this.entityID.equals(other.entityID)
          && (this.entityVersion == other.entityVersion)
          && this.clientInstanceID.equals(other.clientInstanceID);
    }
    return isEqual;
  }

  @Override
  public int hashCode() {
    return (13 * this.entityID.hashCode())
        ^ (7 * (int)(this.entityVersion))
        ^ (3 * this.clientInstanceID.hashCode());
  }

  @Override
  public void serializeTo(TCByteBufferOutput output) {
    this.entityID.serializeTo(output);
    output.writeLong(this.entityVersion);
    this.clientInstanceID.serializeTo(output);
  }

  @Override
  public ClientEntityReferenceContext deserializeFrom(TCByteBufferInput input) throws IOException {
    this.entityID = EntityID.readFrom(input);
    this.entityVersion = input.readLong();
    this.clientInstanceID = ClientInstanceID.readFrom(input);
    return this;
  }

  @Override
  public String toString() {
    return "ClientEntityReferenceContext [entityID=" + this.entityID
        + ", entityVersion=" + this.entityVersion
        + ", clientInstanceID=" + this.clientInstanceID
        + "]";
  }
}
