/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

/**
 * Manages object id requests to servers
 */
public class RemoteObjectIDBatchSequenceProvider extends AbstractEventHandler implements BatchSequenceProvider {
  private final ObjectIDBatchRequestMessageFactory mf;
  private volatile BatchSequenceReceiver           receiver;

  public RemoteObjectIDBatchSequenceProvider(ObjectIDBatchRequestMessageFactory mf) {
    this.mf = mf;
  }

  public void setBatchSequenceReceiver(BatchSequenceReceiver receiver) {
    this.receiver = receiver;
  }

  public void requestBatch(BatchSequenceReceiver r, int size) {
    Assert.assertTrue(receiver == r);
    ObjectIDBatchRequestMessage m = mf.newObjectIDBatchRequestMessage();
    m.initialize(size);
    m.send();
  }

  public void handleEvent(EventContext context) {
    ObjectIDBatchRequestResponseMessage m = (ObjectIDBatchRequestResponseMessage) context;
    receiver.setNextBatch(m.getBatchStart(), m.getBatchEnd());
  }

}