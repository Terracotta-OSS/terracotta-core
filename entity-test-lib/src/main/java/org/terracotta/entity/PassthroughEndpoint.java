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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.Future;

import org.junit.Assert;


public class PassthroughEndpoint implements EntityClientEndpoint {
  private final ClientDescriptor clientDescriptor = new FakeClientDescriptor();
  private ActiveServerEntity entity;
  private final Set<EndpointListener> listeners = Collections.newSetFromMap(new IdentityHashMap<EndpointListener, Boolean>());
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
    public Future<byte[]> invoke() {
      // Note that the passthrough end-point wants to preserve the semantics of a single-threaded server, no matter how
      // complicated the caller is (since multiple threads often are used to simulate multiple clients or multiple threads
      // using one client).
      // We will synchronize on the entity instance so it will only ever see one caller at a time, no matter how many
      // end-points connect to it.
      synchronized (entity) {
        try {
          return Futures.immediateFuture(entity.invoke(clientDescriptor, payload));
        } catch (Exception e) {
          return Futures.immediateFailedCheckedFuture(e);
        }
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

  @Override
  public void setReconnectHandler(EntityClientReconnectHandler handler) {
    // The reconnect handler isn't used in this case since there is no reconnect in this testing system.
    // We can safely ignore this.
  }

  @Override
  public byte[] getExtendedReconnectData() {
    // This should never be called since there is no reconnect.
    Assert.fail("Reconnect not supported");
    return null;
  }
}
