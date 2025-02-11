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
package org.terracotta.passthrough;

import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityServerException;
import org.terracotta.passthrough.PassthroughMessage.Type;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


/**
 * A helper class which decodes a message, on the server, deciding if it needs to be replicated to a passive and translating
 * it into high-level operations on the server.
 * One instance of this is created for every message processed by a server.
 * It is used entirely on the server thread.
 */
public class PassthroughServerMessageDecoder implements PassthroughMessageCodec.Decoder<Void> {
  private final PassthroughServerProcess thisServer;
  private final MessageHandler messageHandler;
  private final PassthroughTransactionOrderManager transactionOrderManager;
  private final LifeCycleMessageHandler lifeCycleMessageHandler;
  private final Set<PassthroughServerProcess> downstreamPassives = new HashSet<PassthroughServerProcess>();
  private final IMessageSenderWrapper sender;
  private final IAsynchronousServerCrasher crasher;
  private final byte[] message;

 @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value="EI_EXPOSE_REP2")
  public PassthroughServerMessageDecoder(PassthroughServerProcess thisServer, MessageHandler messageHandler, PassthroughTransactionOrderManager transactionOrderManager, LifeCycleMessageHandler lifeCycleMessageHandler, Set<PassthroughServerProcess> downstreamPassives, IMessageSenderWrapper sender, IAsynchronousServerCrasher crasher, byte[] message) {
    this.thisServer = thisServer;
    this.messageHandler = messageHandler;
    this.transactionOrderManager = transactionOrderManager;
    this.lifeCycleMessageHandler = lifeCycleMessageHandler;
    this.downstreamPassives.addAll(downstreamPassives);
    this.sender = sender;
    this.crasher = crasher;
    this.message = message;
  }
  @Override
  public Void decode(Type type, boolean shouldReplicate, final long transactionID, final long oldestTransactionID, DataInputStream input) throws IOException {
    // First step, update our persistence.
    long originID = this.sender.getClientOriginID();
    // Negative origin IDs are for internal messages - we don't want to track them.
    if ((null != this.transactionOrderManager) && (originID >= 0)) {
      this.transactionOrderManager.updateTracking(originID, transactionID, oldestTransactionID);
    }
    
    // Next, send the ack.
    PassthroughMessage ack = PassthroughMessageCodec.createAckMessage();
    // The oldestTransactionID isn't relevant when sent back.
    long oldestTransactionIDToReturn = -1;
    ack.setTransactionTracking(transactionID, oldestTransactionIDToReturn);
    sender.sendAck(ack);
    
    // Now, before we can actually RUN the message, we need to make sure that we wait for its replicated copy to complete
    // on the passive.
    // In order to implement the consensus model, we also need to see if the passives experienced success or failure when running the replicated message.
    // Either all the servers need to succeed, all them need to fail, or any failing servers must be crashed.
    // (note that we only use this in the create/reconfigure cases, for now).
    Set<PassthroughServerProcess> failingServers = new HashSet<PassthroughServerProcess>();
    if (shouldReplicate && this.downstreamPassives.size() > 0) {
      for (PassthroughServerProcess passive : downstreamPassives) {
        PassthroughInterserverInterlock wrapper = new PassthroughInterserverInterlock(this.sender);
        passive.sendMessageToServerFromActive(wrapper, message);
        boolean didSucceed = wrapper.waitForComplete();
        if (!didSucceed) {
          failingServers.add(passive);
        }
      }
    }
    
    // Now, decode the message and interpret it.
    switch (type) {
      case CREATE_ENTITY: {
        long clientOriginID = this.sender.getClientOriginID();
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        long version = input.readLong();
        byte[] serializedConfiguration = new byte[input.readInt()];
        input.readFully(serializedConfiguration);
        byte[] response = null;
        EntityException error = null;
        
        boolean didAlreadyHandle = false;
        try {
          // There is no response on successful create.
          didAlreadyHandle = this.lifeCycleMessageHandler.didAlreadyHandle(clientOriginID, transactionID);
        } catch (EntityException e) {
          error = e;
          didAlreadyHandle = true;
        }
        
        if (!didAlreadyHandle) {
          try {
            // There is no response on successful create.
            this.messageHandler.create(entityClassName, entityName, version, serializedConfiguration);
          } catch (EntityException e) {
            error = e;
          } catch (RuntimeException e) {
            // Just wrap this as a user exception since it was unexpected.
            error = new EntityServerException(entityClassName, entityName, e.getLocalizedMessage(), e);
          }
          
          // Record the result in order to handle future re-sends.
          if (null == error) {
            // No special result.
            this.lifeCycleMessageHandler.successInMessage(clientOriginID, transactionID, oldestTransactionID, null);
          } else {
            this.lifeCycleMessageHandler.failureInMessage(clientOriginID, transactionID, oldestTransactionID, error);
          }
        }
        // Before sending the complete, determine how to handle the case where there is an inconsistency across the stripe.
        boolean shouldSendResponse = handleConsensus(failingServers, error);
        if (shouldSendResponse) {
          sendCompleteResponse(sender, transactionID, response, error);
        }
        break;
      }
      case RECONFIGURE_ENTITY: {
        long clientOriginID = this.sender.getClientOriginID();
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        long version = input.readLong();
        byte[] serializedConfiguration = new byte[input.readInt()];
        input.readFully(serializedConfiguration);
        byte[] response = null;
        EntityException error = null;
        
        boolean didAlreadyHandle = false;
        try {
          // There is no response on successful create.
          response = this.lifeCycleMessageHandler.didAlreadyHandleResult(clientOriginID, transactionID);
          if (null != response) {
            didAlreadyHandle = true;
          }
        } catch (EntityException e) {
          error = e;
          didAlreadyHandle = true;
        }
        
        if (!didAlreadyHandle) {
          try {
            // We response with the previous configuration.
            response = this.messageHandler.reconfigure(entityClassName, entityName, version, serializedConfiguration);
          } catch (EntityException e) {
            error = e;
          } catch (RuntimeException e) {
            // Just wrap this as a user exception since it was unexpected.
            error = new EntityServerException(entityClassName, entityName, e.getLocalizedMessage(), e);
          }
          
          // Record the result in order to handle future re-sends.
          if (null == error) {
            this.lifeCycleMessageHandler.successInMessage(clientOriginID, transactionID, oldestTransactionID, response);
          } else {
            this.lifeCycleMessageHandler.failureInMessage(clientOriginID, transactionID, oldestTransactionID, error);
          }
        }
        // Before sending the complete, determine how to handle the case where there is an inconsistency across the stripe.
        boolean shouldSendResponse = handleConsensus(failingServers, error);
        if (shouldSendResponse) {
          sendCompleteResponse(sender, transactionID, response, error);
        }
        break;
      }      
      case DESTROY_ENTITY: {
        long clientOriginID = this.sender.getClientOriginID();
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        byte[] response = null;
        EntityException error = null;
        
        try {
          // There is no response on successful create.
          response = this.lifeCycleMessageHandler.didAlreadyHandleResult(clientOriginID, transactionID);
        } catch (EntityException e) {
          error = e;
        }
        
        if (response == null && error == null) {
          try {
            // There is no response on successful delete.
            boolean did = this.messageHandler.destroy(entityClassName, entityName);
            response = new byte[1];
            response[0] = did ? (byte)1 : 0;
          } catch (EntityException e) {
            error = e;
          } catch (RuntimeException e) {
            // Just wrap this as a user exception since it was unexpected.
            error = new EntityServerException(entityClassName, entityName, e.getLocalizedMessage(), e);
          }
          
          // Record the result in order to handle future re-sends.
          if (null == error) {
            // No special result.
            this.lifeCycleMessageHandler.successInMessage(clientOriginID, transactionID, oldestTransactionID, response);
          } else {
            this.lifeCycleMessageHandler.failureInMessage(clientOriginID, transactionID, oldestTransactionID, error);
          }
        }
        Assert.assertTrue(response != null || error != null);
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
          public void onFetchComplete(byte[] config, EntityException error) {
            sendCompleteResponse(sender, transactionID, config, error);
          }
        };
        try {
          this.messageHandler.fetch(sender, clientInstanceID, entityClassName, entityName, version, onFetch);
        } catch (RuntimeException e) {
          // Just wrap this as a user exception since it was unexpected.
          EntityException error = new EntityServerException(entityClassName, entityName, e.getLocalizedMessage(), e);
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
        EntityException error = null;
        try {
          // There is no response on successful delete.
          this.messageHandler.release(sender, clientInstanceID, entityClassName, entityName);
        } catch (EntityException e) {
          error = e;
        } catch (RuntimeException e) {
          // Just wrap this as a user exception since it was unexpected.
          error = new EntityServerException(entityClassName, entityName, e.getLocalizedMessage(), e);
        }
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case UNEXPECTED_RELEASE: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        long clientInstanceID = input.readLong();
        try {
          // We don't even want to hand back an error, in this case, since the call is just to emulate a disconnect.
          this.messageHandler.release(sender, clientInstanceID, entityClassName, entityName);
        } catch (Exception e) {
          // If there is an error, it is fatal.
          Assert.unexpected(e);
        }
        // Just send the empty response.
        byte[] response = null;
        EntityException error = null;
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
        EntityException error = null;
        try {
          // We respond with the config, if found.
          response = this.messageHandler.invoke(sender,
                                                clientInstanceID,
                                                transactionID,
                                                oldestTransactionID,
                                                entityClassName,
                                                entityName,
                                                payload);
        } catch (EntityException e) {
          error = e;
        } catch (RuntimeException e) {
          e.printStackTrace();
          // Just wrap this as a user exception since it was unexpected.
          error = new EntityServerException(entityClassName, entityName, e.getLocalizedMessage(), e);
        }
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case ACK_FROM_SERVER:
      case COMPLETE_FROM_SERVER:
      case EXCEPTION_FROM_SERVER:
      case MONITOR_MESSAGE:
      case MONITOR_EXCEPTION:
      case RETIRE_FROM_SERVER:
      case INVOKE_ON_CLIENT:
        // Not invoked on server.
        Assert.unreachable();
        break;
      case RECONNECT: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        long clientInstanceID = input.readLong();
        int extendedDataLength = input.readInt();
        byte[] extendedData = new byte[extendedDataLength];
        input.readFully(extendedData);
        
        // This is similar to FETCH but fully synchronous since we can't wait for lock on reconnect.
        byte[] response = null;
        EntityException error = null;
        try {
          this.messageHandler.reconnect(sender, clientInstanceID, entityClassName, entityName, extendedData);
          // No response;
          response = new byte[0];
        } catch (RuntimeException e) {
          // Just wrap this as a user exception since it was unexpected.
          error = new EntityServerException(entityClassName, entityName, e.getLocalizedMessage(), e);
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
        EntityException error = null;
        try {
          this.messageHandler.create(entityClassName, entityName, version, serializedConfiguration);
          this.messageHandler.syncEntityStart(sender, entityClassName, entityName);
        } catch (EntityException e) {
          error = e;
        } catch (RuntimeException e) {
          // Just wrap this as a user exception since it was unexpected.
          error = new EntityServerException(entityClassName, entityName, e.getLocalizedMessage(), e);
        }
        // Note that there is no response for sync messages.
        byte[] response = null;
        sendCompleteResponse(sender, transactionID, response, error);
        break;
      }
      case SYNC_ENTITY_END: {
        String entityClassName = input.readUTF();
        String entityName = input.readUTF();
        EntityException error = null;
        try {
          this.messageHandler.syncEntityEnd(sender, entityClassName, entityName);
        } catch (EntityException e) {
          error = e;
        } catch (RuntimeException e) {
          // Just wrap this as a user exception since it was unexpected.
          error = new EntityServerException(entityClassName, entityName, e.getLocalizedMessage(), e);
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
        EntityException error = null;
        try {
          this.messageHandler.syncEntityKeyStart(sender, entityClassName, entityName, concurrencyKey);
        } catch (EntityException e) {
          error = e;
        } catch (RuntimeException e) {
          // Just wrap this as a user exception since it was unexpected.
          error = new EntityServerException(entityClassName, entityName, e.getLocalizedMessage(), e);
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
        EntityException error = null;
        try {
          this.messageHandler.syncEntityKeyEnd(sender, entityClassName, entityName, concurrencyKey);
        } catch (EntityException e) {
          error = e;
        } catch (RuntimeException e) {
          // Just wrap this as a user exception since it was unexpected.
          error = new EntityServerException(entityClassName, entityName, e.getLocalizedMessage(), e);
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
        EntityException error = null;
        try {
          this.messageHandler.syncPayload(sender, entityClassName, entityName, concurrencyKey, payload);
        } catch (EntityException e) {
          error = e;
        } catch (RuntimeException e) {
          // Just wrap this as a user exception since it was unexpected.
          error = new EntityServerException(entityClassName, entityName, e.getLocalizedMessage(), e);
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

  /**
   * 
   * @param failingServers
   * @param error
   * @return True if the response should be sent (false implies that we are the one being crashed so we shouldn't respond).
   */
  private boolean handleConsensus(Set<PassthroughServerProcess> failingServers, EntityException error) {
    boolean shouldSendResponse = true;
    if (null == error) {
      // This was a success so kill any other servers which don't agree.
      for (PassthroughServerProcess serverProcess : failingServers) {
        this.crasher.terminateServerProcess(serverProcess);
      }
    } else {
      // This was a failure so see if everyone failed.  If anyone succeeded, kill all failing servers and then ourself.
      if (this.downstreamPassives.size() != failingServers.size()) {
        for (PassthroughServerProcess serverProcess : failingServers) {
          this.crasher.terminateServerProcess(serverProcess);
          shouldSendResponse = false;
        }
        this.crasher.terminateServerProcess(this.thisServer);
      }
    }
    return shouldSendResponse;
  }

  private void sendCompleteResponse(IMessageSenderWrapper sender, long transactionID, byte[] response, EntityException error) {
    if(error != null) {
//      System.err.println("ERROR: An error occured while processing message " + transactionID + " from " + sender.getClientOriginID());
//      error.printStackTrace();
    }
    PassthroughMessage complete = PassthroughMessageCodec.createCompleteMessage(response, error);
    // The oldestTransactionID isn't relevant when sent back.
    long oldestTransactionID = -1;
    complete.setTransactionTracking(transactionID, oldestTransactionID);
    sender.sendComplete(complete, true);
    
    // Note that we will create the retire message, as well, at this point.  At this level, we don't distinguish between
    // "complete" and "retire" since the operation is logically "done".
    // We just need to create both message instances and pass them back to the sender where it will determine if the retire
    // should be sent immediately, or held until later.
    PassthroughMessage retire = PassthroughMessageCodec.createRetireMessage();
    retire.setTransactionTracking(transactionID, oldestTransactionID);
    sender.sendRetire(retire);
  }


  /**
   * This interface handles the result of the message processing, exposing high-level methods to be called to satisfy the
   * meaning of a message.
   */
  public static interface MessageHandler {
    void create(String entityClassName, String entityName, long version, byte[] serializedConfiguration) throws EntityException;
    byte[] reconfigure(String entityClassName, String entityName, long version, byte[] serializedConfiguration) throws EntityException;
    boolean destroy(String entityClassName, String entityName) throws EntityException;
    void fetch(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName, long version, IFetchResult onFetch);
    void release(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName) throws EntityException;
    byte[] invoke(IMessageSenderWrapper sender,
                  long clientInstanceID,
                  long transactionId,
                  long eldestTransactionId,
                  String entityClassName,
                  String entityName,
                  byte[] payload) throws EntityException;
    void reconnect(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName, byte[] extendedData);
    void syncEntityStart(IMessageSenderWrapper sender, String entityClassName, String entityName) throws EntityException;
    void syncEntityEnd(IMessageSenderWrapper sender, String entityClassName, String entityName) throws EntityException;
    void syncEntityKeyStart(IMessageSenderWrapper sender, String entityClassName, String entityName, int concurrencyKey) throws EntityException;
    void syncEntityKeyEnd(IMessageSenderWrapper sender, String entityClassName, String entityName, int concurrencyKey) throws EntityException;
    void syncPayload(IMessageSenderWrapper sender, String entityClassName, String entityName, int concurrencyKey, byte[] payload) throws EntityException;
  }


  /**
   * Used to ensure that calls to create/destroy/doesExist give consistent results, on re-send.
   */
  public static interface LifeCycleMessageHandler {
    boolean didAlreadyHandle(long clientOriginID, long transactionID) throws EntityException;
    // Like didAlreadyHandle but used only in cases where there is a result - a null return means that this was not previously handled.
    byte[] didAlreadyHandleResult(long clientOriginID, long transactionID) throws EntityException;
    void failureInMessage(long clientOriginID, long transactionID, long oldestTransactionID, EntityException error);
    void successInMessage(long clientOriginID, long transactionID, long oldestTransactionID, byte[] reconfigureResponse);
  }
}
