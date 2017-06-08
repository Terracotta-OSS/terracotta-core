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

import com.google.common.util.concurrent.Futures;
import com.tc.classloader.BuiltinService;
import org.junit.Assert;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityServerException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Similar to the PassthroughEndpoint although designed to handle the broader cases of active/passive distinction,
 *  creation/destruction of entities, and multiple clients connected to one entity.
 */
public class PassthroughStripe<M extends EntityMessage, R extends EntityResponse> implements ClientCommunicator {

  private final EntityServerService<M, R> service;
  private final FakeServiceRegistry serviceRegistry = new FakeServiceRegistry();
  private final Map<String, ActiveServerEntity<M, R>> activeMap = new HashMap<String, ActiveServerEntity<M, R>>();
  private final Map<String, PassiveServerEntity<M, R>> passiveMap = new HashMap<String, PassiveServerEntity<M, R>>();
  private final Map<String, MessageCodec<M, R>> codecs = new HashMap<String, MessageCodec<M, R>>();
  private final Map<String, byte[]> configMap = new HashMap<String, byte[]>();
  private final Map<String, Integer> connectCountMap = new HashMap<String, Integer>();
  private final Map<ClientDescriptor, FakeEndpoint> endpoints = new HashMap<ClientDescriptor, FakeEndpoint>();
  
  private int nextClientID = 1;
  private int consumerID = 1;
  private AtomicLong txIdGenerator=new AtomicLong(0);
  private long eldestTxid=-1;

  public PassthroughStripe(EntityServerService<M, R> service, Class<?> clazz) {
    Assert.assertTrue(service.handlesEntityType(clazz.getName()));
    this.service = service;
  }
  
  public boolean createServerEntity(String name, byte[] configuration) throws ConfigurationException {
    boolean didCreate = false;
    if (!activeMap.containsKey(name)) {
      // Create the instances.
      MessageCodec<M, R> codec = service.getMessageCodec();
      ActiveServerEntity<M, R> active = service.createActiveEntity(serviceRegistry.create(consumerID++), configuration);
      PassiveServerEntity<M, R> passive = service.createPassiveEntity(serviceRegistry.create(consumerID++), configuration);
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
      endpoint.sendNoResponse(payload);
    } catch (MessageCodecException e) {
      // Not expected in the testing environment.
      Assert.fail(e.getLocalizedMessage());
    }
    return Futures.immediateFuture(null);
  }

  @Override
  public void closeClientConnection(ClientDescriptor clientDescriptor) {
    endpoints.get(clientDescriptor).close();
  }

  private class FakeServiceRegistry {
    private final Set<ServiceProvider> builtins = new HashSet<ServiceProvider>();
    
    FakeServiceRegistry() {
      java.util.ServiceLoader<ServiceProvider> loader = ServiceLoader.load(ServiceProvider.class);
      for (ServiceProvider provider : loader) {
        if (!provider.getClass().isAnnotationPresent(BuiltinService.class)) {
          System.err.println("service:" + provider.getClass().getName() + " not annotated with @BuiltinService.  The service will not be included");
        } else {
          builtins.add(provider);
        }
      }
      final List<Class<?>> selfTypes = new ArrayList<Class<?>>(1);
      selfTypes.add(ClientCommunicator.class);
//  add the ClientCommunicator builtin
      builtins.add(new ServiceProvider() {
        @Override
        public void dumpStateTo(StateDumper stateDumper) {

        }

        @Override
        public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
          return true;
        }

        @Override
        public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
          if (configuration.getServiceType().equals(ClientCommunicator.class)) {
            return configuration.getServiceType().cast(PassthroughStripe.this);
          }
          return null;
        }
//  weird 1.6 behavior that can't use Collections.singleton() or Arrays.asList()
        @Override
        public Collection<Class<?>> getProvidedServiceTypes() {
          return selfTypes;
        }

        @Override
        public void prepareForSynchronization() throws ServiceProviderCleanupException {
        }
      });
    }
    
    public ServiceRegistry create(final long cid) {
      return new ServiceRegistry() {
        @Override
        public <T> T getService(ServiceConfiguration<T> configuration) throws ServiceException {
          T rService = null;
          for (ServiceProvider provider : builtins) {
            if (provider.getProvidedServiceTypes().contains(configuration.getServiceType())) {
              T service = provider.getService(cid, configuration);
              if (service != null) {
                if (rService != null) {
                  throw new ServiceException("multiple services defined");
                } else {
                  rService = service;
                }
              }
            }
          }
          return rService;
        }

        @Override
        public <T> Collection<T> getServices(ServiceConfiguration<T> configuration) {
          List<T> choices = new ArrayList<T>();
          for (ServiceProvider provider : builtins) {
            if (provider.getProvidedServiceTypes().contains(configuration.getServiceType())) {
              T service = provider.getService(cid, configuration);
              if (service != null) {
                choices.add(service);
              }
            }
          }
          return choices;
        }
        
      };
    }
  }
  
  private class FakeEndpoint implements TxIdAwareClientEndpoint<M, R> {
    private EndpointDelegate delegate;
    private final String entityName;
    private final ClientDescriptor clientDescriptor;
    private final MessageCodec<M, R> codec;
    private AtomicLong currentId = new AtomicLong(0);
    private volatile long eldestid = -1;

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
          currentId.incrementAndGet(),
          eldestid,
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

    @Override
    public long getCurrentId() {
      return currentId.get();
    }

    @Override
    public long resetEldestId() {
      return eldestid;
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
    private final long eldestid;
    private final long currentId;
    private M request = null;

    public StripeInvocationBuilder(ClientDescriptor clientDescriptor,
                                   long currentId,
                                   long eldestid,
                                   ActiveServerEntity<M, R> activeServerEntity,
                                   PassiveServerEntity<M, R> passiveServerEntity,
                                   MessageCodec<M, R> codec) {
      this.clientDescriptor = clientDescriptor;
      this.currentId=currentId;
      this.eldestid=eldestid;
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
    public InvocationBuilder<M, R> blockGetOnRetire(boolean shouldBlock) {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvokeFuture<R> invoke() throws MessageCodecException {
      byte[] result = null;
      EntityException error = null;
      try {
        result = sendInvocation(currentId, eldestid, activeServerEntity, codec);
      } catch (EntityException e) {
        error = e;
      }
      return new ImmediateInvokeFuture<R>(codec.decodeResponse(result), error);
    }
    
    private byte[] sendInvocation(long currentId, long eldestId, ActiveServerEntity<M, R> entity, MessageCodec<M, R>
      codec) throws EntityException {
      byte[] result = null;
      try {
        R response = entity.invokeActive(clientDescriptor, currentId, eldestId, request);
        result = codec.encodeResponse(response);
      } catch (Exception e) {
        throw new EntityServerException(null, null, null, e);
      }
      return result;
    }
  }
}
