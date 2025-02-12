/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.services;

import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.entity.ClientDescriptorImpl;
import com.tc.util.Assert;

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

import java.util.concurrent.ConcurrentMap;


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

  public void sendInvokeMessage(ClientDescriptor clientDescriptor, long invokeID, EntityResponse message) throws MessageCodecException {
    // We are in internal code so downcast the descriptor.
    ClientDescriptorImpl rawDescriptor = (ClientDescriptorImpl)clientDescriptor;
    ClientAccount clientAccount = clientAccounts.get(rawDescriptor.getNodeID());
    if (clientAccount != null) {
      TransactionID tid = new TransactionID(invokeID);
      byte[] payload = serialize(this.owningEntity.getCodec(), message);
      clientAccount.sendInvokeMessage(tid, payload);
    }
  }
  
  @Override
  public void closeClientConnection(ClientDescriptor clientDescriptor) {
    ClientDescriptorImpl descriptor = (ClientDescriptorImpl)clientDescriptor;
    clientAccounts.computeIfPresent(descriptor.getNodeID(), (node, account)->{account.close(); return null;});
  }

  @SuppressWarnings("unchecked")
  private <R extends EntityResponse> byte[] serialize(MessageCodec<?, R> codec, EntityResponse response) throws MessageCodecException {
    // We do this downcast, inline, instead of asking the codec (since a safer cast is all it could do, anyway).
    // This should be safe as we received this object from an entity using this codec. 
    return codec.encodeResponse((R)response);
  }
}
