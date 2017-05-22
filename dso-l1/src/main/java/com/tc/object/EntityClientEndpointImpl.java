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

package com.tc.object;

import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodecException;

import com.tc.entity.VoltronEntityMessage;
import com.tc.util.Assert;
import org.terracotta.exception.EntityException;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class EntityClientEndpointImpl<M extends EntityMessage, R extends EntityResponse> implements EntityClientEndpoint<M, R> {
  private final InvocationHandler invocationHandler;
  private final byte[] configuration;
  private final EntityDescriptor invokeDescriptor;
  private final EntityID entityID;
  private final long version;
  private final MessageCodec<M, R> codec;
  private final Runnable closeHook;
  private EndpointDelegate delegate;
  private boolean isOpen;

  /**
   * @param eid The type name name of the target entity
   * @param version the version of the entity targeted
   * @param instance the combination of the FetchID from the server and the ClientInstanceID from the client
   * @param invocationHandler Called to handle "invokeAction" requests made on this end-point.
   * @param entityConfiguration Opaque byte[] describing how to configure the entity to be built on top of this end-point.
   * @param closeHook A Runnable which will be run last when the end-point is closed.
   */
  public EntityClientEndpointImpl(EntityID eid, long version, EntityDescriptor instance, InvocationHandler invocationHandler, byte[] entityConfiguration, MessageCodec<M, R> codec, Runnable closeHook) {
    this.entityID = eid;
    this.version = version;
    this.invokeDescriptor = instance;
    this.invocationHandler = invocationHandler;
    this.configuration = entityConfiguration;
    this.codec = codec;
    this.closeHook = closeHook;
    // We start in the open state.
    this.isOpen = true;
  }
  
  EntityID getEntityID() {
    return this.entityID;
  }
  
  long getVersion() {
    return this.version;
  }
  
  EntityDescriptor getEntityDescriptor() {
    return this.invokeDescriptor;
  }
  
  

  @Override
  public byte[] getEntityConfiguration() {
    // This is harmless while closed but shouldn't be called so check open.
    checkEndpointOpen();
    return configuration;
  }

  @Override
  public void setDelegate(EndpointDelegate delegate) {
    // This is harmless while closed but shouldn't be called so check open.
    checkEndpointOpen();
    Assert.assertNull(this.delegate);
    this.delegate = delegate;
  }
  
  public void handleMessage(byte[] message) throws MessageCodecException {
    // We technically allow messages to come back from the server, after we are closed, simple because it means that the
    // server hasn't yet handled the close.
    if (null != this.delegate) {
      R messageFromServer = this.codec.decodeResponse(message);
      this.delegate.handleMessage(messageFromServer);
    }
  }

  @Override
  public InvocationBuilder<M, R> beginInvoke() {
    // We can't create new invocations when the endpoint is closed.
    checkEndpointOpen();
    return new InvocationBuilderImpl();
  }

  private class InvocationBuilderImpl implements InvocationBuilder<M, R> {
    private boolean invoked = false;
    private M request;
    private final Set<VoltronEntityMessage.Acks> acks = EnumSet.noneOf(VoltronEntityMessage.Acks.class);
    private boolean requiresReplication = true;
    // By default, we block the get() on the RETIRE ack.
    private boolean shouldBlockGetOnRetire = true;

    // TODO: fill in durability/consistency options here.

    @Override
    public synchronized InvocationBuilderImpl message(M request) {
      checkInvoked();
      this.request = request;
      return this;
    }

    @Override
    public InvocationBuilder<M, R> ackSent() {
      acks.add(VoltronEntityMessage.Acks.SENT);
      return this;
    }

    @Override
    public InvocationBuilder<M, R> ackReceived() {
      acks.add(VoltronEntityMessage.Acks.RECEIVED);
      return this;
    }

    @Override
    public InvocationBuilder<M, R> ackCompleted() {
      acks.add(VoltronEntityMessage.Acks.COMPLETED);
      return this;
    }

    @Override
    public InvocationBuilder<M, R> ackRetired() {
      acks.add(VoltronEntityMessage.Acks.RETIRED);
      return this;
    }

    @Override
    public InvocationBuilder<M, R> replicate(boolean requiresReplication) {
      this.requiresReplication = requiresReplication;
      return this;
    }

    @Override
    public InvocationBuilder<M, R> blockGetOnRetire(boolean shouldBlock) {
      this.shouldBlockGetOnRetire = shouldBlock;
      return this;
    }

    @Override
    public synchronized InvokeFuture<R> invoke() throws MessageCodecException {
      checkInvoked();
      invoked = true;
      final InvokeFuture<byte[]> invokeFuture = invocationHandler.invokeAction(invokeDescriptor, this.acks, this.requiresReplication, this.shouldBlockGetOnRetire, codec.encodeMessage(request));
      return new InvokeFuture<R>() {
        @Override
        public boolean isDone() {
          return invokeFuture.isDone();
        }

        @Override
        public R get() throws InterruptedException, EntityUserException, EntityException {
          try {
            return codec.decodeResponse(invokeFuture.get());
          } catch (MessageCodecException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public R getWithTimeout(long timeout, TimeUnit unit) throws InterruptedException, EntityException, EntityUserException, TimeoutException {
          try {
            return codec.decodeResponse(invokeFuture.getWithTimeout(timeout, unit));
          } catch (MessageCodecException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void interrupt() {
          invokeFuture.interrupt();
        }
      };
    }

    private void checkInvoked() {
      if (invoked) {
        throw new IllegalStateException("Already invoked");
      }
    }
  }

  @Override
  public byte[] getExtendedReconnectData() {
    // TODO:  Determine if we need to limit anything here on closed.  The call can come from another thread so it may not
    // yet know that we are closed when the call originated.
    byte[] reconnectData = null;
    if (null != this.delegate) {
      reconnectData = this.delegate.createExtendedReconnectData();
    }
    if (null == reconnectData) {
      reconnectData = new byte[0];
    }
    return reconnectData;
  }

  @Override
  public void close() {
    // We can't close twice.
    checkEndpointOpen();
    if (this.closeHook != null) {
      this.closeHook.run();
    }
    // We also need to invalidate ourselves so we don't continue allowing new messages through when disconnecting.
    this.isOpen = false;
  }

  @Override
  public void didCloseUnexpectedly() {
    // TODO:  Determine if we need to limit anything here on closed.  The call can come from another thread so it may not
    // yet know that we are closed when the call originated.
    // NOTE:  We do NOT run the close hook in this situation since it is assuming that the close was requested and that the
    // underlying connection is still viable.
    if (null != this.delegate) {
      this.delegate.didDisconnectUnexpectedly();
    }
  }

  private void checkEndpointOpen() {
    if (!this.isOpen) {
      throw new IllegalStateException("Endpoint closed");
    }
  }
}
