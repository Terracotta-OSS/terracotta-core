package org.terracotta.passthrough;

import java.util.concurrent.Future;

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.Service;
import org.terracotta.entity.ServiceConfiguration;


/**
 * The implementation of the communicator service which is normally built-in to the platform.
 * This is used, by server-side entities, to send messages back to specific client-side instances.
 * TODO:  we currently need to determine how to handle the synchronous send.
 */
public class PassthroughCommunicatorService implements Service<ClientCommunicator>, ClientCommunicator {
  @Override
  public void sendNoResponse(ClientDescriptor clientDescriptor, byte[] payload) {
    prepareAndSendMessage(clientDescriptor, payload);
  }

  @Override
  public Future<Void> send(ClientDescriptor clientDescriptor, byte[] payload) {
    return prepareAndSendMessage(clientDescriptor, payload);
  }

  @Override
  public void initialize(ServiceConfiguration<? extends ClientCommunicator> configuration) {
  }

  @Override
  public ClientCommunicator get() {
    return this;
  }

  @Override
  public void destroy() {
    // Do nothing.
  }

  private Future<Void> prepareAndSendMessage(ClientDescriptor clientDescriptor, byte[] payload) {
    PassthroughClientDescriptor rawDescriptor = (PassthroughClientDescriptor) clientDescriptor;
    PassthroughConnection connection = rawDescriptor.sender;
    long clientInstanceID = rawDescriptor.clientInstanceID;
    Future<Void> waiter = connection.createClientResponseFuture();
    PassthroughMessage message = PassthroughMessageCodec.createMessageToClient(clientInstanceID, payload);
    connection.sendMessageToClient(message.asSerializedBytes());
    return waiter;
  }
}
