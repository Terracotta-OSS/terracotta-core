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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.Futures;
import org.junit.Assert;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityUserException;


/**
 * Similar to the PassthroughEndpoint although designed to handle the broader cases of active/passive distinction,
 *  creation/destruction of entities, and multiple clients connected to one entity.
 */
public class PassthroughStripe implements ClientCommunicator {

  private final ServerEntityService<? extends ActiveServerEntity<?, ?>, ? extends PassiveServerEntity<?, ?>> service;
  private final FakeServiceRegistry serviceRegistry = new FakeServiceRegistry();
  private final Map<String, ActiveServerEntity<?, ?>> activeMap = new HashMap<String, ActiveServerEntity<?, ?>>();
  private final Map<String, PassiveServerEntity<?, ?>> passiveMap = new HashMap<String, PassiveServerEntity<?, ?>>();
  private final Map<String, byte[]> configMap = new HashMap<String, byte[]>();
  private final Map<String, Integer> connectCountMap = new HashMap<String, Integer>();
  private final Map<ClientDescriptor, FakeEndpoint> endpoints = new HashMap<ClientDescriptor, FakeEndpoint>();
  
  private int nextClientID = 1;

  public PassthroughStripe(ServerEntityService<? extends ActiveServerEntity<?, ?>, ? extends PassiveServerEntity<?, ?>> service, Class<?> clazz) {
    Assert.assertTrue(service.handlesEntityType(clazz.getName()));
    this.service = service;
  }
  
  public boolean createServerEntity(String name, byte[] configuration) {
    boolean didCreate = false;
    if (!activeMap.containsKey(name)) {
      // Create the instances.
      ActiveServerEntity<?, ?> active = service.createActiveEntity(serviceRegistry, configuration);
      PassiveServerEntity<?, ?> passive = service.createPassiveEntity(serviceRegistry, configuration);
      // Set them as new instances.
      active.createNew();
      passive.createNew();
      // Store them for later lookup.
      activeMap.put(name, active);
      passiveMap.put(name, passive);
      configMap.put(name, configuration);
      connectCountMap.put(name, 0);
      didCreate = true;
    }
    return didCreate;
  }
  
  public EntityClientEndpoint connectNewClientToEntity(String name) {
    FakeEndpoint endpoint = null;
    if (activeMap.containsKey(name)) {
      ClientDescriptor descriptor = new FakeClientDescriptor(nextClientID);
      endpoint = new FakeEndpoint(name, descriptor);
      endpoints.put(descriptor, endpoint);
      nextClientID += 1;
      // Update the connect count.
      connectCountMap.put(name, connectCountMap.get(name).intValue() + 1);
    }
    return endpoint;
  }

  @Override
  public void sendNoResponse(ClientDescriptor clientDescriptor, byte[] payload) {
    endpoints.get(clientDescriptor).sendNoResponse(payload);
  }

  @Override
  public Future<Void> send(ClientDescriptor clientDescriptor, byte[] payload) {
    endpoints.get(clientDescriptor).sendNoResponse(payload);
    return Futures.immediateFuture(null);
  }

  private class FakeServiceRegistry implements ServiceRegistry {
    @Override
    public <T> T getService(ServiceConfiguration<T> configuration) {
      return configuration.getServiceType().cast(PassthroughStripe.this);
    }
  }
  
  private class FakeEndpoint implements EntityClientEndpoint {
    private EndpointDelegate delegate;
    private final String entityName;
    private final ClientDescriptor clientDescriptor;
    
    public FakeEndpoint(String name, ClientDescriptor clientDescriptor) {
      this.entityName = name;
      this.clientDescriptor = clientDescriptor;
    }

    public void sendNoResponse(byte[] payload) {
      if (null != this.delegate) {
        this.delegate.handleMessage(payload);
      }
    }

    @Override
    public byte[] getEntityConfiguration() {
      return PassthroughStripe.this.configMap.get(this.entityName);
    }

    @Override
    public void setDelegate(EndpointDelegate delegate) {
      Assert.assertNull(this.delegate);
      this.delegate = delegate;
    }

    @Override
    public InvocationBuilder beginInvoke() {
      return new StripeInvocationBuilder(
          this.clientDescriptor,
          PassthroughStripe.this.activeMap.get(this.entityName),
          PassthroughStripe.this.passiveMap.get(this.entityName));
    }

    @Override
    public void close() {
      PassthroughStripe.this.connectCountMap.put(this.entityName, PassthroughStripe.this.connectCountMap.get(this.entityName).intValue() - 1);
    }

    @Override
    public byte[] getExtendedReconnectData() {
      return new byte[0];
    }

    @Override
    public void didCloseUnexpectedly() {
      Assert.fail("Not expecting this close");
    }
  }
  
  private class FakeClientDescriptor implements ClientDescriptor {
    private final int id;

    public FakeClientDescriptor(int id) {
      this.id = id;
    }

    @Override
    public int hashCode() {
      return this.id;
    }

    @Override
    public boolean equals(Object obj) {
      return ((FakeClientDescriptor)obj).id == this.id;
    }
  }

  private class StripeInvocationBuilder implements InvocationBuilder {
    private final ClientDescriptor clientDescriptor;
    private final ActiveServerEntity<?, ?> activeServerEntity;
    // Note that the passiveServerEntity is not yet used in tests related to this class.
    @SuppressWarnings("unused")
    private final PassiveServerEntity<?, ?> passiveServerEntity;
    private byte[] payload = null;

    public StripeInvocationBuilder(ClientDescriptor clientDescriptor,
        ActiveServerEntity<?, ?> activeServerEntity,
        PassiveServerEntity<?, ?> passiveServerEntity) {
      this.clientDescriptor = clientDescriptor;
      this.activeServerEntity = activeServerEntity;
      this.passiveServerEntity = passiveServerEntity;
    }

    @Override
    public InvocationBuilder ackSent() {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder ackReceived() {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder ackCompleted() {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder replicate(boolean requiresReplication) {
      // Replication ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder payload(byte[] payload) {
      this.payload = payload;
      return this;
    }

    @Override
    public InvokeFuture<byte[]> invoke() {
      byte[] result = null;
      EntityException error = null;
      try {
        result = sendInvocation(activeServerEntity);
      } catch (EntityUserException e) {
        error = e;
      }
      return new ImmediateInvokeFuture<byte[]>(result, error);
    }
    
    private <M extends EntityMessage, R extends EntityResponse> byte[] sendInvocation(ActiveServerEntity<M, R> entity) throws EntityUserException {
      byte[] result = null;
      try {
        result = entity.invoke(clientDescriptor, entity.getMessageCodec().deserialize(payload));
      } catch (Exception e) {
        throw new EntityUserException(null, null, e);
      }
      return result;
    }
  }
}
