package org.terracotta.passthrough;

import java.util.concurrent.Future;

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.Service;
import org.terracotta.entity.ServiceConfiguration;


public class PassthroughCommunicatorService implements Service<ClientCommunicator>, ClientCommunicator {

  @Override
  public void sendNoResponse(ClientDescriptor clientDescriptor, byte[] payload) {
    PassthroughClientDescriptor rawDescriptor = (PassthroughClientDescriptor) clientDescriptor;
    PassthroughConnection connection = rawDescriptor.sender;
    long clientInstanceID = rawDescriptor.clientInstanceID;
    PassthroughMessage message = PassthroughMessageCodec.createMessageToClient(clientInstanceID, payload);
    connection.sendMessageToClient(message.asSerializedBytes());
  }

  @Override
  public Future<Void> send(ClientDescriptor clientDescriptor, byte[] payload) {
    // TODO:  Handle the case where we want to block here.
    Assert.unimplemented();
    return null;
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
}
