/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import org.mockito.InOrder;

import com.tc.l2.ha.L2HACoordinator;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.test.TCTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResentTransactionSequencerTest extends TCTestCase {

  private ServerTransactionManager       transactionManager;
  private ServerGlobalTransactionManager gtxm;
  private ResentTransactionSequencer     sequencer;
  private TxnsInSystemCompletionListener callBack;
  private ReplicatedObjectManager        replicatedObjectManager;
  private long                           nextGID = 0;

  @Override
  protected void setUp() throws Exception {
    transactionManager = mock(ServerTransactionManager.class);
    gtxm = mock(ServerGlobalTransactionManager.class);
    callBack = mock(TxnsInSystemCompletionListener.class);
    sequencer = new ResentTransactionSequencer();
    replicatedObjectManager = mock(ReplicatedObjectManager.class);

    L2HACoordinator l2HACoordinator = mock(L2HACoordinator.class);
    when(l2HACoordinator.getReplicatedObjectManager()).thenReturn(replicatedObjectManager);

    ServerConfigurationContext context = mock(ServerConfigurationContext.class);
    when(context.getServerGlobalTransactionManager()).thenReturn(gtxm);
    when(context.getTransactionManager()).thenReturn(transactionManager);
    when(context.getL2Coordinator()).thenReturn(l2HACoordinator);

    sequencer.initializeContext(context);
  }

  public void testAddResentAfterStarted() throws Exception {
    sequencer.goToActiveMode();
    sequencer.transactionManagerStarted(Collections.EMPTY_SET);
    try {
      sequencer.addResentServerTransactionIDs(transactionIDs(0, 1, 2));
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }
  }

  public void testCallbackOnResentComplete() throws Exception {
    sequencer.callBackOnResentTxnsInSystemCompletion(callBack);
    sequencer.goToActiveMode();
    sequencer.addResentServerTransactionIDs(transactionIDs(0, 1, 2, 3));
    verify(transactionManager, never()).callBackOnTxnsInSystemCompletion(callBack);

    sequencer.transactionManagerStarted(Collections.singleton(new ClientID(0)));

    verify(transactionManager, never()).callBackOnTxnsInSystemCompletion(callBack);

    sequencer.addTransactions(transactionBatch(0, 1, 2, 3));

    verify(transactionManager).callBackOnTxnsInSystemCompletion(callBack);
  }

  public void testClientDisconnect() throws Exception {
    sequencer.callBackOnResentTxnsInSystemCompletion(callBack);
    sequencer.goToActiveMode();
    sequencer.addResentServerTransactionIDs(transactionIDs(0, 1, 2));
    sequencer.addResentServerTransactionIDs(transactionIDs(1, 1, 2));
    sequencer.transactionManagerStarted(clientIDs(0, 1));
    sequencer.addTransactions(transactionBatch(0, 1, 2));
    verify(transactionManager, never()).callBackOnTxnsInSystemCompletion(callBack);

    sequencer.clearAllTransactionsFor(new ClientID(1));
    verify(transactionManager).callBackOnTxnsInSystemCompletion(callBack);
  }

  public void testOrderedIncoming() throws Exception {
    TransactionBatchContext batch1 = transactionBatch(0, 1, 2);
    TransactionBatchContext batch2 = transactionBatch(1, 2, 3);

    sequencer.goToActiveMode();
    sequencer.addResentServerTransactionIDs(batch2.getTransactionIDs());
    sequencer.addResentServerTransactionIDs(batch1.getTransactionIDs());
    sequencer.transactionManagerStarted(clientIDs(0, 1));

    // Add a batch that isn't resent, should be processed last
    TransactionBatchContext batch3 = transactionBatch(0, 3, 4);
    TransactionBatchContext batch4 = transactionBatch(1, 3, 4);
    sequencer.addTransactions(batch3);
    sequencer.addTransactions(batch4);

    sequencer.addTransactions(batch2);
    sequencer.addTransactions(batch1);

    InOrder inOrder = inOrder(replicatedObjectManager, transactionManager);
    inOrder.verify(transactionManager).incomingTransactions(batch1.getSourceNodeID(), transactionMap(batch1));
    inOrder.verify(replicatedObjectManager).relayTransactions(batch1);
    inOrder.verify(transactionManager).incomingTransactions(batch2.getSourceNodeID(), transactionMap(batch2));
    inOrder.verify(replicatedObjectManager).relayTransactions(batch2);
    inOrder.verify(transactionManager).incomingTransactions(batch3.getSourceNodeID(), transactionMap(batch3));
    inOrder.verify(replicatedObjectManager).relayTransactions(batch3);
    inOrder.verify(transactionManager).incomingTransactions(batch4.getSourceNodeID(), transactionMap(batch4));
    inOrder.verify(replicatedObjectManager).relayTransactions(batch4);
  }

  private static Set<ClientID> clientIDs(long... clientIds) {
    Set<ClientID> set = new HashSet<ClientID>();
    for (long clientId : clientIds) {
      set.add(new ClientID(clientId));
    }
    return set;
  }

  private Set<ServerTransactionID> transactionIDs(long clientId, long ... transactions) {
    Set<ServerTransactionID> stxIDs = new HashSet<ServerTransactionID>();
    for (long transactionId : transactions) {
      ServerTransactionID stxID = new ServerTransactionID(new ClientID(clientId), new TransactionID(transactionId));
      stxIDs.add(stxID);
      when(gtxm.getGlobalTransactionID(stxID)).thenReturn(new GlobalTransactionID(nextGID++));
    }
    return stxIDs;
  }

  private TransactionBatchContext transactionBatch(long clientId, long... transactionIds) {
    TransactionBatchContext context = mock(TransactionBatchContext.class);
    List<ServerTransaction> transactions = new ArrayList<ServerTransaction>();
    Set<ServerTransactionID> ids = transactionIDs(clientId, transactionIds);
    for (ServerTransactionID stxId : ids) {
      ServerTransaction transaction = mock(ServerTransaction.class);
      when(transaction.getServerTransactionID()).thenReturn(stxId);
      transactions.add(transaction);
    }
    when(context.getTransactions()).thenReturn(transactions);
    when(context.getTransactionIDs()).thenReturn(ids);
    when(context.getSourceNodeID()).thenReturn(new ClientID(clientId));
    return context;
  }

  private Map<ServerTransactionID, ServerTransaction> transactionMap(TransactionBatchContext transactionBatchContext) {
    Map<ServerTransactionID, ServerTransaction> map = new LinkedHashMap<ServerTransactionID, ServerTransaction>();
    for (ServerTransaction txn : transactionBatchContext.getTransactions()) {
      map.put(txn.getServerTransactionID(), txn);
    }
    return map;
  }
}
