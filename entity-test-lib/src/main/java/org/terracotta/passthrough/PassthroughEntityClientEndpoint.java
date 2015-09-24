package org.terracotta.passthrough;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvocationBuilder;


/**
 * The object representing the connection end-point of a client-side entity.  Messages sent from the client entity are routed
 * through the InvocationBuilder into the server, from here.  Additionally, messages from the server to the entity are routed
 * through here.
 */
public class PassthroughEntityClientEndpoint implements EntityClientEndpoint {
  private final PassthroughConnection connection;
  private final Class<?> entityClass;
  private final String entityName;
  private final long clientInstanceID;
  private final byte[] config;
  private final Runnable onClose;
  private EndpointDelegate delegate;
  
  public PassthroughEntityClientEndpoint(PassthroughConnection passthroughConnection, Class<?> entityClass, String entityName, long clientInstanceID, byte[] config, Runnable onClose) {
    this.connection = passthroughConnection;
    this.entityClass = entityClass;
    this.entityName = entityName;
    this.clientInstanceID = clientInstanceID;
    this.config = config;
    this.onClose = onClose;
  }

  @Override
  public byte[] getEntityConfiguration() {
    return this.config;
  }

  @Override
  public void setDelegate(EndpointDelegate delegate) {
    Assert.assertTrue(null == this.delegate);
    this.delegate = delegate;
  }

  @Override
  public InvocationBuilder beginInvoke() {
    return new PassthroughInvocationBuilder(this.connection, this.entityClass, this.entityName, this.clientInstanceID);
  }

  @Override
  public void close() {
    // We need to release this entity.
    PassthroughMessage releaseMessage = PassthroughMessageCodec.createReleaseMessage(this.entityClass, this.entityName, this.clientInstanceID);
    Future<byte[]> received = this.connection.sendInternalMessageAfterAcks(releaseMessage);
    try {
      received.get();
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    } catch (ExecutionException e) {
      Assert.unexpected(e);
    }
    onClose.run();
  }

  @Override
  public void didCloseUnexpectedly() {
    if (null != this.delegate) {
      this.delegate.didDisconnectUnexpectedly();
    }
  }

  public void handleMessageFromServer(byte[] payload) {
    if (null != this.delegate) {
      this.delegate.handleMessage(payload);
    }
  }

  @Override
  public byte[] getExtendedReconnectData() {
    return (null != this.delegate)
        ? this.delegate.createExtendedReconnectData()
        : new byte[0];
  }

  public void reconnect(byte[] extendedData) {
    // Construct the reconnect message.
    // NOTE:  This currently only describes the entity we are referencing.
    PassthroughMessage reconnectMessage = PassthroughMessageCodec.createReconnectMessage(this.entityClass, this.entityName, this.clientInstanceID, extendedData);
    // We ignore the return value.
    this.connection.sendInternalMessageAfterAcks(reconnectMessage);
  }
}
