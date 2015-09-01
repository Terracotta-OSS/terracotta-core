package org.terracotta.passthrough;

import java.io.DataInputStream;
import java.io.IOException;

import org.terracotta.passthrough.PassthroughMessage.Type;


/**
 * A helper class which decodes a message, on the server, deciding if it needs to be replicated to a passive and translating
 * it into high-level operations on the server.
 * One instance of this is created for every message processed by a server.
 * It is used entirely on the server thread.
 */
public class PassthroughServerMessageDecoder implements PassthroughMessageCodec.Decoder<Void> {
  private final MessageHandler messageHandler;
  private final PassthroughServerProcess downstreamPassive;
  private final IMessageSenderWrapper sender;
  private final byte[] message;

  public PassthroughServerMessageDecoder(MessageHandler messageHandler, PassthroughServerProcess downstreamPassive, IMessageSenderWrapper sender, byte[] message) {
    this.messageHandler = messageHandler;
    this.downstreamPassive = downstreamPassive;
    this.sender = sender;
    this.message = message;
  }
  @Override
  public Void decode(Type type, boolean shouldReplicate, long transactionID, DataInputStream input) throws IOException {
    // First step, send the ack.
    PassthroughMessage ack = PassthroughMessageCodec.createAckMessage();
    ack.setTransactionID(transactionID);
    sender.sendAck(ack);
    
    // Now, before we can actually RUN the message, we need to make sure that we wait for its replicated copy to complete
    // on the passive.
    if (shouldReplicate && (null != this.downstreamPassive)) {
      ServerSender wrapper = new ServerSender();
      this.downstreamPassive.sendMessageToServerFromActive(wrapper, message);
      wrapper.waitForComplete();
    }
    
    // Now, decode the message and interpret it.
    switch (type) {
      case CREATE_ENTITY: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        long version = input.readLong();
        byte[] serializedConfiguration = new byte[input.readInt()];
        input.readFully(serializedConfiguration);
        byte[] response = null;
        Exception error = null;
        try {
          // There is no response on successful create.
          this.messageHandler.create(entityClassName, entityName, version, serializedConfiguration);
        } catch (Exception e) {
          error = e;
        }
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case DESTROY_ENTITY: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        byte[] response = null;
        Exception error = null;
        try {
          // There is no response on successful delete.
          this.messageHandler.destroy(entityClassName, entityName);
        } catch (Exception e) {
          error = e;
        }
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case DOES_ENTITY_EXIST:
        // TODO:  Implement.
        Assert.unimplemented();
        break;
      case FETCH_ENTITY: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        long clientInstanceID = input.readLong();
        long version = input.readLong();
        byte[] response = null;
        Exception error = null;
        try {
          PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
          // We respond with the config, if found.
          response = this.messageHandler.fetch(clientDescriptor, entityClassName, entityName, version);
        } catch (Exception e) {
          error = e;
        }
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case RELEASE_ENTITY: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        long clientInstanceID = input.readLong();
        byte[] response = null;
        Exception error = null;
        try {
          PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
          // There is no response on successful delete.
          this.messageHandler.release(clientDescriptor, entityClassName, entityName);
        } catch (Exception e) {
          error = e;
        }
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case INVOKE_ON_SERVER: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        long clientInstanceID = input.readLong();
        byte[] payload = new byte[input.readInt()];
        input.readFully(payload);
        byte[] response = null;
        Exception error = null;
        try {
          PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
          // We respond with the config, if found.
          response = this.messageHandler.invoke(clientDescriptor, entityClassName, entityName, payload);
        } catch (Exception e) {
          error = e;
        }
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case ACK_FROM_SERVER:
      case COMPLETE_FROM_SERVER:
      case INVOKE_ON_CLIENT:
        // Not invoked on server.
        Assert.unreachable();
        break;
      default:
        Assert.unreachable();
        break;
    }
    return null;
  }

  private void sendCompleteResponse(IMessageSenderWrapper sender, long transactionID, byte[] response, Exception error) {
    PassthroughMessage complete = PassthroughMessageCodec.createCompleteMessage(response, error);
    complete.setTransactionID(transactionID);
    sender.sendComplete(complete);
  }


  /**
   * In the case where we are an active sending a message to a downstream passive, we use this implementation to provide the
   * basic interlock across the 2 threads.
   */
  private static class ServerSender implements IMessageSenderWrapper {
    private boolean isDone = false;
    
    public synchronized void waitForComplete() {
      while (!this.isDone) {
        try {
          wait();
        } catch (InterruptedException e) {
          Assert.unexpected(e);
        }
      }
    }
    @Override
    public void sendAck(PassthroughMessage ack) {
    }
    @Override
    public synchronized void sendComplete(PassthroughMessage complete) {
      this.isDone = true;
      notifyAll();
    }
    @Override
    public PassthroughClientDescriptor clientDescriptorForID(long clientInstanceID) {
      Assert.unreachable();
      return null;
    }
  }


  /**
   * This interface handles the result of the message processing, exposing high-level methods to be called to satisfy the
   * meaning of a message.
   */
  public static interface MessageHandler {
    void create(String entityClassName, String entityName, long version, byte[] serializedConfiguration) throws Exception;
    void destroy(String entityClassName, String entityName) throws Exception;
    byte[] fetch(PassthroughClientDescriptor clientDescriptor, String entityClassName, String entityName, long version) throws Exception;
    void release(PassthroughClientDescriptor clientDescriptor, String entityClassName, String entityName) throws Exception;
    byte[] invoke(PassthroughClientDescriptor clientDescriptor, String entityClassName, String entityName, byte[] payload) throws Exception;
  }
}
