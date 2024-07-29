/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import com.tc.bytes.TCByteBufferFactory;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import org.terracotta.entity.EntityMessage;

public class ReconfigureMessage implements VoltronEntityMessage {
  private final EntityDescriptor entityDescriptor;
  private final TCByteBuffer configuration;

  public ReconfigureMessage(EntityDescriptor entityDescriptor, byte[] configuration) {
    this.entityDescriptor = entityDescriptor;
    this.configuration = TCByteBufferFactory.wrap(configuration);
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
    return entityDescriptor;
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
    return Type.RECONFIGURE_ENTITY;
  }

  @Override
  public TCByteBuffer getExtendedData() {
    return configuration;
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