/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.l2.ha.L2HADisabledCooridinator;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.handler.GlobalTransactionIDBatchRequestHandler;
import com.tc.objectserver.handler.GlobalTransactionIDBatchRequestHandler.GlobalTransactionIDBatchRequestContext;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.sequence.BatchSequenceReceiver;

public class GlobalTransactionIDBatchRequestHandlerTest extends TCTestCase {

  private GlobalTransactionIDBatchRequestHandler provider;
  private TestBatchSequenceReceiver              receiver;
  private TestMutableSequence                    persistentSequence;
  private TestSink                               requestBatchSink;

  @Override
  public void setUp() throws Exception {
    persistentSequence = new TestMutableSequence();
    requestBatchSink = new TestSink();

    provider = new GlobalTransactionIDBatchRequestHandler(persistentSequence);
    provider.setRequestBatchSink(requestBatchSink);

    TestServerConfigurationContext scc = new TestServerConfigurationContext();
    scc.l2Coordinator = new L2HADisabledCooridinator();
    provider.initializeContext(scc);

    receiver = new TestBatchSequenceReceiver();
  }

  public void testx() throws Exception {
    int batchSize = 5;
    // make sure that the request context gets put in the sink properly.
    provider.requestBatch(receiver, batchSize);
    GlobalTransactionIDBatchRequestContext ctxt = (GlobalTransactionIDBatchRequestContext) requestBatchSink
        .getInternalQueue().remove(0);
    assertTrue(requestBatchSink.getInternalQueue().isEmpty());
    assertSame(receiver, ctxt.getReceiver());
    assertEquals(batchSize, ctxt.getBatchSize());

    // now check the handler interface to make sure it handles the request properly
    provider.handleEvent(ctxt);

    // make sure it called the right thing on the sequence
    Object[] args = (Object[]) persistentSequence.nextBatchQueue.poll(1);
    assertEquals(Integer.valueOf(batchSize), args[0]);

    // make sure it called back on the receiver
    args = (Object[]) receiver.nextBatchQueue.poll(1);
    assertEquals(Long.valueOf(persistentSequence.sequence - batchSize), args[0]);
    assertEquals(Long.valueOf(persistentSequence.sequence), args[1]);
  }

  private static final class TestBatchSequenceReceiver implements BatchSequenceReceiver {

    public final NoExceptionLinkedQueue nextBatchQueue = new NoExceptionLinkedQueue();

    public void setNextBatch(long start, long end) {
      nextBatchQueue.put(new Object[] { Long.valueOf(start), Long.valueOf(end) });
    }

    public boolean isBatchRequestPending() {
      return true;
    }

  }
}
