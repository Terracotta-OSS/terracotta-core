/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
import org.terracotta.entity.EntityMessage;

/**
 *
 */

public class ReferenceMessage implements VoltronEntityMessage {
  private final ClientID clientID;
  private final EntityDescriptor entityDescriptor;
  private final VoltronEntityMessage.Type type;
  private final TCByteBuffer extendedData;

  public ReferenceMessage(ClientID clientID, boolean fetch, EntityDescriptor entityDescriptor, TCByteBuffer extendedData) {
    Assert.assertNotNull(extendedData);
    this.clientID = clientID;
    this.type = fetch ? VoltronEntityMessage.Type.FETCH_ENTITY : VoltronEntityMessage.Type.RELEASE_ENTITY;
    this.entityDescriptor = entityDescriptor;
    this.extendedData = extendedData == null || extendedData.isReadOnly() ? extendedData : extendedData.asReadOnlyBuffer();
  }

  @Override
  public ClientID getSource() {
    return clientID;
  }

  @Override
  public TransactionID getTransactionID() {
    return TransactionID.NULL_ID;
  }

  @Override
  public EntityDescriptor getEntityDescriptor() {
    return this.entityDescriptor;
  }

  @Override
  public boolean doesRequireReplication() {
    return true;
  }

  @Override
  public boolean doesRequestReceived() {
    return true;
  }

  @Override
  public boolean doesRequestRetired() {
    return false;
  }
  
  @Override
  public VoltronEntityMessage.Type getVoltronType() {
    return type;
  }

  @Override
  public TCByteBuffer getExtendedData() {
    return extendedData == null ? extendedData : extendedData.duplicate();
  }

  @Override
  public TransactionID getOldestTransactionOnClient() {
    // This message isn't from a "client", in the traditional sense, so there isn't an "oldest transaction".
    // Since it is a disconnect, that means that this client can't end up in a reconnect scenario.  Therefore, we
    // will return null, here, and define that to mean that the client is no longer requiring persistent ordering.
    // Note that it may be worth making this a more explicit case in case other unexpected null cases are found.
    return TransactionID.NULL_ID;
  }

  @Override
  public EntityMessage getEntityMessage() {
    // There is no message instance for this type.
    return null;
  }

  @Override
  public String toString() {
    return "ReferenceMessage{" + "clientID=" + clientID + ", entityDescriptor=" + entityDescriptor + ", type=" + type + '}';
  }
}