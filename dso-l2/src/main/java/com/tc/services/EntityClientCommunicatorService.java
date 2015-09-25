package com.tc.services;

import com.google.common.util.concurrent.Futures;
import com.tc.net.NodeID;
import com.tc.object.EntityDescriptor;
import com.tc.objectserver.entity.ClientDescriptorImpl;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ServiceConfiguration;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

public class EntityClientCommunicatorService implements ClientCommunicator {
  private final ConcurrentMap<NodeID, ClientAccount> clientAccounts;

  public EntityClientCommunicatorService(ConcurrentMap<NodeID, ClientAccount> clientAccounts) {
    this.clientAccounts = clientAccounts;
  }

  @Override
  public void sendNoResponse(ClientDescriptor clientDescriptor, byte[] payload) {
    // We are in internal code so downcast the descriptor.
    ClientDescriptorImpl rawDescriptor = (ClientDescriptorImpl)clientDescriptor;
    ClientAccount clientAccount = clientAccounts.get(rawDescriptor.getNodeID());
    if (clientAccount != null) {
      EntityDescriptor entityDescriptor = rawDescriptor.getEntityDescriptor();
      clientAccount.sendNoResponse(entityDescriptor, payload);
    }
  }

  @Override
  public Future<Void> send(ClientDescriptor clientDescriptor, byte[] payload) {
    // We are in internal code so downcast the descriptor.
    ClientDescriptorImpl rawDescriptor = (ClientDescriptorImpl)clientDescriptor;
    ClientAccount clientAccount = clientAccounts.get(rawDescriptor.getNodeID());
    if (clientAccount != null) {
      EntityDescriptor entityDescriptor = rawDescriptor.getEntityDescriptor();
      return clientAccount.send(entityDescriptor, payload);
    } else {
      return Futures.immediateFuture(null);
    }
  }
}
