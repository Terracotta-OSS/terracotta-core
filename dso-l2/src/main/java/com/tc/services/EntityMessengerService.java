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
package com.tc.services;

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ManagedEntity;


/**
 * Implements the IEntityMessenger interface by maintaining a "fake" EntityDescriptor (as there is no actual reference from
 * a client) and using that to send "fake" VoltronEntityMessage instances into the server's message sink.
 */
public class EntityMessengerService implements IEntityMessenger {
  private final Sink<VoltronEntityMessage> messageSink;
  private final MessageCodec<EntityMessage, ?> codec;
  private final EntityDescriptor fakeDescriptor;

  @SuppressWarnings("unchecked")
  public EntityMessengerService(Sink<VoltronEntityMessage> messageSink, ManagedEntity owningEntity) {
    this.messageSink = messageSink;
    // Note that the codec will actually expect to work on a sub-type of EntityMessage but this service isn't explicitly
    // given the actual type.  This means that incorrect usage will result in a runtime failure.
    this.codec = (MessageCodec<EntityMessage, ?>) owningEntity.getCodec();
    
    ClientInstanceID clientInstanceID = null;
    this.fakeDescriptor = new EntityDescriptor(owningEntity.getID(), clientInstanceID, owningEntity.getVersion());
  }

  @Override
  public void messageSelf(EntityMessage message) throws MessageCodecException {
    // We first serialize the message (note that this is partially so we can use the common message processor, which expects
    // to deserialize, but also because we may have to replicate the message to the passive).
    byte[] serializedMessage = this.codec.encodeMessage(message);
    FakeEntityMessage interEntityMessage = new FakeEntityMessage(this.fakeDescriptor, serializedMessage);
    this.messageSink.addSingleThreaded(interEntityMessage);
  }


  /**
   * We fake up a Voltron entity message to enqueue for the entity to process in the future.
   */
  private static class FakeEntityMessage implements VoltronEntityMessage {
    private final EntityDescriptor descriptor;
    private final byte[] message;

    public FakeEntityMessage(EntityDescriptor descriptor, byte[] message) {
      this.descriptor = descriptor;
      this.message = message;
    }
    @Override
    public ClientID getSource() {
      return null;
    }
    @Override
    public TransactionID getTransactionID() {
      return null;
    }
    @Override
    public EntityDescriptor getEntityDescriptor() {
      return this.descriptor;
    }
    @Override
    public boolean doesRequireReplication() {
      return true;
    }
    @Override
    public Type getVoltronType() {
      return Type.INVOKE_ACTION;
    }
    @Override
    public byte[] getExtendedData() {
      return this.message;
    }
    @Override
    public TransactionID getOldestTransactionOnClient() {
      return null;
    }
  }
}
