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
  private byte[] extendedReconnectData;

  public ClientEntityReferenceContext() {
    // to make TCSerializable happy
  }

  public ClientEntityReferenceContext(EntityID entityID, long entityVersion, ClientInstanceID clientInstanceID, byte[] extendedReconnectData) {
    this.entityID = entityID;
    this.entityVersion = entityVersion;
    this.clientInstanceID = clientInstanceID;
    this.extendedReconnectData = extendedReconnectData;
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

  public byte[] getExtendedReconnectData() {
    return this.extendedReconnectData;
  }

  @Override
  public boolean equals(Object o) {
    boolean isEqual = (this == o);
    if (!isEqual && (o instanceof ClientEntityReferenceContext)) {
      ClientEntityReferenceContext other = (ClientEntityReferenceContext) o;
      isEqual = this.entityID.equals(other.entityID)
          && (this.entityVersion == other.entityVersion)
          && this.clientInstanceID.equals(other.clientInstanceID);
      // Now, compare the extended data arrays.  This is arguably gratuitous but it makes tests easier to write and clearer.
      if (this.extendedReconnectData.length == other.extendedReconnectData.length) {
        int i = 0;
        while (isEqual && (i < this.extendedReconnectData.length)) {
          isEqual = (this.extendedReconnectData[i] == other.extendedReconnectData[i]);
          i += 1;
        }
      } else {
        isEqual = false;
      }
    }
    return isEqual;
  }

  @Override
  public int hashCode() {
    // We won't bother computing a hashcode for the extended data, here.
    return (13 * this.entityID.hashCode())
        ^ (7 * (int)(this.entityVersion))
        ^ (3 * this.clientInstanceID.hashCode());
  }

  @Override
  public void serializeTo(TCByteBufferOutput output) {
    this.entityID.serializeTo(output);
    output.writeLong(this.entityVersion);
    this.clientInstanceID.serializeTo(output);
    output.writeInt(this.extendedReconnectData.length);
    output.write(this.extendedReconnectData);
  }

  @Override
  public ClientEntityReferenceContext deserializeFrom(TCByteBufferInput input) throws IOException {
    this.entityID = EntityID.readFrom(input);
    this.entityVersion = input.readLong();
    this.clientInstanceID = ClientInstanceID.readFrom(input);
    int extendedDataLength = input.readInt();
    this.extendedReconnectData = new byte[extendedDataLength];
    input.readFully(this.extendedReconnectData);
    return this;
  }

  @Override
  public String toString() {
    return "ClientEntityReferenceContext [entityID=" + this.entityID
        + ", entityVersion=" + this.entityVersion
        + ", clientInstanceID=" + this.clientInstanceID
        + ", extendedData size=" + this.extendedReconnectData.length
        + "]";
  }
}
