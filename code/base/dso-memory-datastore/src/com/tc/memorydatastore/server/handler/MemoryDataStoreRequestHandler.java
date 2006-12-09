/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.memorydatastore.server.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.memorydatastore.message.MemoryDataStoreRequestMessage;
import com.tc.memorydatastore.message.MemoryDataStoreResponseMessage;
import com.tc.memorydatastore.server.MemoryDataStore;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.util.Assert;

import java.util.Collection;

public class MemoryDataStoreRequestHandler extends AbstractEventHandler {
  private final static String   DATA_STORE_NAME_ATTACHMENT_KEY = "DataStoreName";

  private final MemoryDataStore store                          = new MemoryDataStore();
  
  private long numOfRequestsProcessed = 0;
  private long totalTimeProcessed = 0; 


  public void handleEvent(EventContext context) throws EventHandlerException {
    long startTime = System.currentTimeMillis();
    
    MemoryDataStoreRequestMessage message = (MemoryDataStoreRequestMessage)context;
    MemoryDataStoreRequestMessage dataStoreRequestMessage = (MemoryDataStoreRequestMessage) message;

    MessageChannel channel = message.getChannel();

    serviceRequest(channel, dataStoreRequestMessage);

    long endTime = System.currentTimeMillis();
    
    synchronized(this) {
      totalTimeProcessed += (endTime - startTime);
      numOfRequestsProcessed++;
      if (numOfRequestsProcessed % 10000 == 0) {
        System.err.println(numOfRequestsProcessed + " requests processed with average processing time: " + (totalTimeProcessed*1.0/numOfRequestsProcessed) + "ms");
      }
    }
  }

  private void serviceRequest(MessageChannel channel, MemoryDataStoreRequestMessage requestMessage) {
    int type = requestMessage.getType();
    String dataStoreName = getDataStoreName(channel, requestMessage);

    byte[] key = requestMessage.getKey();
    byte[] value = requestMessage.getValue();

    switch (type) {
    case MemoryDataStoreRequestMessage.PUT:
      Assert.assertNotNull(key);
      Assert.assertNotNull(value);

      //System.err.println("MemoryDataStore -- type: " + type + ", key: " + new String(key) + ", value: "
      //    + new String(value));

      store.put(dataStoreName, key, value);
      sendPutResponseMessage(channel, requestMessage.getThreadID(), true);
      break;
    case MemoryDataStoreRequestMessage.GET:
      Assert.assertNotNull(key);

      if (requestMessage.isGetAll()) {
        Collection values = store.getAll(dataStoreName, key);
        //System.err.println("MemoryDataStore -- type: " + type + ", size of returned collection: " + values.size());
        sendGetAllResponseMessage(channel, requestMessage.getThreadID(), values, true);
      } else {
        value = store.get(dataStoreName, key);
        //System.err.println("MemoryDataStore -- type: " + type + ", return value: "
        //    + (value != null ? new String(value) : null));
        sendGetResponseMessage(channel, requestMessage.getThreadID(), value, true);
      }
      break;
    case MemoryDataStoreRequestMessage.REMOVE:
      Assert.assertNotNull(key);

      if (requestMessage.isRemoveAll()) {
        int numOfRemove = store.removeAll(dataStoreName, key);
        //System.err.println("MemoryDataStore -- type: " + type + ", return num of remove: " + numOfRemove);
        sendRemoveAllResponseMessage(channel, requestMessage.getThreadID(), numOfRemove, true);
      } else {
        value = store.remove(dataStoreName, key);
        //System.err.println("MemoryDataStore -- type: " + type + ", return value: "
        //    + (value != null ? new String(value) : null));
        sendRemoveResponseMessage(channel, requestMessage.getThreadID(), value, true);
      }
      break;
    }

  }

  private String getDataStoreName(MessageChannel channel, MemoryDataStoreRequestMessage requestMessage) {
    String dataStoreName = (String) channel.getAttachment(DATA_STORE_NAME_ATTACHMENT_KEY);
    if (dataStoreName == null) {
      dataStoreName = requestMessage.getDataStoreName();
      channel.addAttachment(DATA_STORE_NAME_ATTACHMENT_KEY, dataStoreName, false);
    }
    return dataStoreName;
  }

  private void sendRemoveResponseMessage(MessageChannel channel, ThreadID threadID, byte[] value,
      boolean requestCompletedStatus) {
    MemoryDataStoreResponseMessage response = (MemoryDataStoreResponseMessage) channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_RESPONSE_MESSAGE);
    response.initializeRemoveResponse(threadID, value, requestCompletedStatus);
    response.send();
  }

  private void sendRemoveAllResponseMessage(MessageChannel channel, ThreadID threadID, int numOfRemove,
      boolean requestCompletedStatus) {
    MemoryDataStoreResponseMessage response = (MemoryDataStoreResponseMessage) channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_RESPONSE_MESSAGE);
    response.initializeRemoveAllResponse(threadID, numOfRemove, requestCompletedStatus);
    response.send();
  }

  private void sendGetResponseMessage(MessageChannel channel, ThreadID threadID, byte[] value,
      boolean requestCompletedStatus) {
    MemoryDataStoreResponseMessage response = (MemoryDataStoreResponseMessage) channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_RESPONSE_MESSAGE);
    response.initializeGetResponse(threadID, value, requestCompletedStatus);
    response.send();
  }
  
  private void sendGetAllResponseMessage(MessageChannel channel, ThreadID threadID, Collection values,
      boolean requestCompletedStatus) {
    MemoryDataStoreResponseMessage response = (MemoryDataStoreResponseMessage) channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_RESPONSE_MESSAGE);
    response.initializeGetAllResponse(threadID, values, requestCompletedStatus);
    response.send();
  }

  private void sendPutResponseMessage(MessageChannel channel, ThreadID threadID, boolean requestCompletedStatus) {
    MemoryDataStoreResponseMessage response = (MemoryDataStoreResponseMessage) channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_RESPONSE_MESSAGE);
    response.initializePutResponse(threadID, requestCompletedStatus);
    response.send();
  }


}
