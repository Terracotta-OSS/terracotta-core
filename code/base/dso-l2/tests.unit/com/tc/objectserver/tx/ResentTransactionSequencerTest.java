/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;

public class ResentTransactionSequencerTest extends TestCase {

  private ServerTransactionManager       transactionManager;
  private ServerGlobalTransactionManager gtxm;
  private TransactionalObjectManager     txnObjectManager;
  private ResentTransactionSequencer     sequencer;
  private TxnsInSystemCompletionListener   callBack;

  @Override
  protected void setUp() throws Exception {
    this.transactionManager = mock(ServerTransactionManager.class);
    this.gtxm = mock(ServerGlobalTransactionManager.class);
    this.txnObjectManager = mock(TransactionalObjectManager.class);
    this.callBack = mock(TxnsInSystemCompletionListener.class);
    this.sequencer = new ResentTransactionSequencer(this.transactionManager, this.gtxm, this.txnObjectManager);
  }

  // DEV-4049
  public void test() {
    this.sequencer.callBackOnResentTxnsInSystemCompletion(this.callBack);
    this.sequencer.goToActiveMode();
    this.sequencer.clearAllTransactionsFor(new ClientID(3));
    verify(this.transactionManager, never()).callBackOnTxnsInSystemCompletion(this.callBack);

    Collection txnsIDs = createTxnsIDsMapping(new HashSet(), new ClientID(0), new ArrayList());
    Collection client1Txns = new ArrayList();
    createTxnsIDsMapping(txnsIDs, new ClientID(1), client1Txns);
    this.sequencer.addResentServerTransactionIDs(txnsIDs);

    this.sequencer.clearAllTransactionsFor(new ClientID(0));
    verify(this.transactionManager, never()).callBackOnTxnsInSystemCompletion(this.callBack);

    HashSet cids = new HashSet();
    cids.add(new ClientID(1));
    this.sequencer.transactionManagerStarted(cids);
    verify(this.transactionManager, never()).callBackOnTxnsInSystemCompletion(this.callBack);

    this.sequencer.addTransactions(client1Txns);
    verify(this.txnObjectManager, times(1)).addTransactions(client1Txns);
    verify(this.transactionManager, times(1)).callBackOnTxnsInSystemCompletion(this.callBack);

  }

  private Collection createTxnsIDsMapping(Collection txnIDs, ClientID clientID, Collection txns) {
    for (int i = 0; i < 5; i++) {
      ServerTransactionID sid = new ServerTransactionID(clientID, new TransactionID(i));
      txnIDs.add(sid);
      when(this.gtxm.getGlobalTransactionID(sid)).thenReturn(new GlobalTransactionID(i + clientID.toLong() * 1000));
      ServerTransaction txn = mock(ServerTransaction.class);
      when(txn.getServerTransactionID()).thenReturn(sid);
      txns.add(txn);
    }
    return txnIDs;
  }
}
