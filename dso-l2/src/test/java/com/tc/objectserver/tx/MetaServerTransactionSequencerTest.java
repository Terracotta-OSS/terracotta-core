/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.objectserver.context.TransactionLookupContext;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class MetaServerTransactionSequencerTest extends TestCase {

  MetaServerTransactionSequencerImpl sequencer;
  private int                        bid;
  private int                        sid;

  @Override
  protected void setUp() throws Exception {
    sequencer = new MetaServerTransactionSequencerImpl();
  }

  public void testBasic() throws Exception {
    List txnContexts = createSomeTxnsFrom(new ClientID(5), 10);
    sequencer.addTransactionLookupContexts(txnContexts);
    assertEquals(1, sequencer.getTxnSequencersCount());

    txnContexts = createSomeTxnsFrom(new ClientID(6), 10);
    sequencer.addTransactionLookupContexts(txnContexts);
    assertEquals(2, sequencer.getTxnSequencersCount());

    txnContexts = createSomeTxnsFrom(new ClientID(5), 10);
    sequencer.addTransactionLookupContexts(txnContexts);
    assertEquals(2, sequencer.getTxnSequencersCount());

    txnContexts = createSomeTxnsFrom(new ClientID(5), 10);
    List txnContexts1 = createSomeTxnsFrom(new ClientID(7), 10);
    txnContexts.addAll(txnContexts1);
    sequencer.addTransactionLookupContexts(txnContexts);
    assertEquals(3, sequencer.getTxnSequencersCount());

    int count = 50;

    // Try to test without depending on the order inwhich we will be getting the transactions (ie between ClientID 5 6
    // and 7)
    TransactionLookupContext txnContext = sequencer.getNextTxnLookupContextToProcess();
    assertNotNull(txnContext);
    TransactionLookupContext txnContext1;
    count--;
    for (int i = 0; i < 4; i++) {
      txnContext1 = sequencer.getNextTxnLookupContextToProcess();
      count--;
      assertEquals(txnContext.getSourceID(), txnContext1.getSourceID());
    }

    txnContext = sequencer.getNextTxnLookupContextToProcess();
    count--;
    assertNotNull(txnContext);
    sequencer.makePending(txnContext.getTransaction());

    // because we make pending (and because of the way we create txns in this test) the next context should be from the
    // next client
    txnContext1 = sequencer.getNextTxnLookupContextToProcess();
    count--;
    assertNotNull(txnContext1);
    assertFalse(txnContext.getSourceID().equals(txnContext1.getSourceID()));

    for (int i = 0; i < 4; i++) {
      txnContext1 = sequencer.getNextTxnLookupContextToProcess();
      count--;
      assertNotNull(txnContext1);
    }
    sequencer.makePending(txnContext1.getTransaction());

    TransactionLookupContext txnContext2 = sequencer.getNextTxnLookupContextToProcess();
    count--;
    assertNotNull(txnContext2);
    assertFalse(txnContext1.getSourceID().equals(txnContext2.getSourceID()));

    while (sequencer.getNextTxnLookupContextToProcess() != null) {
      count--;
    }

    for (int i = 0; i < 40; i++) {
      txnContext2 = sequencer.getNextTxnLookupContextToProcess();
      assertNull(txnContext2);
    }

    // now unblock first one
    sequencer.makeUnpending(txnContext.getTransaction());
    txnContext2 = sequencer.getNextTxnLookupContextToProcess();
    count--;
    assertNotNull(txnContext2);
    assertEquals(txnContext.getSourceID(), txnContext2.getSourceID());

    while (sequencer.getNextTxnLookupContextToProcess() != null) {
      count--;
    }

    for (int i = 0; i < 40; i++) {
      txnContext2 = sequencer.getNextTxnLookupContextToProcess();
      assertNull(txnContext2);
    }

    // now unblock second one
    sequencer.makeUnpending(txnContext1.getTransaction());
    txnContext2 = sequencer.getNextTxnLookupContextToProcess();
    count--;
    assertNotNull(txnContext2);
    assertEquals(txnContext1.getSourceID(), txnContext2.getSourceID());

    while (sequencer.getNextTxnLookupContextToProcess() != null) {
      count--;
    }

    assertEquals(0, count);

  }

  private List createSomeTxnsFrom(ClientID clientID, int count) {
    TestServerTransaction stxn = new TestServerTransaction(new ServerTransactionID(clientID, new TransactionID(sid++)),
                                                           new TxnBatchID(bid++));
    stxn.oids.add(new ObjectID(clientID.toLong()));

    ArrayList txns = new ArrayList();
    while (count-- > 0) {
      txns.add(new TransactionLookupContext(stxn, true));
    }
    return txns;
  }

}
