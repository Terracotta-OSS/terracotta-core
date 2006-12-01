/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.impl.MockSink;
import com.tc.async.impl.MockStage;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.context.BatchedTransactionProcessingContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
import com.tc.objectserver.handler.BatchTransactionLookupHandler;
import com.tc.objectserver.impl.TestObjectManager;
import com.tc.test.TCTestCase;
import com.tc.util.SequenceID;
import com.tc.util.SequenceValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BatchedTransactionProcessorImplTest extends TCTestCase {

  private TestServerConfigurationContext  cctxt;
  private Map                             stageMap;
  private TestObjectManager               objectManager;
  private BatchTransactionLookupHandler   handler;
  private TestGlobalTransactionManager    gtxm;
  private SequenceValidator               sequenceValidator;
  private BatchedTransactionProcessorImpl batchTxnProcessor;
  private int                             sqID;
  private int                             txnID;
  private int                             batchID;
  private ChannelID                       channelID;

  public void setUp() throws Exception {
    txnID = 100;
    sqID = 100;
    batchID = 100;
    channelID = new ChannelID(0);
    objectManager = new TestObjectManager();
    gtxm = new TestGlobalTransactionManager();
    sequenceValidator = new SequenceValidator(sqID++);
    MockStage batchTxnStage = new MockStage(ServerConfigurationContext.BATCH_TRANSACTION_LOOKUP_STAGE);
    Sink batchTxnSink = batchTxnStage.getSink();

    cctxt = new TestServerConfigurationContext();
    stageMap = cctxt.stages;

    stageMap.put(ServerConfigurationContext.BATCH_TRANSACTION_LOOKUP_STAGE, batchTxnStage);
    handler = new BatchTransactionLookupHandler();
    MockStage applyStage = new MockStage(ServerConfigurationContext.APPLY_CHANGES_STAGE);
    stageMap.put(ServerConfigurationContext.APPLY_CHANGES_STAGE, applyStage);
    batchTxnProcessor = new BatchedTransactionProcessorImpl(-1, sequenceValidator, objectManager, gtxm, batchTxnSink);
    cctxt.txnBatchProcessor = batchTxnProcessor;

    handler.initialize(cctxt);
  }

  public void tests() throws Exception {

    List txns = createTxns(2);
    Collection completedTxnIds = getCompletedTxnIDs(5);

    // A request to lookup should be added to batch
    batchTxnProcessor.addTransactions(channelID, txns, completedTxnIds);

    MockSink batchTxnSink = (MockSink) ((Stage) stageMap.get(ServerConfigurationContext.BATCH_TRANSACTION_LOOKUP_STAGE))
        .getSink();
    MockSink applySink = (MockSink) ((Stage) stageMap.get(ServerConfigurationContext.APPLY_CHANGES_STAGE)).getSink();

    // make sure that a Transaction Queue is created and put into the Batch Transaction Lookup Stage
    assertFalse(batchTxnSink.queue.isEmpty());
    EventContext context = (EventContext) batchTxnSink.queue.remove(0);
    assertNotNull(context);

    assertTrue(applySink.queue.isEmpty());

    // send more txn and see that Transaction Queue is NOT added again to the stage
    List txns2 = createTxns(2);
    Collection completedTxnIds2 = getCompletedTxnIDs(5);
    completedTxnIds.addAll(completedTxnIds2);
    txns.addAll(txns2);

    batchTxnProcessor.addTransactions(channelID, txns2, completedTxnIds2);

    assertTrue(batchTxnSink.queue.isEmpty());
    assertTrue(applySink.queue.isEmpty());

    // Look up shouldnt have happened yet
    Object[] args = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.poll(100);
    assertNull(args);

    // Now process context and make sure that object lookups are done.
    handler.handleEvent(context);

    // Look up should have happened
    int count = txns.size();
    while (count-- > 0) {
      args = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.poll(100);
      assertNotNull(args);
    }

    // Batched Transaction context should have been added to applySink
    assertTrue(batchTxnSink.queue.isEmpty());
    assertFalse(applySink.queue.isEmpty());
    BatchedTransactionProcessingContext btp = (BatchedTransactionProcessingContext) applySink.queue.remove(0);
    assertNotNull(btp);
    assertTrue(btp.isClosed());
    assertEquals(completedTxnIds, btp.getCompletedTransactionIDs());

    assertEquals(txns.size(), btp.getTransactionsCount());
    assertEquals(txns, btp.getTxns());

    // make lookups pending...
    objectManager.makePending = true;

    txns = createTxns(2);
    completedTxnIds = getCompletedTxnIDs(5);

    // A request to lookup should be added to batch
    batchTxnProcessor.addTransactions(channelID, txns, completedTxnIds);

    // make sure that a Transaction Queue is put into the Batch Transaction Lookup Stage
    assertFalse(batchTxnSink.queue.isEmpty());
    context = (EventContext) batchTxnSink.queue.remove(0);
    assertNotNull(context);

    assertTrue(applySink.queue.isEmpty());

    // Now process context and make sure that object lookups are done.
    handler.handleEvent(context);

    // Look up should have happened yet
    args = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.poll(100);
    assertNotNull(args);

    assertTrue(batchTxnProcessor.isPending(channelID));

    // send more txn and see that Transaction Queue is NOT added again to the stage
    txns2 = createTxns(2);
    completedTxnIds2 = getCompletedTxnIDs(5);
    completedTxnIds.addAll(completedTxnIds2);
    txns.addAll(txns2);

    batchTxnProcessor.addTransactions(channelID, txns2, completedTxnIds2);

    assertTrue(batchTxnSink.queue.isEmpty());
    assertTrue(applySink.queue.isEmpty());

    // Now process pending
    objectManager.makePending = false;
    objectManager.processPending(args);

    // Transaction Queue should have been added to batchTxnSink
    assertFalse(batchTxnProcessor.isPending(channelID));
    assertFalse(batchTxnSink.queue.isEmpty());
    context = (EventContext) batchTxnSink.queue.remove(0);
    assertNotNull(context);

    assertTrue(applySink.queue.isEmpty());

    // Now process context and make sure that object lookups are done.
    handler.handleEvent(context);

    // Look up should have happened
    count = txns.size() - 1; // -1 coz one request when to pending
    while (count-- > 0) {
      args = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.poll(100);
      assertNotNull(args);
    }
    // No more requests
    args = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.poll(100);
    assertNull(args);

    // Batched Transaction context should have been added to applySink
    assertTrue(batchTxnSink.queue.isEmpty());
    assertFalse(applySink.queue.isEmpty());
    btp = (BatchedTransactionProcessingContext) applySink.queue.remove(0);
    assertNotNull(btp);
    assertTrue(btp.isClosed());
    assertEquals(completedTxnIds, btp.getCompletedTransactionIDs());

    assertEquals(txns.size(), btp.getTransactionsCount());
    assertEquals(txns, btp.getTxns());

    // make sure our context queues are empty...
    assertTrue(objectManager.lookupObjectForCreateIfNecessaryContexts.isEmpty());
    assertTrue(batchTxnSink.queue.isEmpty());
    assertTrue(applySink.queue.isEmpty());

    System.err.println("Test Done !!!!");
  }

  private Collection getCompletedTxnIDs(int count) {
    Set txnIds = new HashSet();
    while (count-- > 0) {
      txnIds.add(new ServerTransactionID(channelID, new TransactionID(txnID - 50 - count)));
    }
    return txnIds;
  }

  private List createTxns(int i) {
    List txns = new ArrayList();
    batchID++;
    while (i-- > 0) {
      txns.add(new ServerTransactionImpl(new TxnBatchID(batchID), new TransactionID(txnID++), new SequenceID(sqID++),
                                         new LockID[0], channelID, Collections.EMPTY_LIST,
                                         new ObjectStringSerializer(), Collections.EMPTY_MAP, TxnType.NORMAL,
                                         new LinkedList()));
    }
    return txns;
  }

}
