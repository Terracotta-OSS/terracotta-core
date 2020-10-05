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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.FetchID;
import com.tc.util.Assert;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class ClientEntityStateManagerImpl implements ClientEntityStateManager {
  private final Map<ClientDescriptorImpl, FetchID> clientStates = new ConcurrentHashMap<>();
  private static final Logger logger    = LoggerFactory.getLogger(ClientEntityStateManagerImpl.class);

  public ClientEntityStateManagerImpl() {
  }

  @Override
  public boolean addReference(ClientDescriptorImpl instance, FetchID eid) {
    Assert.assertFalse(instance.getClientInstanceID() == ClientInstanceID.NULL_ID);
    FetchID check = clientStates.put(instance, eid);
    return Objects.isNull(check);
  }

  @Override
  public boolean removeReference(ClientDescriptorImpl descriptor) {
    Assert.assertFalse(descriptor.getClientInstanceID() == ClientInstanceID.NULL_ID);
    FetchID eid = clientStates.remove(descriptor);
    return Objects.nonNull(eid);
  }

  @Override
  public boolean verifyNoEntityReferences(FetchID eid) {
    return !clientStates.values().stream().anyMatch((led)->led.equals(eid));
  }

  @Override
  public boolean verifyNoClientReferences(ClientID eid) {
    return !clientStates.keySet().stream().anyMatch((led)->led.getNodeID().equals(eid));
  }
  
  @Override
  public List<FetchID> clientDisconnected(ClientID client) {
    return clientStates.entrySet().stream()
        .filter(e->e.getKey().getNodeID().equals(client))
        .map(e->e.getValue())
        .distinct()
        .collect(Collectors.toList());
  }
  
  @Override
  public List<EntityDescriptor> clientDisconnectedFromEntity(ClientID client, FetchID entity) {
    return clientStates.entrySet().stream()
        .filter(e->(e.getKey().getNodeID().equals(client) && e.getValue().equals(entity)))
        .map(e->EntityDescriptor.createDescriptorForInvoke(e.getValue(), e.getKey().getClientInstanceID()))
        .collect(Collectors.toList());
  }

  @Override
  public Set<ClientID> clearClientReferences() {
    Set<ClientID> msgs = clientStates.keySet().stream().map(ClientDescriptorImpl::getNodeID).distinct().collect(Collectors.toSet());
    clientStates.clear();
    return msgs;
  }
}
