package org.terracotta.entity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.Futures;
import org.junit.Assert;


/**
 * Similar to the PassthroughEndpoint although designed to handle the broader cases of active/passive distinction,
 *  creation/destruction of entities, and multiple clients connected to one entity.
 */
public class PassthroughStripe<ID> implements Service<ClientCommunicator>, ClientCommunicator {

  private final ServerEntityService<ID ,? extends ActiveServerEntity, ? extends PassiveServerEntity> service;
  private final FakeServiceRegistry serviceRegistry = new FakeServiceRegistry();
  private final Map<String, ActiveServerEntity> activeMap = new HashMap<>();
  private final Map<String, PassiveServerEntity> passiveMap = new HashMap<>();
  private final Map<String, byte[]> configMap = new HashMap<>();
  private final Map<String, Integer> connectCountMap = new HashMap<>();
  private final Map<ClientDescriptor, FakeEndpoint> endpoints = new HashMap<>();
  
  private int nextClientID = 1;

  public PassthroughStripe(ServerEntityService<ID, ? extends ActiveServerEntity, ? extends PassiveServerEntity> service, Class<?> clazz) {
    Assert.assertTrue(service.handlesEntityType(clazz.getName()));
    this.service = service;
  }
  
  public boolean createServerEntity(ID id,String name, byte[] configuration) {
    boolean didCreate = false;
    if (!activeMap.containsKey(name)) {
      // Create the instances.
      ActiveServerEntity active = service.createActiveEntity(id, serviceRegistry, configuration);
      PassiveServerEntity passive = service.createPassiveEntity(id, serviceRegistry, configuration);
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

  @Override
  public void initialize(ServiceConfiguration<? extends ClientCommunicator> configuration) {
    //do nothing
  }

  @Override
  public ClientCommunicator get() {
    return this;
  }

  @Override
  public void destroy() {
    //TODO what should be done for this?
  }


  private class FakeServiceRegistry implements ServiceRegistry {
    @Override
    public Optional getService(ServiceConfiguration configuration) {
      return Optional.of(new Service() {

        @Override
        public Object get() {
          return PassthroughStripe.this;
        }

        @Override
        public void initialize(ServiceConfiguration configuration) {

        }

        @Override
        public void destroy() {

        }  
      });
    }

    @Override
    public void destroy() {
      // Not implemented for this test.
      Assert.fail();
    }
  }
  
  private class FakeEndpoint implements EntityClientEndpoint {
    private final List<EndpointListener> listeners = new Vector<>();
    private final String entityName;
    private final ClientDescriptor clientDescriptor;
    
    public FakeEndpoint(String name, ClientDescriptor clientDescriptor) {
      this.entityName = name;
      this.clientDescriptor = clientDescriptor;
    }

    public void sendNoResponse(byte[] payload) {
      for (EndpointListener listener : listeners) {
        listener.handleMessage(payload);
      }
    }

    @Override
    public byte[] getEntityConfiguration() {
      return PassthroughStripe.this.configMap.get(this.entityName);
    }

    @Override
    public void registerListener(EndpointListener listener) {
      listeners.add(listener);
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
    private final ActiveServerEntity activeServerEntity;
    private final PassiveServerEntity passiveServerEntity;
    private byte[] payload = null;
    private final Set<Acks> acks = EnumSet.noneOf(Acks.class);

    public StripeInvocationBuilder(ClientDescriptor clientDescriptor,
        ActiveServerEntity activeServerEntity,
        PassiveServerEntity passiveServerEntity) {
      this.clientDescriptor = clientDescriptor;
      this.activeServerEntity = activeServerEntity;
      this.passiveServerEntity = passiveServerEntity;
    }

    @Override
    public InvocationBuilder ackReceipt() {
      acks.add(Acks.RECEIPT);
      return this;
    }

    @Override
    public InvocationBuilder ackReplicated() {
      acks.add(Acks.REPLICATED);
      return this;
    }

    @Override
    public InvocationBuilder ackLogged() {
      acks.add(Acks.PERSIST_IN_SEQUENCER);
      return this;
    }

    @Override
    public InvocationBuilder ackCompleted() {
      acks.add(Acks.APPLIED);
      return this;
    }

    @Override
    public InvocationBuilder payload(byte[] payload) {
      this.payload = payload;
      return this;
    }

    @Override
    public Future<byte[]> invoke() {
      try {
        Future<byte[]> activeResult = Futures.immediateFuture(activeServerEntity.invoke(clientDescriptor, payload));
        passiveServerEntity.invoke(payload);
        return activeResult;
      } catch (Exception e) {
        return Futures.immediateFailedCheckedFuture(e);
      }
    }
  }
}
