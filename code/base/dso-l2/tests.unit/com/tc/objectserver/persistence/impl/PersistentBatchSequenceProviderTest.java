/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.exception.ImplementMe;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.persistence.api.PersistentSequence;
import com.tc.objectserver.persistence.impl.PersistentBatchSequenceProvider.GlobalTransactionIDBatchRequestContext;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.sequence.BatchSequenceReceiver;

public class PersistentBatchSequenceProviderTest extends TCTestCase {
  
  public void tests() throws Exception {
    TestPersistentSequence persistentSequence = new TestPersistentSequence();
    TestSink requestBatchSink = new TestSink();

    PersistentBatchSequenceProvider provider = new PersistentBatchSequenceProvider(persistentSequence);
    provider.setRequestBatchSink(requestBatchSink);

    TestBatchSequenceReceiver receiver = new TestBatchSequenceReceiver();
    
    int batchSize = 5;
    // make sure that the request context gets put in the sink properly.
    provider.requestBatch(receiver, batchSize);
    GlobalTransactionIDBatchRequestContext ctxt = (GlobalTransactionIDBatchRequestContext) requestBatchSink.getInternalQueue().remove(0);
    assertTrue(requestBatchSink.getInternalQueue().isEmpty());
    assertSame(receiver, ctxt.getReceiver());
    assertEquals(batchSize, ctxt.getBatchSize());
    
    // now check the handler interface to make sure it handles the request properly
    provider.handleEvent(ctxt);

    // make sure it called the right thing on the sequence
    Object[] args = (Object[]) persistentSequence.nextBatchQueue.poll(1);
    assertEquals(new Integer(batchSize), args[0]);
    
    // make sure it called back on the receiver
    args = (Object[]) receiver.nextBatchQueue.poll(1);
    assertEquals(new Long(persistentSequence.sequence), args[0]);
    assertEquals(new Long(persistentSequence.sequence + batchSize), args[1]);
  }
  
  private static final class TestBatchSequenceReceiver implements BatchSequenceReceiver {
    
    public final NoExceptionLinkedQueue nextBatchQueue = new NoExceptionLinkedQueue();

    public void setNextBatch(long start, long end) {
      nextBatchQueue.put(new Object[] {new Long(start), new Long(end)});
    }

    public boolean hasNext() {
      return true;
    }
    
  }
  
  private static final class TestPersistentSequence implements PersistentSequence {

    public long sequence = 1;
    public final NoExceptionLinkedQueue nextBatchQueue = new NoExceptionLinkedQueue();
    
    public long next() {
      return sequence;
    }

    public long nextBatch(int batchSize) {
      nextBatchQueue.put(new Object[] {new Integer(batchSize) });
      return sequence;
    }

    public String getUID() {
      throw new ImplementMe();
    }
    
  }
  
}
