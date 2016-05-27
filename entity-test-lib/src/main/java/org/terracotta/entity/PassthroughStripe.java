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
public class PassthroughStripe<M extends EntityMessage, R extends EntityResponse> implements ClientCommunicator {

  private final ServerEntityService<M, R> service;
  private final FakeServiceRegistry serviceRegistry = new FakeServiceRegistry();
  private final Map<String, ActiveServerEntity<M, R>> activeMap = new HashMap<String, ActiveServerEntity<M, R>>();
  private final Map<String, PassiveServerEntity<M, R>> passiveMap = new HashMap<String, PassiveServerEntity<M, R>>();
  private final Map<String, MessageCodec<M, R>> codecs = new HashMap<String, MessageCodec<M, R>>();
  private final Map<String, byte[]> configMap = new HashMap<String, byte[]>();
  private final Map<String, Integer> connectCountMap = new HashMap<String, Integer>();
  private final Map<ClientDescriptor, FakeEndpoint> endpoints = new HashMap<ClientDescriptor, FakeEndpoint>();
  
  private int nextClientID = 1;

  public PassthroughStripe(ServerEntityService<M, R> service, Class<?> clazz) {
    Assert.assertTrue(service.handlesEntityType(clazz.getName()));
    this.service = service;
  }
  
  public boolean createServerEntity(String name, byte[] configuration) {
    boolean didCreate = false;
    if (!activeMap.containsKey(name)) {
      // Create the instances.
      MessageCodec<M, R> codec = service.getMessageCodec();
      ActiveServerEntity<M, R> active = service.createActiveEntity(serviceRegistry, configuration);
      PassiveServerEntity<M, R> passive = service.createPassiveEntity(serviceRegistry, configuration);
      // Set them as new instances.
      active.createNew();
      passive.createNew();
      // Store them for later lookup.
      activeMap.put(name, active);
      passiveMap.put(name, passive);
      codecs.put(name, codec);
      configMap.put(name, configuration);
      connectCountMap.put(name, 0);
      didCreate = true;
    }
    return didCreate;
  }
  
  public EntityClientEndpoint<M, R> connectNewClientToEntity(String name) {
    FakeEndpoint endpoint = null;
    if (activeMap.containsKey(name)) {
      ClientDescriptor descriptor = new FakeClientDescriptor(nextClientID);
      MessageCodec<M, R> codec = codecs.get(name);
      endpoint = getEndpoint(name, descriptor, codec);
      endpoints.put(descriptor, endpoint);
      nextClientID += 1;
      // Update the connect count.
      connectCountMap.put(name, connectCountMap.get(name).intValue() + 1);
    }
    return endpoint;
  }

  private FakeEndpoint getEndpoint(String name, ClientDescriptor descriptor, MessageCodec<M, R> codec) {
    return new FakeEndpoint(name, descriptor, codec);
  }
  

  @Override
  public void sendNoResponse(ClientDescriptor clientDescriptor, EntityResponse message) {
    FakeEndpoint endpoint = endpoints.get(clientDescriptor);
    byte[] payload = endpoint.serializeResponse(message);
    try {
      endpoint.sendNoResponse(payload);
    } catch (MessageCodecException e) {
      // Not expected in the testing environment.
      Assert.fail(e.getLocalizedMessage());
    }
  }

  @Override
  public Future<Void> send(ClientDescriptor clientDescriptor, EntityResponse message) {
    FakeEndpoint endpoint = endpoints.get(clientDescriptor);
    byte[] payload = endpoint.serializeResponse(message);
    try {
      endpoints.get(clientDescriptor).sendNoResponse(payload);
    } catch (MessageCodecException e) {
      // Not expected in the testing environment.
      Assert.fail(e.getLocalizedMessage());
    }
    return Futures.immediateFuture(null);
  }

  private class FakeServiceRegistry implements ServiceRegistry {
    @Override
    public <T> T getService(ServiceConfiguration<T> configuration) {
      return configuration.getServiceType().cast(PassthroughStripe.this);
    }
  }
  
  private class FakeEndpoint implements EntityClientEndpoint<M, R> {
    private EndpointDelegate delegate;
    private final String entityName;
    private final ClientDescriptor clientDescriptor;
    private final MessageCodec<M, R> codec;
    
    public FakeEndpoint(String name, ClientDescriptor clientDescriptor, MessageCodec<M, R> codec) {
      this.entityName = name;
      this.clientDescriptor = clientDescriptor;
      this.codec = codec;
    }

    @SuppressWarnings("unchecked")
    public byte[] serializeResponse(EntityResponse r) {
      byte[] raw = null;
      try {
        // The cast should be safe as r and this.codec are from the same implementation.
        raw = this.codec.encodeResponse((R)r);
      } catch (MessageCodecException e) {
        // Not expected in these tests.
        Assert.fail();
      }
      return raw;
    }

    public void sendNoResponse(byte[] payload) throws MessageCodecException {
      if (null != this.delegate) {
        R fromServer = this.codec.decodeResponse(payload);
        this.delegate.handleMessage(fromServer);
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
    public InvocationBuilder<M, R> beginInvoke() {
      return new StripeInvocationBuilder(
          this.clientDescriptor,
          PassthroughStripe.this.activeMap.get(this.entityName),
          PassthroughStripe.this.passiveMap.get(this.entityName),
          PassthroughStripe.this.codecs.get(this.entityName)
      );
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

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
      return ((FakeClientDescriptor)obj).id == this.id;
    }
  }

  private class StripeInvocationBuilder implements InvocationBuilder<M, R> {
    private final ClientDescriptor clientDescriptor;
    private final ActiveServerEntity<M, R> activeServerEntity;
    private final MessageCodec<M, R> codec;
    // Note that the passiveServerEntity is not yet used in tests related to this class.
    @SuppressWarnings("unused")
    private final PassiveServerEntity<M, R> passiveServerEntity;
    private M request = null;

    public StripeInvocationBuilder(ClientDescriptor clientDescriptor,
        ActiveServerEntity<M, R> activeServerEntity,
        PassiveServerEntity<M, R> passiveServerEntity,
        MessageCodec<M, R> codec
        ) {
      this.clientDescriptor = clientDescriptor;
      this.activeServerEntity = activeServerEntity;
      this.passiveServerEntity = passiveServerEntity;
      this.codec = codec;
    }

    @Override
    public InvocationBuilder<M, R> ackSent() {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder<M, R> ackReceived() {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder<M, R> ackCompleted() {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder<M, R> ackRetired() {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder<M, R> replicate(boolean requiresReplication) {
      // Replication ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder<M, R> message(M message) {
      this.request = message;
      return this;
    }

    @Override
    public InvocationBuilder<M, R> blockGetOnRetire() {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvokeFuture<R> invoke() throws MessageCodecException {
      byte[] result = null;
      EntityException error = null;
      try {
        result = sendInvocation(activeServerEntity, codec);
      } catch (EntityUserException e) {
        error = e;
      }
      return new ImmediateInvokeFuture<R>(codec.decodeResponse(result), error);
    }
    
    private byte[] sendInvocation(ActiveServerEntity<M, R> entity, MessageCodec<M, R> codec) throws EntityUserException {
      byte[] result = null;
      try {
        R response = entity.invoke(clientDescriptor, request);
        result = codec.encodeResponse(response);
      } catch (Exception e) {
        throw new EntityUserException(null, null, e);
      }
      return result;
    }
  }
}
