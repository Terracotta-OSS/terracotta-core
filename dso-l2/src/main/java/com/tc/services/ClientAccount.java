package com.tc.services;

import com.tc.entity.ServerEntityMessage;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.EntityDescriptor;

import java.util.HashMap;
import java.util.Map;

public class ClientAccount {
  private final MessageChannel channel;
  private final Map<Long, ResponseWaiter> waitingResponse = new HashMap<>();
  private volatile boolean open = true;
  private long responseId = 0;

  ClientAccount(MessageChannel channel) {
    this.channel = channel;
  }

  synchronized ResponseWaiter send(EntityDescriptor entityDescriptor, byte[] payload) {
    ResponseWaiter responseWaiter = new ResponseWaiter();
    if (!open) {
      responseWaiter.done();
    } else {
      waitingResponse.put(responseId, responseWaiter);
      ServerEntityMessage message = (ServerEntityMessage) channel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE);
      message.setMessage(entityDescriptor, payload, responseId++);
      message.send();
    }
    return responseWaiter;
  }

  synchronized void sendNoResponse(EntityDescriptor entityDescriptor, byte[] payload) {
    if (open) {
      ServerEntityMessage message = (ServerEntityMessage) channel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE);
      message.setMessage(entityDescriptor, payload);
      message.send();
    }
  }

  synchronized void close() {
    open = false;
    for (ResponseWaiter responseWaiter : waitingResponse.values()) {
      // Client closed, whether or not it received the message is not important anymore since it's gone.
      responseWaiter.done();
    }
  }

  synchronized void response(long responseId) {
    if (open) {
      ResponseWaiter responseWaiter = waitingResponse.remove(responseId);
      if (responseWaiter != null) {
        responseWaiter.done();
      }
    }
  }
}
