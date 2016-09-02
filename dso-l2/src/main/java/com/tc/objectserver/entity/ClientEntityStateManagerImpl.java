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
import com.tc.async.api.StageManager;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.util.Assert;
import java.util.ArrayList;
import java.util.List;
import org.terracotta.entity.EntityMessage;


public class ClientEntityStateManagerImpl implements ClientEntityStateManager {
  private final Multimap<ClientID, EntityDescriptor> clientStates = Multimaps.synchronizedMultimap(HashMultimap.create());
  private final StageManager stageManager;
  private final ManagementTopologyEventCollector collector;
  private final DSOChannelManagerEventListener clientChain;

  public ClientEntityStateManagerImpl(StageManager stageManager, ManagementTopologyEventCollector collector, DSOChannelManagerEventListener chain) {
    this.stageManager = stageManager;
    this.collector = collector;
    this.clientChain = chain;
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
  public boolean verifyNoReferences(EntityID eid) {
    return !clientStates.values().stream().anyMatch((ed)->ed.getEntityID().equals(eid));
  }

  @Override
  public void channelCreated(MessageChannel channel) {
    clientChain.channelCreated(channel);
  }

  @Override
  public void channelRemoved(MessageChannel channel) {
    NodeID node = channel.getRemoteNodeID();
    // We know that this is a remote client so make the down-cast.
    ClientID client = (ClientID) node;
    
    List<EntityDescriptor> list = new ArrayList(this.clientStates.get(client));
    collector.expectedReleases(client, list);
    Sink<VoltronEntityMessage> remover = stageManager.getStage(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE, VoltronEntityMessage.class).getSink();
    // Note that we will clean these up when the removal request comes through so leave the clientStates unchanged, for now.
    for (EntityDescriptor oneInstance : list) {
      remover.addSingleThreaded(new RemovalMessage(client, oneInstance));
    }
    clientChain.channelRemoved(channel);
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

    @Override
    public EntityMessage getEntityMessage() {
      // There is no message instance for this type.
      return null;
    }
  }
}
