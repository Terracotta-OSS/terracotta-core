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
  public Void decode(Type type, boolean shouldReplicate, final long transactionID, DataInputStream input) throws IOException {
    // First step, send the ack.
    PassthroughMessage ack = PassthroughMessageCodec.createAckMessage();
    ack.setTransactionID(transactionID);
    sender.sendAck(ack);
    
    // Now, before we can actually RUN the message, we need to make sure that we wait for its replicated copy to complete
    // on the passive.
    if (shouldReplicate && (null != this.downstreamPassive)) {
      PassthroughInterserverInterlock wrapper = new PassthroughInterserverInterlock(this.sender);
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
        
        // Note that the fetch is asynchronous since it may be blocked acquiring the read-lock (which is asynchronous).
        IFetchResult onFetch = new IFetchResult() {
          @Override
          public void onFetchComplete(byte[] config, Exception error) {
            sendCompleteResponse(sender, transactionID, config, error);
          }
        };
        try {
          this.messageHandler.fetch(sender, clientInstanceID, entityClassName, entityName, version, onFetch);
        } catch (Exception error) {
          // An unexpected exception is the only case where we send the response at this level.
          byte[] response = null;
          sendCompleteResponse(sender, transactionID, response, error);
        }
        break;
      }
      case RELEASE_ENTITY: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        long clientInstanceID = input.readLong();
        byte[] response = null;
        Exception error = null;
        try {
          // There is no response on successful delete.
          this.messageHandler.release(sender, clientInstanceID, entityClassName, entityName);
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
          // We respond with the config, if found.
          response = this.messageHandler.invoke(sender, clientInstanceID, entityClassName, entityName, payload);
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
      case LOCK_ACQUIRE: {
        // This is used for the maintenance write-lock.  It is made for the connection, on the entity name (as there aren't
        // "clientInstanceIDs" for maintenance mode refs).
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        Runnable onAcquire = new Runnable() {
          @Override
          public void run() {
            // We will just send an empty response, with no error, on acquire.
            byte[] response = new byte[0];
            Exception error = null;
            sendCompleteResponse(sender, transactionID, response, error);
          }
        };
        try {
          this.messageHandler.acquireWriteLock(sender, entityClassName, entityName, onAcquire);
        } catch (Exception error) {
          // An unexpected exception is the only case where we send the response at this level.
          sendCompleteResponse(sender, transactionID, null, error);
        }
        break;
      }
      case LOCK_RELEASE: {
        // This is used for the maintenance write-lock.  It is made for the connection, on the entity name (as there aren't
        // "clientInstanceIDs" for maintenance mode refs).
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        byte[] response = null;
        Exception error = null;
        try {
          this.messageHandler.releaseWriteLock(sender, entityClassName, entityName);
          response = new byte[0];
        } catch (Exception e) {
          error = e;
        }
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case RECONNECT: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        long clientInstanceID = input.readLong();
        int extendedDataLength = input.readInt();
        byte[] extendedData = new byte[extendedDataLength];
        input.readFully(extendedData);
        
        // This is similar to FETCH but fully synchronous since we can't wait for lock on reconnect.
        byte[] response = null;
        Exception error = null;
        try {
          this.messageHandler.reconnect(sender, clientInstanceID, entityClassName, entityName, extendedData);
          // No response;
          response = new byte[0];
        } catch (Exception e) {
          error = e;
        }
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case SYNC_ENTITY_START: {
        // We just want to do the same thing that CREATE does.
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        long version = input.readLong();
        byte[] serializedConfiguration = new byte[input.readInt()];
        input.readFully(serializedConfiguration);
        Exception error = null;
        try {
          this.messageHandler.create(entityClassName, entityName, version, serializedConfiguration);
          this.messageHandler.syncEntityStart(sender, entityClassName, entityName);
        } catch (Exception e) {
          error = e;
        }
        // Note that there is no response for sync messages.
        byte[] response = null;
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case SYNC_ENTITY_END: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        Exception error = null;
        try {
          this.messageHandler.syncEntityEnd(sender, entityClassName, entityName);
        } catch (Exception e) {
          error = e;
        }
        // Note that there is no response for sync messages.
        byte[] response = null;
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case SYNC_ENTITY_KEY_START: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        int concurrencyKey = input.readInt();
        Exception error = null;
        try {
          this.messageHandler.syncEntityKeyStart(sender, entityClassName, entityName, concurrencyKey);
        } catch (Exception e) {
          error = e;
        }
        // Note that there is no response for sync messages.
        byte[] response = null;
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case SYNC_ENTITY_KEY_END: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        int concurrencyKey = input.readInt();
        Exception error = null;
        try {
          this.messageHandler.syncEntityKeyEnd(sender, entityClassName, entityName, concurrencyKey);
        } catch (Exception e) {
          error = e;
        }
        // Note that there is no response for sync messages.
        byte[] response = null;
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case SYNC_ENTITY_PAYLOAD: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        int concurrencyKey = input.readInt();
        byte[] payload = new byte[input.readInt()];
        input.readFully(payload);
        Exception error = null;
        try {
          this.messageHandler.syncPayload(sender, entityClassName, entityName, concurrencyKey, payload);
        } catch (Exception e) {
          error = e;
        }
        // Note that there is no response for sync messages.
        byte[] response = null;
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
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
   * This interface handles the result of the message processing, exposing high-level methods to be called to satisfy the
   * meaning of a message.
   */
  public static interface MessageHandler {
    void create(String entityClassName, String entityName, long version, byte[] serializedConfiguration) throws Exception;
    void destroy(String entityClassName, String entityName) throws Exception;
    void fetch(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName, long version, IFetchResult onFetch);
    void release(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName) throws Exception;
    byte[] invoke(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName, byte[] payload) throws Exception;
    void acquireWriteLock(IMessageSenderWrapper sender, String entityClassName, String entityName, Runnable onAcquire);
    void releaseWriteLock(IMessageSenderWrapper sender, String entityClassName, String entityName);
    void reconnect(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName, byte[] extendedData);
    void syncEntityStart(IMessageSenderWrapper sender, String entityClassName, String entityName) throws Exception;
    void syncEntityEnd(IMessageSenderWrapper sender, String entityClassName, String entityName) throws Exception;
    void syncEntityKeyStart(IMessageSenderWrapper sender, String entityClassName, String entityName, int concurrencyKey) throws Exception;
    void syncEntityKeyEnd(IMessageSenderWrapper sender, String entityClassName, String entityName, int concurrencyKey) throws Exception;
    void syncPayload(IMessageSenderWrapper sender, String entityClassName, String entityName, int concurrencyKey, byte[] payload) throws Exception;
  }
}
