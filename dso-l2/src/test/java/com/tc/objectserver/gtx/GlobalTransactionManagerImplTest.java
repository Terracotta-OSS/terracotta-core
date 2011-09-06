/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.handler.GlobalTransactionIDBatchRequestHandler;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
import com.tc.objectserver.persistence.impl.TestPersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.TestTransactionStore;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.SequenceValidator;
import com.tc.util.sequence.SimpleSequence;

import junit.framework.TestCase;

public class GlobalTransactionManagerImplTest extends TestCase {

  private TestTransactionStore               transactionStore;
  private TestPersistenceTransactionProvider ptxp;
  private ServerGlobalTransactionManager     gtxm;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    transactionStore = new TestTransactionStore();
    ptxp = new TestPersistenceTransactionProvider();
    GlobalTransactionIDSequenceProvider gsp = new GlobalTransactionIDBatchRequestHandler(new TestMutableSequence());
    gtxm = new ServerGlobalTransactionManagerImpl(new SequenceValidator(0), transactionStore, ptxp, gsp,
                                                  new SimpleSequence());
  }

  public void testStartAndCommitApply() throws Exception {
    ClientID cid = new ClientID(1);
    ServerTransactionID stxID1 = new ServerTransactionID(cid, new TransactionID(1));
    ServerTransactionID stxID2 = new ServerTransactionID(cid, new TransactionID(2));

    GlobalTransactionID gtxID1 = gtxm.getOrCreateGlobalTransactionID(stxID1);
    GlobalTransactionID gtxID2 = gtxm.getOrCreateGlobalTransactionID(stxID2);
    assertNotSame(gtxID1, gtxID2);

    assertTrue(gtxm.initiateApply(stxID1));
    assertTrue(gtxm.initiateApply(stxID2));

    // the apply has been initiated so
    assertFalse(gtxm.initiateApply(stxID1));
    assertFalse(gtxm.initiateApply(stxID2));

    // now commit them
    gtxm.commit(null, stxID1);
    gtxm.commit(null, stxID2);

    assertFalse(gtxm.initiateApply(stxID1));
    assertFalse(gtxm.initiateApply(stxID2));

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
    ServerTransactionID stxid = new ServerTransactionID(new ClientID(channel1.toLong()), tx1);

    GlobalTransactionID gid1 = gtxm.getOrCreateGlobalTransactionID(stxid);
    assertNextGlobalTXWasCalled(stxid);

    assertTrue(gtxm.initiateApply(stxid));
    assertGlobalTxWasLoaded(stxid);

    assertNextGlobalTxNotCalled();

    // RESTART
    transactionStore.restart();
    GlobalTransactionID gid2 = gtxm.getOrCreateGlobalTransactionID(stxid);
    assertFalse(gid1.equals(gid2));
    assertNextGlobalTXWasCalled(stxid);

    // the transaction is still not applied
    assertTrue(gtxm.initiateApply(stxid));
    assertGlobalTxWasLoaded(stxid);

    // no longer needs to be applied
    assertFalse(gtxm.initiateApply(stxid));
    assertGlobalTxWasLoaded(stxid);

    GlobalTransactionID gid3 = gtxm.getOrCreateGlobalTransactionID(stxid);
    assertTrue(gid2.equals(gid3));

    gtxm.commit(null, stxid);
    assertGlobalTxWasLoaded(stxid);

    assertNotNull(transactionStore.commitContextQueue.poll(1));

    // make sure no calls to store were made
    assertTrue(transactionStore.commitContextQueue.isEmpty());

    // RESTART
    transactionStore.restart();

    // make sure that it isn't in progress
    assertFalse(gtxm.initiateApply(stxid));
    assertGlobalTxWasLoaded(stxid);
    assertNextGlobalTxNotCalled();

    try {
      gtxm.commit(null, stxid);
      fail("Should not be able to commit twice");
    } catch (TransactionCommittedError e) {
      // expected
    }
    assertGlobalTxWasLoaded(stxid);
    assertNextGlobalTxNotCalled();

    // APPLY A NEW TRANSACTION
    ServerTransactionID stxid2 = new ServerTransactionID(new ClientID(channel1.toLong()), new TransactionID(2));
    GlobalTransactionID gid4 = gtxm.getOrCreateGlobalTransactionID(stxid2);
    assertNextGlobalTXWasCalled(stxid2);
    assertNotSame(gid3, gid4);
    assertTrue(gtxm.initiateApply(stxid2));
    assertGlobalTxWasLoaded(stxid2);

    // apply does create
    gtxm.getOrCreateGlobalTransactionID(stxid2);
    gtxm.commit(null, stxid2);

    assertFalse(gtxm.initiateApply(stxid2));

    ServerTransactionID stxid3 = new ServerTransactionID(stxid2.getSourceID(), stxid2.getClientTransactionID().next());
    PersistenceTransaction txn = ptxp.newTransaction();
    gtxm.clearCommitedTransactionsBelowLowWaterMark(stxid3);
    txn.commit();
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
