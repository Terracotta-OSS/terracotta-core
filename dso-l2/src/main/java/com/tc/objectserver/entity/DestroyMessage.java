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
 */
package com.tc.objectserver.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import org.terracotta.entity.EntityMessage;

/**
 *
 */

public class DestroyMessage implements VoltronEntityMessage {
  private final EntityDescriptor entityDescriptor;

  public DestroyMessage(EntityDescriptor entityDescriptor) {
    this.entityDescriptor = entityDescriptor;
  }

  @Override
  public ClientID getSource() {
    return ClientID.NULL_ID;
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
    return Type.DESTROY_ENTITY;
  }

  @Override
  public TCByteBuffer getExtendedData() {
    return TCByteBufferFactory.getInstance(false, 0);
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
}