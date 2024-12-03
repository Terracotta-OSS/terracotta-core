/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.entity;

import org.junit.Assert;

import java.util.Set;
import java.util.concurrent.Future;

import static java.util.concurrent.CompletableFuture.completedFuture;


/**
 * Used for basic testing (not part of the in-process testing framework) of an entity, to act like the client end-point.
 */
public class PassthroughEndpoint<M extends EntityMessage, R extends EntityResponse> implements
  TxIdAwareClientEndpoint<M, R> {
  private final ClientDescriptor clientDescriptor = new FakeClientDescriptor();
  private MessageCodec<M, R> codec;
  private byte[] configuration;
  private EndpointDelegate<R> delegate;
  private final ClientCommunicator clientCommunicator = new TestClientCommunicator();
  private boolean isOpen;
  private volatile long eldest = -1L;

  public PassthroughEndpoint() {
    // We start in the open state.
    this.isOpen = true;
  }
  
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value="EI_EXPOSE_REP2", 
    justification="fix later")
  public void attach(ActiveServerEntity<M, R> entity,
                     MessageCodec<M, R> codec,
                     ConcurrencyStrategy<M> concurrencyStrategy,
                     byte[] config) {
    this.codec = codec;
    this.configuration = config;
    entity.connected(clientDescriptor);
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value="EI_EXPOSE_REP", 
    justification="fix later")
  @Override
  public byte[] getEntityConfiguration() {
    // This is harmless while closed but shouldn't be called so check open.
    checkEndpointOpen();
    return configuration;
  }

  @Override
  public void setDelegate(EndpointDelegate<R> delegate) {
    // This is harmless while closed but shouldn't be called so check open.
    checkEndpointOpen();
    Assert.assertNull(this.delegate);
    this.delegate = delegate;
  }

  @Override
  public Invocation<R> message(M message) {
    // We can't create new invocations when the endpoint is closed.
    checkEndpointOpen();
    return new InvocationImpl<>(message);
  }

  private class FakeClientDescriptor implements ClientDescriptor {
    @Override
    public ClientSourceId getSourceId() {
      return null;
    }

    @Override
    public boolean isValidClient() {
      return false;
    }
  }

  private class InvocationImpl<M, R> implements Invocation<R> {
    private final M request;

    private InvocationImpl(M request) {
      this.request = request;
    }

    @Override
    public Task invoke(InvocationCallback<R> callback, Set<InvocationCallback.Types> callbacks) {
      return null;
    }

  }

  public ClientCommunicator clientCommunicator() {
    return clientCommunicator;
  }

  private class TestClientCommunicator implements ClientCommunicator {
    @Override
    public void sendNoResponse(ClientDescriptor clientDescriptor, EntityResponse message) {
      if (clientDescriptor == PassthroughEndpoint.this.clientDescriptor) {
        if (null != PassthroughEndpoint.this.delegate) {
          try {
            // We will encode and decode the message, to simulate how this would function over a network.
            @SuppressWarnings("unchecked")
            byte[] payload = PassthroughEndpoint.this.codec.encodeResponse((R)message);
            R fromServer = PassthroughEndpoint.this.codec.decodeResponse(payload);
            PassthroughEndpoint.this.delegate.handleMessage(fromServer);
          } catch (MessageCodecException e) {
            // Unexpected in this test.
            Assert.fail();
          }
        }
      }
    }

    @Override
    public void closeClientConnection(ClientDescriptor clientDescriptor) {
      close();
    }
  }

  @Override
  public void close() {
    // We can't close twice.
    checkEndpointOpen();
    this.isOpen = false;
    // In a real implementation, this is where a call to the PlatformService, to clean up, would be.
  }

  @Override
  public Future<Void> release() {
    close();
    return completedFuture(null);
  }

  private void checkEndpointOpen() {
    if (!this.isOpen) {
      throw new IllegalStateException("Endpoint closed");
    }
  }

  public long getCurrentId() {
    return 0L;
  }

  public long resetEldestId() {
    eldest = 0L;
    return eldest;
  }
}
