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
package org.terracotta.passthrough;

import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;


/**
 * The object representing the connection end-point of a client-side entity.  Messages sent from the client entity are routed
 * through the InvocationBuilder into the server, from here.  Additionally, messages from the server to the entity are routed
 * through here.
 */
public class PassthroughEntityClientEndpoint<M extends EntityMessage, R extends EntityResponse> implements EntityClientEndpoint<M, R> {
  private final PassthroughConnection connection;
  private final Class<?> entityClass;
  private final String entityName;
  private final long clientInstanceID;
  private final byte[] config;
  private final MessageCodec<M, R> messageCodec;
  private final Runnable onClose;
  private EndpointDelegate delegate;
  private boolean isOpen;
  
  public PassthroughEntityClientEndpoint(PassthroughConnection passthroughConnection, Class<?> entityClass, String entityName, long clientInstanceID, byte[] config, MessageCodec<M, R> messageCodec, Runnable onClose) {
    this.connection = passthroughConnection;
    this.entityClass = entityClass;
    this.entityName = entityName;
    this.clientInstanceID = clientInstanceID;
    this.config = config;
    this.messageCodec = messageCodec;
    this.onClose = onClose;
    // We start in the open state.
    this.isOpen = true;
  }

  @Override
  public byte[] getEntityConfiguration() {
    // This is harmless while closed but shouldn't be called so check open.
    checkEndpointOpen();
    return this.config;
  }

  @Override
  public void setDelegate(EndpointDelegate delegate) {
    // This is harmless while closed but shouldn't be called so check open.
    checkEndpointOpen();
    Assert.assertTrue(null == this.delegate);
    this.delegate = delegate;
  }

  @Override
  public InvocationBuilder<M, R> beginInvoke() {
    // We can't create new invocations when the endpoint is closed.
    checkEndpointOpen();
    return new PassthroughInvocationBuilder<M, R>(this.connection, this.entityClass.getCanonicalName(), this.entityName, this.clientInstanceID, messageCodec);
  }

  @Override
  public void close() {
    // We can't close twice.
    checkEndpointOpen();
    this.isOpen = false;
    // We need to release this entity.
    PassthroughMessage releaseMessage = PassthroughMessageCodec.createReleaseMessage(this.entityClass.getCanonicalName(), this.entityName, this.clientInstanceID);
    InvokeFuture<byte[]> received = this.connection.sendInternalMessageAfterAcks(releaseMessage);
    try {
      received.get();
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    } catch (EntityException e) {
      Assert.unexpected(e);
    }
    onClose.run();
  }

  public void didCloseUnexpectedly() {
    if (null != this.delegate) {
      this.delegate.didDisconnectUnexpectedly();
    }
  }

  public void handleMessageFromServer(byte[] payload) throws MessageCodecException {
    if (null != this.delegate) {
      R fromServer = this.messageCodec.decodeResponse(payload);
      this.delegate.handleMessage(fromServer);
    }
  }

  public byte[] getExtendedReconnectData() {
    byte[] toReturn = null;
    if (null != this.delegate) {
      toReturn = this.delegate.createExtendedReconnectData();
    }
    // We can't return a null byte array.
    if (null == toReturn) {
      toReturn = new byte[0];
    }
    return toReturn;
  }

  public PassthroughMessage buildReconnectMessage(byte[] extendedData) {
    // Construct the reconnect message.
    // NOTE:  This currently only describes the entity we are referencing.
    return PassthroughMessageCodec.createReconnectMessage(this.entityClass.getCanonicalName(), this.entityName, this.clientInstanceID, extendedData);
  }

  /**
   * This is called by the PassthroughConnection, when it is unexpectedly closed, to get the message which describes which
   * connection to break, to the server.
   * 
   * @return The message which can be sent to the server.
   */
  public PassthroughMessage createUnexpectedReleaseMessage() {
    // The unexpected release message is similar to the normal kind but we give it a different type so that we can track
    // this exceptional case, more easily.
    // In the future, this may also allow the implementation to be more aggressive.
    return PassthroughMessageCodec.createUnexpectedReleaseMessage(this.entityClass.getCanonicalName(), this.entityName, this.clientInstanceID);
  }

  private void checkEndpointOpen() {
    if (!this.isOpen) {
      throw new IllegalStateException("Endpoint closed");
    }
  }
}
