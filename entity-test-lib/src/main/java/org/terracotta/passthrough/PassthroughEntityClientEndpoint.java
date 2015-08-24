package org.terracotta.passthrough;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.terracotta.entity.EndpointListener;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientReconnectHandler;
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
  private final List<EndpointListener> listeners;
  
  public PassthroughEntityClientEndpoint(PassthroughConnection passthroughConnection, Class<?> entityClass, String entityName, long clientInstanceID, byte[] config, Runnable onClose) {
    this.connection = passthroughConnection;
    this.entityClass = entityClass;
    this.entityName = entityName;
    this.clientInstanceID = clientInstanceID;
    this.config = config;
    this.onClose = onClose;
    this.listeners = new Vector<EndpointListener>();
  }

  @Override
  public byte[] getEntityConfiguration() {
    return this.config;
  }

  @Override
  public void registerListener(EndpointListener listener) {
    this.listeners.add(listener);
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

  public void handleMessageFromServer(byte[] payload) {
    for (EndpointListener listener : this.listeners) {
      listener.handleMessage(payload);
    }
  }

  @Override
  public void setReconnectHandler(EntityClientReconnectHandler handler) {
    Assert.unimplemented();
  }

  @Override
  public byte[] getExtendedReconnectData() {
    return new byte[0];
  }
}
