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
package com.tc.objectserver.entity;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;


public class ClientEntityStateManagerImpl implements ClientEntityStateManager {
  private final Multimap<ClientID, EntityDescriptor> clientStates = Multimaps.synchronizedMultimap(HashMultimap.create());
  private final Sink<VoltronEntityMessage> voltronSink;

  public ClientEntityStateManagerImpl(Sink<VoltronEntityMessage> voltronSink) {
    this.voltronSink = voltronSink;
  }

  @Override
  public void addReference(ClientID clientID, EntityDescriptor entityDescriptor) {
    boolean didAdd = clientStates.put(clientID, entityDescriptor);
    // We currently assume that we are being used precisely:  all add/remove calls are expected to have a specific meaning.
    Assert.assertTrue(didAdd);
  }

  @Override
  public void removeReference(ClientID clientID, EntityDescriptor entityDescriptor) {
    boolean didRemove = clientStates.remove(clientID, entityDescriptor);
    // We currently assume that we are being used precisely:  all add/remove calls are expected to have a specific meaning.
    Assert.assertTrue(didRemove);
  }

  @Override
  public void verifyNoReferences(EntityDescriptor entityDescriptor) {
    boolean doesContain = clientStates.containsValue(entityDescriptor);
    Assert.assertFalse(doesContain);
  }

  @Override
  public void channelCreated(MessageChannel channel) {
    // ignore it until something actually happens
  }

  @Override
  public void channelRemoved(MessageChannel channel) {
    NodeID node = channel.getRemoteNodeID();
    // We know that this is a remote client so make the down-cast.
    ClientID client = (ClientID) node;
    // Note that we will clean these up when the removal request comes through so leave the clientStates unchanged, for now.
    for (EntityDescriptor oneInstance : this.clientStates.get(client)) {
      this.voltronSink.addSingleThreaded(new RemovalMessage(client, oneInstance));
    }
  }

  private static class RemovalMessage implements VoltronEntityMessage {
    private static final byte[] EMPTY_EXTENDED_DATA = new byte[0];
    private final ClientID clientID;
    private final EntityDescriptor entityDescriptor;

    public RemovalMessage(ClientID clientID, EntityDescriptor entityDescriptor) {
      this.clientID = clientID;
      this.entityDescriptor = entityDescriptor;
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
      return false;
    }

    @Override
    public Type getVoltronType() {
      return Type.RELEASE_ENTITY;
    }

    @Override
    public byte[] getExtendedData() {
      return EMPTY_EXTENDED_DATA;
    }

    @Override
    public TransactionID getOldestTransactionOnClient() {
      // This message isn't from a "client", in the traditional sense, so there isn't an "oldest transaction".
      // Since it is a disconnect, that means that this client can't end up in a reconnect scenario.  Therefore, we
      // will return null, here, and define that to mean that the client is no longer requiring persistent ordering.
      // Note that it may be worth making this a more explicit case in case other unexpected null cases are found.
      return null;
    }
  }
}
