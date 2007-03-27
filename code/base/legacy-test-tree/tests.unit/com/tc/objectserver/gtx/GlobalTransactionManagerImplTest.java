/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.impl.TestPersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.TestTransactionStore;
import com.tc.util.SequenceValidator;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

public class GlobalTransactionManagerImplTest extends TestCase {

  private TestTransactionStore               transactionStore;
  private TestPersistenceTransactionProvider ptxp;
  private ServerGlobalTransactionManager     gtxm;

  protected void setUp() throws Exception {
    super.setUp();
    transactionStore = new TestTransactionStore();
    ptxp = new TestPersistenceTransactionProvider();
    gtxm = new ServerGlobalTransactionManagerImpl(new SequenceValidator(0), transactionStore, ptxp);
  }

  public void testStartAndCommitApply() throws Exception {
    ServerTransactionID stxID1 = new ServerTransactionID(new ChannelID(1), new TransactionID(1));
    ServerTransactionID stxID2 = new ServerTransactionID(new ChannelID(1), new TransactionID(2));

    assertTrue(gtxm.needsApply(stxID1));
    assertTrue(gtxm.needsApply(stxID2));

    assertTrue(gtxm.needsApply(stxID1));
    assertTrue(gtxm.needsApply(stxID2));

    // now commit them
    gtxm.commit(null, stxID1);
    gtxm.commit(null, stxID2);

    // the apply has already been started, so no-one else should have to do it.
    assertFalse(gtxm.needsApply(stxID1));
    assertFalse(gtxm.needsApply(stxID2));

    // now try to commit again
    try {
      gtxm.commit(null, stxID1);
      fail("TransactionCommittedError");
    } catch (TransactionCommittedError e) {
      // expected
    }

    transactionStore.restart();
  }

  public void testReapplyTransactionsAcrossRestart() throws Exception {
    ChannelID channel1 = new ChannelID(1);
    TransactionID tx1 = new TransactionID(1);
    ServerTransactionID stxid = new ServerTransactionID(channel1, tx1);

    GlobalTransactionID gtxid = gtxm.getGlobalTransactionID(stxid);
    assertGlobalTxWasLoaded(stxid);

    assertTrue(gtxm.needsApply(stxid));
    assertGlobalTxWasLoaded(stxid);
    assertNextGlobalTxNotCalled();

    assertTrue(gtxm.needsApply(stxid));
    assertGlobalTxWasLoaded(stxid);
    assertNextGlobalTxNotCalled();

    // make sure no new calls to load were made
    assertGlobalTxWasNotLoaded();

    // RESTART
    transactionStore.restart();

    // the transaction is still not applied
    assertTrue(gtxm.needsApply(stxid));
    assertGlobalTxWasLoaded(stxid);
    assertNextGlobalTxNotCalled();

    // get a new global Txid number
    GlobalTransactionID gtxid2 = gtxm.getGlobalTransactionID(stxid);
    assertNotSame(gtxid, gtxid2);
    assertFalse(gtxid.equals(gtxid2));
    assertGlobalTxWasLoaded(stxid);

    gtxm.commit(null, stxid);
    assertGlobalTxWasLoaded(stxid);
    assertNextGlobalTXWasCalled(stxid);
    
    assertNotNull(transactionStore.commitContextQueue.poll(1));

    // no longer needs to be applied
    assertFalse(gtxm.needsApply(stxid));
    assertGlobalTxWasLoaded(stxid);
    assertNextGlobalTxNotCalled();

    // make sure no calls to store were made
    assertTrue(transactionStore.commitContextQueue.isEmpty());

    // RESTART
    transactionStore.restart();

    // make sure that it isn't in progress
    assertFalse(gtxm.needsApply(stxid));
    assertGlobalTxWasLoaded(stxid);
    assertNextGlobalTxNotCalled();

    // get a new global Txid number
    GlobalTransactionID gtxid3 = gtxm.getGlobalTransactionID(stxid);
    assertNotSame(gtxid2, gtxid3);
    assertFalse(gtxid3.equals(gtxid2));
    assertGlobalTxWasLoaded(stxid);

    try {
      gtxm.commit(null, stxid);
      fail("Should not be able to commit twice");
    } catch (TransactionCommittedError e) {
      // expected
    }
    assertGlobalTxWasLoaded(stxid);
    assertNextGlobalTxNotCalled();

    // APPLY A NEW TRANSACTION
    ServerTransactionID stxid2 = new ServerTransactionID(channel1, new TransactionID(2));
    assertTrue(gtxm.needsApply(stxid2));
    assertGlobalTxWasLoaded(stxid2);

    gtxm.commit(null, stxid2);

    assertFalse(gtxm.needsApply(stxid2));

    Collection finished = new ArrayList(2);
    finished.add(stxid);
    finished.add(stxid2);

    PersistenceTransaction txn = ptxp.newTransaction();
    gtxm.completeTransactions(txn,finished);
    txn.commit();
    

    // Check if the servertransactions are removed from the databases
    assertTrue(gtxm.needsApply(stxid));
    assertTrue(gtxm.needsApply(stxid2));
  }

  private void assertNextGlobalTXWasCalled(ServerTransactionID stxid) {
    assertEquals(stxid, transactionStore.nextTransactionIDContextQueue.poll(1));
    assertNextGlobalTxNotCalled();
  }

  private void assertNextGlobalTxNotCalled() {
    assertTrue(transactionStore.nextTransactionIDContextQueue.isEmpty());
  }

  private void assertGlobalTxWasNotLoaded() {
    assertTrue(transactionStore.loadContextQueue.isEmpty());
  }

  private void assertGlobalTxWasLoaded(ServerTransactionID stxid) {
    ServerTransactionID stxidAsLoadKey = (ServerTransactionID) transactionStore.loadContextQueue.poll(1);
    assertNotNull(stxidAsLoadKey);
    assertEquals(stxid, stxidAsLoadKey);
    assertGlobalTxWasNotLoaded();
  }

}
