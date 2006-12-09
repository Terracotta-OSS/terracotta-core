/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.memorydatastore.client;

import com.tc.exception.TCRuntimeException;
import com.tc.memorydatastore.message.MemoryDataStoreResponseMessage;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.UnknownNameException;
import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.object.lockmanager.api.ThreadID;

import java.io.IOException;

public class MemoryDataStoreResponseSink implements TCMessageSink {
  private final MemoryDataStoreClient client;

  public MemoryDataStoreResponseSink(MemoryDataStoreClient client) {
    super();
    this.client = client;
  }

  public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {
    try {
      message.hydrate();
    } catch (UnknownNameException e) {
      throw new TCRuntimeException(e);
    } catch (IOException e) {
      throw new TCRuntimeException(e);
    }

    MemoryDataStoreResponseMessage responseMessage = (MemoryDataStoreResponseMessage) message;

    if (responseMessage.isGetResponse()) {
      ThreadID threadID = responseMessage.getThreadID();
      client.notifyResponse(threadID, responseMessage);
    }
  }

}
