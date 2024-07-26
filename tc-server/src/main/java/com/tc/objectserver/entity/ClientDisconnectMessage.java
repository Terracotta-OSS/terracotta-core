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
import java.util.function.Consumer;
import org.terracotta.entity.EntityMessage;

/**
 *
 */

public class ClientDisconnectMessage implements VoltronEntityMessage, Runnable {
  private final ClientID clientID;
  private final EntityDescriptor descriptor;
  private final Runnable disconnectComplete;
  private final Consumer<Exception> disconnectException;

  public ClientDisconnectMessage(ClientID clientID, EntityDescriptor entityID, Runnable completion, Consumer<Exception> exception) {
    this.clientID = clientID;
    this.descriptor = entityID;
    this.disconnectComplete = completion;
    this.disconnectException = exception;
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
    return descriptor;
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
    return Type.DISCONNECT_CLIENT;
  }

  @Override
  public TCByteBuffer getExtendedData() {
    return TCByteBufferFactory.getInstance( 0);
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
  public void run() {
    if (this.disconnectComplete != null) {
      this.disconnectComplete.run();
    }
  }
  
  public void disconnectException(Exception e) {
    if (this.disconnectException != null) {
      this.disconnectException.accept(e);
    }
  }
  
  @Override
  public String toString() {
    return "ClientDisconnectMessage{" + "clientID=" + clientID + ", entityDescriptor=" + descriptor + '}';
  }
}