/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.idprovider.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.msg.ObjectIDBatchRequestMessage;
import com.tc.object.msg.ObjectIDBatchRequestMessageFactory;
import com.tc.object.msg.ObjectIDBatchRequestResponseMessage;
import com.tc.util.Assert;
import com.tc.util.sequence.BatchSequenceProvider;
import com.tc.util.sequence.BatchSequenceReceiver;

import java.util.HashMap;
import java.util.Map;

/**
 * @author steve manages requests to servers
 */
public class RemoteObjectIDBatchSequenceProvider extends AbstractEventHandler implements BatchSequenceProvider {
  private long                                     requestID;
  private Map                                      requests = new HashMap();
  private final ObjectIDBatchRequestMessageFactory mf;

  public RemoteObjectIDBatchSequenceProvider(ObjectIDBatchRequestMessageFactory mf) {
    this.mf = mf;
  }

  public synchronized void requestBatch(BatchSequenceReceiver receiver, int size) {
    requests.put(new Long(requestID), new Request(receiver, size, requestID));
    ObjectIDBatchRequestMessage m = mf.newObjectIDBatchRequestMessage();
    m.initialize(requestID++, size);
    m.send();
  }

  private static class Request {
    private BatchSequenceReceiver receiver;

    private Request(BatchSequenceReceiver receiver, int batchSize, long requestID) {
      this.receiver = receiver;
    }

    public BatchSequenceReceiver getReceiver() {
      return receiver;
    }
  }

  public void handleEvent(EventContext context) {
    ObjectIDBatchRequestResponseMessage m = (ObjectIDBatchRequestResponseMessage) context;
    Long reqID = new Long(m.getRequestID());
    Request r = (Request) requests.get(reqID);
    Assert.eval(r != null);
    requests.remove(reqID);
    r.getReceiver().setNextBatch(m.getBatchStart(), m.getBatchEnd());
  }
}