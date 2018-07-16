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
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.StateDumpCollector;


public class CommunicatorService implements ImplementationProvidedServiceProvider, DSOChannelManagerEventListener {
  private final ConcurrentMap<NodeID, ClientAccount> clientAccounts = new ConcurrentHashMap<>();
  private final ClientMessageSender sender;
  private boolean serverIsActive;
  // We have late-bound logic so make sure that is called.
  private boolean wasInitialized;

  public CommunicatorService(ClientMessageSender sender) {
    this.sender = sender;
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    ImplementationProvidedServiceProvider.super.addStateTo(stateDumpCollector); 
  }

  @Override
  public void channelCreated(MessageChannel channel) {
    clientAccounts.put(channel.getRemoteNodeID(), new ClientAccount(sender, channel));
  }

  @Override
  public void channelRemoved(MessageChannel channel, boolean wasActive) {
    ClientAccount clientAccount = clientAccounts.remove(channel.getRemoteNodeID());
    if (clientAccount != null) {
      clientAccount.close();
    }
  }
  
  @Override
  public <T> T getService(long consumerID, ManagedEntity owningEntity, ServiceConfiguration<T> configuration) {
    Assert.assertTrue(this.wasInitialized);
    // This service can't be used for fake entities (this is a bug, not a usage error, since the only fake entities are internal).
    Assert.assertNotNull(owningEntity);
    T serviceToReturn = null;
    if (this.serverIsActive) {
      EntityClientCommunicatorService service = new EntityClientCommunicatorService(clientAccounts, owningEntity);
      serviceToReturn = configuration.getServiceType().cast(service);
    }
    return serviceToReturn;
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(ClientCommunicator.class);
  }

  public void close() {
    clientAccounts.values().stream().forEach(a->a.close());
    clientAccounts.clear();
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    // nothing to do
  }

  @Override
  public void serverDidBecomeActive() {
    Assert.assertTrue(this.wasInitialized);
    // The client communicator service is only enabled when we are active.
    this.serverIsActive = true;
  }

  public void initialized() {
    this.wasInitialized = true;
  }
}
