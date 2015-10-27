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
