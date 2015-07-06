package org.terracotta.entity;

import com.google.common.util.concurrent.Futures;

import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author twu
 */
public class PassthroughEndpoint implements EntityClientEndpoint {
  private final ClientDescriptor clientDescriptor = new FakeClientDescriptor();
  private ActiveServerEntity entity;
  private final Set<EndpointListener> listeners = Collections.newSetFromMap(new IdentityHashMap<>());
  private final ClientCommunicator clientCommunicator = new TestClientCommunicator();

  public PassthroughEndpoint(ActiveServerEntity entity) {
    attach(entity);
  }

  public PassthroughEndpoint() {}

  public void attach(ActiveServerEntity entity) {
    this.entity = entity;
    entity.connected(clientDescriptor);
  }

  @Override
  public byte[] getEntityConfiguration() {
    return entity.getConfig();
  }

  @Override
  public void registerListener(EndpointListener listener) {
    listeners.add(listener);
  }

  @Override
  public InvocationBuilder beginInvoke() {
    return new InvocationBuilderImpl();
  }

  private class FakeClientDescriptor implements ClientDescriptor {
  }

  private class InvocationBuilderImpl implements InvocationBuilder {
    private byte[] payload = null;
    private final Set<Acks> acks = EnumSet.noneOf(Acks.class);

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
        return Futures.immediateFuture(entity.invoke(clientDescriptor, payload));
      } catch (Exception e) {
        return Futures.immediateFailedCheckedFuture(e);
      }
    }
  }

  public ClientCommunicator clientCommunicator() {
    return clientCommunicator;
  }

  private class TestClientCommunicator implements ClientCommunicator {
    @Override
    public void sendNoResponse(ClientDescriptor clientDescriptor, byte[] payload) {
      if (clientDescriptor == PassthroughEndpoint.this.clientDescriptor) {
        for (EndpointListener listener : listeners) {
          listener.handleMessage(payload);
        }
      }
    }

    @Override
    public Future<Void> send(ClientDescriptor clientDescriptor, byte[] payload) {
      sendNoResponse(clientDescriptor, payload);
      return Futures.immediateFuture(null);
    }
  }

  @Override
  public void close() {
    // In a real implementation, this is where a call to the PlatformService, to clean up, would be.
  }
}
