package com.tc.objectserver.entity;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;


public class ClientEntityStateManagerImpl implements ClientEntityStateManager {
  private final Multimap<NodeID, EntityDescriptor> clientStates = Multimaps.synchronizedMultimap(HashMultimap.create());
  private final Sink<VoltronEntityMessage> voltronSink;

  public ClientEntityStateManagerImpl(Sink<VoltronEntityMessage> voltronSink) {
    this.voltronSink = voltronSink;
  }

  @Override
  public boolean addReference(NodeID nodeID, EntityDescriptor entityDescriptor) {
    return clientStates.put(nodeID, entityDescriptor);
  }

  @Override
  public boolean removeReference(NodeID nodeID, EntityDescriptor entityDescriptor) {
    return clientStates.remove(nodeID, entityDescriptor);
  }

  @Override
  public void channelCreated(MessageChannel channel) {
    // ignore it until something actually happens
  }

  @Override
  public void channelRemoved(MessageChannel channel) {
    for (EntityDescriptor oneInstance : clientStates.removeAll(channel.getRemoteNodeID())) {
      this.voltronSink.addSingleThreaded(new RemovalMessage(channel.getRemoteNodeID(), oneInstance));
    }
  }

  private static class RemovalMessage implements VoltronEntityMessage {
    private static final byte[] EMPTY_EXTENDED_DATA = new byte[0];
    private final NodeID clientID;
    private final EntityDescriptor entityDescriptor;

    public RemovalMessage(NodeID clientID, EntityDescriptor entityDescriptor) {
      this.clientID = clientID;
      this.entityDescriptor = entityDescriptor;
    }

    @Override
    public NodeID getSource() {
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
    public Type getType() {
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
