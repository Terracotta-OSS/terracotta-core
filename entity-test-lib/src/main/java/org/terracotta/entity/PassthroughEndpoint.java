package org.terracotta.entity;

import com.google.common.util.concurrent.Futures;
import com.tc.entity.Request;

import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author twu
 */
public class PassthroughEndpoint implements EntityClientEndpoint {
  private final ClientID clientID = new FakeClientID();
  private ServerEntity entity;
  private final Set<EndpointListener> listeners = Collections.newSetFromMap(new IdentityHashMap<>());
  private final ClientCommunicator clientCommunicator = new TestClientCommunicator();

  public PassthroughEndpoint(ServerEntity entity) {
    attach(entity);
  }

  public PassthroughEndpoint() {}

  public void attach(ServerEntity entity) {
    this.entity = entity;
    entity.connected(clientID);
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

  private class FakeClientID implements ClientID {
  }

  private class InvocationBuilderImpl implements InvocationBuilder {
    private byte[] payload = null;
    private final Set<Request.Acks> acks = EnumSet.noneOf(Request.Acks.class);

    @Override
    public InvocationBuilder ackReceipt() {
      acks.add(Request.Acks.RECEIPT);
      return this;
    }

    @Override
    public InvocationBuilder ackReplicated() {
      acks.add(Request.Acks.REPLICATED);
      return this;
    }

    @Override
    public InvocationBuilder ackLogged() {
      acks.add(Request.Acks.PERSIST_IN_SEQUENCER);
      return this;
    }

    @Override
    public InvocationBuilder ackCompleted() {
      acks.add(Request.Acks.APPLIED);
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
        return Futures.immediateFuture(entity.invoke(clientID, payload));
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
    public void sendNoResponse(ClientID clientID, byte[] payload) {
      if (clientID == PassthroughEndpoint.this.clientID) {
        for (EndpointListener listener : listeners) {
          listener.handleMessage(payload);
        }
      }
    }

    @Override
    public Future<Void> send(ClientID clientID, byte[] payload) {
      sendNoResponse(clientID, payload);
      return Futures.immediateFuture(null);
    }
  }

  @Override
  public void close() {
    // In a real implementation, this is where a call to the PlatformService, to clean up, would be.
  }
}
