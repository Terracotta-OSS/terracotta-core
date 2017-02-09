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

import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.entity.ClientDescriptorImpl;
import com.tc.util.Assert;

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class EntityClientCommunicatorService implements ClientCommunicator {
  private final ConcurrentMap<NodeID, ClientAccount> clientAccounts;
  private final ManagedEntity owningEntity;
  

  public EntityClientCommunicatorService(ConcurrentMap<NodeID, ClientAccount> clientAccounts, ManagedEntity owningEntity) {
    Assert.assertNotNull(clientAccounts);
    Assert.assertNotNull(owningEntity);
    
    this.clientAccounts = clientAccounts;
    this.owningEntity = owningEntity;
  }

  @Override
  public void sendNoResponse(ClientDescriptor clientDescriptor, EntityResponse message) throws MessageCodecException {
    // We are in internal code so downcast the descriptor.
    ClientDescriptorImpl rawDescriptor = (ClientDescriptorImpl)clientDescriptor;
    ClientAccount clientAccount = clientAccounts.get(rawDescriptor.getNodeID());
    if (clientAccount != null) {
      ClientInstanceID clientInstance = rawDescriptor.getClientInstanceID();
      byte[] payload = serialize(this.owningEntity.getCodec(), message);
      clientAccount.sendNoResponse(clientInstance, payload);
    }
  }

  @Override
  public Future<Void> send(ClientDescriptor clientDescriptor, EntityResponse message) throws MessageCodecException {
    // We are in internal code so downcast the descriptor.
    ClientDescriptorImpl rawDescriptor = (ClientDescriptorImpl)clientDescriptor;
    ClientAccount clientAccount = clientAccounts.get(rawDescriptor.getNodeID());
    if (clientAccount != null) {
      ClientInstanceID clientInstance = rawDescriptor.getClientInstanceID();
      byte[] payload = serialize(this.owningEntity.getCodec(), message);
      return clientAccount.send(clientInstance, payload);
    } else {
      return new Future<Void>() {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
          return false;
        }

        @Override
        public boolean isCancelled() {
          return false;
        }

        @Override
        public boolean isDone() {
          return true;
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
          return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
          return null;
        }
      };
    }
  }

  @SuppressWarnings("unchecked")
  private <R extends EntityResponse> byte[] serialize(MessageCodec<?, R> codec, EntityResponse response) throws MessageCodecException {
    // We do this downcast, inline, instead of asking the codec (since a safer cast is all it could do, anyway).
    // This should be safe as we received this object from an entity using this codec. 
    return codec.encodeResponse((R)response);
  }
}
