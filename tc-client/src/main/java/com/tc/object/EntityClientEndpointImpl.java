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
import org.terracotta.entity.Invocation;
import org.terracotta.entity.InvocationCallback;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodecException;

import com.tc.util.Assert;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tc.object.SafeInvocationCallback.safe;
import org.terracotta.exception.EntityException;


public class EntityClientEndpointImpl<M extends EntityMessage, R extends EntityResponse> implements EntityClientEndpoint<M, R> {

  private static Logger LOGGER = LoggerFactory.getLogger(EntityClientEndpointImpl.class);

  private final InvocationHandler invocationHandler;
  private final byte[] configuration;
  private final EntityDescriptor invokeDescriptor;
  private final EntityID entityID;
  private final long version;
  private final MessageCodec<M, R> codec;
  private final CloseHookCallable closeHook;
  private final ExecutorService closer;
  private EndpointDelegate<R> delegate;
  private boolean isOpen;
  private Future<Void> releaseFuture;
  private final InFlightStats stats = new InFlightStats();
  /**
   * @param eid The type name name of the target entity
   * @param version the version of the entity targeted
   * @param instance the combination of the FetchID from the server and the ClientInstanceID from the client
   * @param invocationHandler Called to handle "invokeAction" requests made on this end-point.
   * @param entityConfiguration Opaque byte[] describing how to configure the entity to be built on top of this end-point.
   * @param closeHook A Runnable which will be run last when the end-point is closed.
   */
  public EntityClientEndpointImpl(EntityID eid, long version, EntityDescriptor instance, InvocationHandler invocationHandler, byte[] entityConfiguration, MessageCodec<M, R> codec, CloseHookCallable closeHook, ExecutorService closer) {
    this.entityID = eid;
    this.version = version;
    this.invokeDescriptor = instance;
    this.invocationHandler = invocationHandler;
    this.configuration = entityConfiguration;
    this.codec = codec;
    this.closeHook = closeHook;
    this.closer = closer;
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
    checkEndpointOpen(false);
    return configuration;
  }

  @Override
  public void setDelegate(EndpointDelegate<R> delegate) {
    // This is harmless while closed but shouldn't be called so check open.
    checkEndpointOpen(false);
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
    
  public InFlightStats getStatistics() {
    return stats;
  }
    
  @Override
  public Invocation<R> message(M message) {
    // We can't create new invocations when the endpoint is closed.
    checkEndpointOpen(false);
    return new InvocationImpl(message);
  }

  private class InvocationImpl implements Invocation<R> {
    private boolean invoked = false;
    private final M request;

    private InvocationImpl(M request) {
      this.request = request;
    }

    // TODO: fill in durability/consistency options here.

    @Override
    public Task invoke(InvocationCallback<R> callback, Set<InvocationCallback.Types> callbacks) {
      checkInvoked();
      invoked = true;
      SafeInvocationCallback<byte[]> binaryCallback = new BinaryInvocationCallback<>(codec, safe(callback));
      try {
        return invocationHandler.invokeAction(entityID, invokeDescriptor, callbacks, binaryCallback, true, codec.encodeMessage(request));
      } catch (MessageCodecException e) {
        binaryCallback.failure(e);
        binaryCallback.complete();
        binaryCallback.retired();
        return () -> false;
      }
    }

    private void checkInvoked() {
      if (invoked) {
        throw new IllegalStateException("Already invoked");
      }
    }
  }

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
    try {
      checkEndpointOpen(true);
    } catch (IllegalStateException alreadyClosed) {
      LOGGER.info("Trying to close an endpoint which has already been closed", alreadyClosed);
      return;
    }
    if (this.closeHook != null) {
      EntityException e = this.closeHook.call();
      LOGGER.info("Exception occured during close", e);
      // log and swallow this exception closing
    }
  }

  @Override
  public synchronized Future<Void> release() {
    if (releaseFuture == null) {
      if (this.closer == null) {
        close();
        releaseFuture = CompletableFuture.completedFuture(null);
      } else {
        try {
          Callable<Void> call = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
              close();
              return null;
            }
          };
          releaseFuture = this.closer.submit(call);
        } catch (RejectedExecutionException re) {
          // connection already shutdown
          LOGGER.debug("connection already closed", re);
          releaseFuture = CompletableFuture.completedFuture(null);
        }
      }
    }
    return releaseFuture;
  }

  public void didCloseUnexpectedly() {
    // TODO:  Determine if we need to limit anything here on closed.  The call can come from another thread so it may not
    // yet know that we are closed when the call originated.
    // NOTE:  We do NOT run the close hook in this situation since it is assuming that the close was requested and that the
    // underlying connection is still viable.
    if (null != this.delegate) {
      this.delegate.didDisconnectUnexpectedly();
    }
  }

  private synchronized void checkEndpointOpen(boolean close) {
    if (!this.isOpen) {
      throw new IllegalStateException("Endpoint closed");
    }
    if (close) {
      this.isOpen = false;
    }
  }
}
