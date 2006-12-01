/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class TransactionBatchAccountingTest extends TestCase {
  private TransactionBatchAccounting acct;
  private Sequence                   sequence;

  public void setUp() throws Exception {
    acct = new TransactionBatchAccounting();
    sequence = new Sequence();
  }

  public void testBasics() throws Exception {
    final List incompleteBatchIDs = new LinkedList();
    final Set completedGlobalTransactionIDs = new HashSet();
    // try adding an empty batch
    Batch batch1 = new Batch(new TxnBatchID(sequence.next()));
    acct.addBatch(batch1.batchID, batch1.transactionIDs);
    // there should be no completed transactions
    assertEquals(Collections.EMPTY_SET, acct.addCompletedTransactionIDsTo(new HashSet()));
    // there should be no incomplete batches
    assertEquals(Collections.EMPTY_LIST, acct.addIncompleteBatchIDsTo(new LinkedList()));
    // the min incomplete batch id should be the null id.
    assertEquals(TxnBatchID.NULL_BATCH_ID, acct.getMinIncompleteBatchID());

    // try adding a batch with a two transactions
    TransactionID txID1 = new TransactionID(sequence.next());
    TransactionID txID2 = new TransactionID(sequence.next());

    Batch batch2 = new Batch(new TxnBatchID(sequence.next()));
    batch2.addTransactionID(txID1);
    batch2.addTransactionID(txID2);
    incompleteBatchIDs.add(batch2.batchID);
    acct.addBatch(batch2.batchID, batch2.transactionIDs);

    TransactionID txID3 = new TransactionID(sequence.next());
    Batch batch3 = new Batch(new TxnBatchID(sequence.next()));
    batch3.addTransactionID(txID3);
    incompleteBatchIDs.add(batch3.batchID);
    acct.addBatch(batch3.batchID, batch3.transactionIDs);

    TransactionID txID4 = new TransactionID(sequence.next());
    Batch batch4 = new Batch(new TxnBatchID(sequence.next()));
    batch4.addTransactionID(txID4);
    incompleteBatchIDs.add(batch4.batchID);
    acct.addBatch(batch4.batchID, batch4.transactionIDs);

    // there should be no completed transactions, since there were no acknowledgements
    assertEquals(Collections.EMPTY_SET, acct.addCompletedTransactionIDsTo(new HashSet()));
    // check the incomplete batches
    assertEquals(incompleteBatchIDs, acct.addIncompleteBatchIDsTo(new LinkedList()));
    assertEquals(incompleteBatchIDs.get(0), acct.getMinIncompleteBatchID());

    // ACK the first transaction in the multi-transaction batch
    assertEquals(TxnBatchID.NULL_BATCH_ID, acct.acknowledge(txID1));
    completedGlobalTransactionIDs.add(txID1);
    // there should still be no completed batches
    assertEquals(incompleteBatchIDs, acct.addIncompleteBatchIDsTo(new LinkedList()));
    assertEquals(completedGlobalTransactionIDs, acct.addCompletedTransactionIDsTo(new HashSet()));

    // ACK the last transaction in the multi-transaction batch. This should cause that batch to become complete AND
    // cause all of its constituent transactions to become completed.
    assertEquals(batch2.batchID, acct.acknowledge(txID2));
    incompleteBatchIDs.remove(batch2.batchID);
    assertEquals(incompleteBatchIDs, acct.addIncompleteBatchIDsTo(new LinkedList()));
    assertEquals(incompleteBatchIDs.get(0), acct.getMinIncompleteBatchID());
    assertEquals(batch2.transactionIDs, acct.addCompletedTransactionIDsTo(new HashSet()));
    assertEquals(batch2.transactionIDs, acct.addCompletedTransactionIDsTo(new HashSet()));

    // now clear the completed transactions
    acct.clearCompletedTransactionIds();
    completedGlobalTransactionIDs.clear();
    assertEquals(completedGlobalTransactionIDs, acct.addCompletedTransactionIDsTo(new HashSet()));

    // ACK another transaction
    assertEquals(batch3.batchID, acct.acknowledge(txID3));
    completedGlobalTransactionIDs.addAll(batch3.transactionIDs);
    incompleteBatchIDs.remove(batch3.batchID);
    assertEquals(incompleteBatchIDs, acct.addIncompleteBatchIDsTo(new LinkedList()));
    assertEquals(incompleteBatchIDs.get(0), acct.getMinIncompleteBatchID());
    assertEquals(completedGlobalTransactionIDs, acct.addCompletedTransactionIDsTo(new HashSet()));

    // ACK the last transaction
    assertEquals(batch4.batchID, acct.acknowledge(txID4));
    completedGlobalTransactionIDs.addAll(batch4.transactionIDs);
    incompleteBatchIDs.remove(batch4.batchID);
    assertEquals(Collections.EMPTY_LIST, incompleteBatchIDs);
    assertEquals(incompleteBatchIDs, acct.addIncompleteBatchIDsTo(new LinkedList()));
    assertEquals(TxnBatchID.NULL_BATCH_ID, acct.getMinIncompleteBatchID());
    assertEquals(completedGlobalTransactionIDs, acct.addCompletedTransactionIDsTo(new HashSet()));

    acct.clearCompletedTransactionIds();
    completedGlobalTransactionIDs.clear();
    assertEquals(completedGlobalTransactionIDs, acct.addCompletedTransactionIDsTo(new HashSet()));
  }

  /**
   * Tests that the set of incomplete batch ids comes out in the same order it come in.
   */
  public void testBatchOrdering() throws Exception {
    List batchIDs = new LinkedList();
    for (int i = 0; i < 1000; i++) {
      TxnBatchID batchID = new TxnBatchID(sequence.next());
      Set transactionIDs = new HashSet();
      for (int j = 0; j < 10; j++) {
        transactionIDs.add(new TransactionID(sequence.next()));
      }
      acct.addBatch(batchID, transactionIDs);
      batchIDs.add(batchID);

      assertEquals(batchIDs, acct.addIncompleteBatchIDsTo(new LinkedList()));
    }
  }

  private static final class Sequence {
    private SynchronizedLong sequence = new SynchronizedLong(0);

    public long next() {
      return sequence.increment();
    }
  }

  private static final class Batch {

    private final TxnBatchID batchID;
    private final Set        transactionIDs = new HashSet();

    public Batch(TxnBatchID batchID) {
      this.batchID = batchID;
    }

    public void addTransactionID(TransactionID txID) {
      transactionIDs.add(txID);
    }
  }

}
