/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.tx;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tc.l2.ha.L2HACoordinator;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.l2.objectserver.ResentServerTransaction;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.test.TCTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResentTransactionSequencerTest extends TCTestCase {

  private ServerTransactionManager       transactionManager;
  private ServerGlobalTransactionManager gtxm;
  private ResentTransactionSequencer     sequencer;
  private TxnsInSystemCompletionListener callBack;
  private ReplicatedObjectManager        replicatedObjectManager;
  private long                           nextGID = 0;
  private TransactionBatchReader         batchReader;

  @Override
  protected void setUp() throws Exception {
    transactionManager = mock(ServerTransactionManager.class);
    gtxm = mock(ServerGlobalTransactionManager.class);
    callBack = mock(TxnsInSystemCompletionListener.class);
    sequencer = new ResentTransactionSequencer();
    replicatedObjectManager = mock(ReplicatedObjectManager.class);
    batchReader = mock(TransactionBatchReader.class);

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
    TransactionBatchContext context = transactionBatch(0, 1, 2, 3);
    sequencer.addResentServerTransactionIDs(context.getTransactionIDs());
    verify(transactionManager, never()).callBackOnTxnsInSystemCompletion(callBack);

    sequencer.transactionManagerStarted(clientIDs(0));

    verify(transactionManager, never()).callBackOnTxnsInSystemCompletion(callBack);

    sequencer.addTransactions(context);

    verify(transactionManager).callBackOnTxnsInSystemCompletion(callBack);
  }

  public void testClientDisconnect() throws Exception {
    sequencer.callBackOnResentTxnsInSystemCompletion(callBack);
    sequencer.goToActiveMode();
    
    TransactionBatchContext batch1 = transactionBatch(0, 1);
    TransactionBatchContext batch2 = transactionBatch(1, 1);
    TransactionBatchContext batch3 = transactionBatch(2, 1);
    
    sequencer.addResentServerTransactionIDs(batch1.getTransactionIDs());
    sequencer.addResentServerTransactionIDs(batch2.getTransactionIDs());
    sequencer.addResentServerTransactionIDs(batch3.getTransactionIDs());
    sequencer.transactionManagerStarted(clientIDs(0, 1, 2));

    sequencer.addTransactions(batch2);
    sequencer.clearAllTransactionsFor(batch2.getSourceNodeID());

    sequencer.addTransactions(batch3);
    sequencer.addTransactions(batch1);

    InOrder inOrder = inOrder(transactionManager);
    inOrder.verify(transactionManager).incomingTransactions(eq(batch1.getSourceNodeID()),
        argThat(hasServerTransactions(serverTransactionID(0, 1))));
    inOrder.verify(transactionManager).incomingTransactions(eq(batch2.getSourceNodeID()),
        argThat(hasServerTransactions(serverTransactionID(1, 1))));
    inOrder.verify(transactionManager).incomingTransactions(eq(batch3.getSourceNodeID()),
        argThat(hasServerTransactions(serverTransactionID(2, 1))));

    inOrder.verify(transactionManager).callBackOnTxnsInSystemCompletion(callBack);
  }

  public void testOrderedIncoming() throws Exception {
    TransactionBatchContext batch1 = transactionBatch(0, 1, 2);
    TransactionBatchContext batch2 = transactionBatch(1, 2, 3);

    sequencer.goToActiveMode();
    sequencer.addResentServerTransactionIDs(batch2.getTransactionIDs());
    sequencer.addResentServerTransactionIDs(batch1.getTransactionIDs());
    sequencer.transactionManagerStarted(clientIDs(0, 1));

    // Add a batch that isn't resent, should be processed last
    TransactionBatchContext batch3 = transactionBatch(0, 4, 5);
    TransactionBatchContext batch4 = transactionBatch(1, 4, 5);
    sequencer.addTransactions(batch3);
    sequencer.addTransactions(batch4);

    sequencer.addTransactions(batch2);
    sequencer.addTransactions(batch1);

    ArgumentCaptor<Map> c = ArgumentCaptor.<Map> forClass(Map.class);
    ArgumentCaptor<NodeID> n = ArgumentCaptor.<NodeID> forClass(NodeID.class);

    InOrder inOrder = inOrder(replicatedObjectManager, transactionManager);
    inOrder.verify(transactionManager).incomingTransactions(n.capture(), c.capture());
    inOrder.verify(replicatedObjectManager).relayTransactions(batch1);
    inOrder.verify(transactionManager).incomingTransactions(n.capture(), c.capture());
    inOrder.verify(replicatedObjectManager).relayTransactions(batch2);

    inOrder.verify(transactionManager).incomingTransactions(n.capture(), c.capture());
    inOrder.verify(replicatedObjectManager).relayTransactions(batch3);
    inOrder.verify(transactionManager).incomingTransactions(n.capture(), c.capture());
    inOrder.verify(replicatedObjectManager).relayTransactions(batch4);

    verifyBatches(true, c, batch1, batch2, batch3, batch4);
  }

  public void testResentTxnWrapping() throws Exception {
    TransactionBatchContext batch1 = transactionBatch(0, 1, 2);
    TransactionBatchContext batch2 = transactionBatch(1, 2, 3);

    sequencer.goToActiveMode();
    sequencer.addResentServerTransactionIDs(batch2.getTransactionIDs());
    sequencer.addResentServerTransactionIDs(batch1.getTransactionIDs());
    sequencer.transactionManagerStarted(clientIDs(0, 1));

    sequencer.addTransactions(batch2);
    sequencer.addTransactions(batch1);

    // Add non-resent batches (pass through mode)
    TransactionBatchContext batch3 = transactionBatch(0, 4, 5);
    TransactionBatchContext batch4 = transactionBatch(1, 4, 5);
    sequencer.addTransactions(batch3);
    sequencer.addTransactions(batch4);

    ArgumentCaptor<Map> c = ArgumentCaptor.<Map> forClass(Map.class);
    ArgumentCaptor<NodeID> n = ArgumentCaptor.<NodeID> forClass(NodeID.class);

    InOrder inOrder = inOrder(replicatedObjectManager, transactionManager);
    inOrder.verify(transactionManager).incomingTransactions(n.capture(), c.capture());
    inOrder.verify(replicatedObjectManager).relayTransactions(batch1);
    inOrder.verify(transactionManager).incomingTransactions(n.capture(), c.capture());
    inOrder.verify(replicatedObjectManager).relayTransactions(batch2);

    verifyBatches(true, c, batch1, batch2);

    // reset arg captor
    c = ArgumentCaptor.<Map> forClass(Map.class);

    inOrder.verify(transactionManager).incomingTransactions(n.capture(), c.capture());
    inOrder.verify(replicatedObjectManager).relayTransactions(batch3);
    inOrder.verify(transactionManager).incomingTransactions(n.capture(), c.capture());
    inOrder.verify(replicatedObjectManager).relayTransactions(batch4);

    verifyBatches(false, c, batch3, batch4);
  }

  public void testDiscontinuousGIDInOrder() throws Exception {
    // Cut batch1 into 2 sub-batches based on the GID split, batch2 is just 1 batch sitting between batch1 part1 and batch1 part2
    when(gtxm.getGlobalTransactionID(serverTransactionID(0, 1))).thenReturn(new GlobalTransactionID(0));
    when(gtxm.getGlobalTransactionID(serverTransactionID(1, 1))).thenReturn(new GlobalTransactionID(1));
    when(gtxm.getGlobalTransactionID(serverTransactionID(0, 2))).thenReturn(new GlobalTransactionID(2));
    
    TransactionBatchContext batch1 = transactionBatch(0, 1, 2);
    TransactionBatchContext batch2 = transactionBatch(1, 1);

    sequencer.goToActiveMode();
    sequencer.addResentServerTransactionIDs(batch1.getTransactionIDs());
    sequencer.addResentServerTransactionIDs(batch2.getTransactionIDs());
    sequencer.transactionManagerStarted(clientIDs(0, 1));

    sequencer.addTransactions(batch1);
    sequencer.addTransactions(batch2);

    InOrder inOrder = inOrder(transactionManager, replicatedObjectManager);
    // Make sure transactions are passed through and relayed in 3 chunks (1 per GID range) in the correct order
    inOrder.verify(transactionManager).incomingTransactions(eq(batch1.getSourceNodeID()), argThat(hasServerTransactions(serverTransactionID(0, 1))));
    inOrder.verify(replicatedObjectManager).relayTransactions(argThat(hasClientIDAndTxns(0, serverTransactionID(0, 1))));
    inOrder.verify(transactionManager).incomingTransactions(eq(batch2.getSourceNodeID()), argThat(hasServerTransactions(serverTransactionID(1, 1))));
    inOrder.verify(replicatedObjectManager).relayTransactions(argThat(hasClientIDAndTxns(1, serverTransactionID(1, 1))));
    inOrder.verify(transactionManager).incomingTransactions(eq(batch1.getSourceNodeID()), argThat(hasServerTransactions(serverTransactionID(0, 2))));
    inOrder.verify(replicatedObjectManager).relayTransactions(argThat(hasClientIDAndTxns(0, serverTransactionID(0, 2))));
  }

  private Matcher<Map<ServerTransactionID, ServerTransaction>> hasServerTransactions(final ServerTransactionID... txnIDs) {
    return new BaseMatcher<Map<ServerTransactionID, ServerTransaction>>() {
      @Override
      public boolean matches(final Object item) {
        if (item instanceof Map) {
          Map<?, ?> map = (Map<?, ?>) item;
          return map.keySet().containsAll(Arrays.asList(txnIDs)) && map.size() == txnIDs.length;
        }
        return false;
      }

      @Override
      public void describeTo(final Description description) {
      }
    };
  }

  private Matcher<TransactionBatchContext> hasClientIDAndTxns(final long clientID, final ServerTransactionID ... txnIDs) {
    return new BaseMatcher<TransactionBatchContext>() {
      @Override
      public boolean matches(final Object item) {
        if (item instanceof TransactionBatchContext) {
          TransactionBatchContext transactionBatchContext = (TransactionBatchContext)item;
          return transactionBatchContext.getSourceNodeID().equals(new ClientID(clientID)) &&
                 transactionBatchContext.getNumTxns() == txnIDs.length &&
                 transactionBatchContext.getTransactionIDs().containsAll(Arrays.asList(txnIDs));
        } else {
          return false;
        }
      }

      @Override
      public void describeTo(final Description description) {
      }
    };
  }

  private void verifyBatches(boolean isResent, ArgumentCaptor<Map> captor, TransactionBatchContext... batch) {
    List<Map> actual = captor.getAllValues();
    Iterator<Map> itr = actual.iterator();
    for (TransactionBatchContext tb : batch) {
      Map am = itr.next();
      for (ServerTransactionID tid : tb.getTransactionIDs()) {
        ServerTransaction actualTxn = (ServerTransaction) am.get(tid);
        assertNotNull(tid.toString(), actualTxn);
        assertEquals(tid.toString(), isResent, actualTxn instanceof ResentServerTransaction);
      }
    }
  }

  private static Set<ClientID> clientIDs(long... clientIds) {
    Set<ClientID> set = new HashSet<ClientID>();
    for (long clientId : clientIds) {
      set.add(new ClientID(clientId));
    }
    return set;
  }

  private List<ServerTransactionID> transactionIDs(long clientId, long ... transactions) {
    List<ServerTransactionID> stxIDs = Lists.newArrayList();
    for (long transactionId : transactions) {
      ServerTransactionID stxID = serverTransactionID(clientId, transactionId);
      stxIDs.add(stxID);
    }
    return stxIDs;
  }

  private GlobalTransactionID getOrCreateGID(ServerTransactionID serverTransactionID) {
    GlobalTransactionID globalTransactionID = gtxm.getGlobalTransactionID(serverTransactionID);
    if (globalTransactionID == null) {
      globalTransactionID = new GlobalTransactionID(nextGID++);
      when(gtxm.getGlobalTransactionID(serverTransactionID)).thenReturn(globalTransactionID);
    }
    return globalTransactionID;
  }

  private TransactionBatchContext transactionBatch(long clientId, long... transactionIds) {
    TransactionBatchContext context = mock(TransactionBatchContext.class);
    List<ServerTransaction> transactions = new ArrayList<ServerTransaction>();
    List<ServerTransactionID> ids = transactionIDs(clientId, transactionIds);
    for (ServerTransactionID stxId : ids) {
      ServerTransaction transaction = mock(ServerTransaction.class);
      GlobalTransactionID globalTransactionID = getOrCreateGID(stxId);
      when(transaction.getGlobalTransactionID()).thenReturn(globalTransactionID);
      when(transaction.getServerTransactionID()).thenReturn(stxId);
      transactions.add(transaction);
    }
    when(context.getTransactionBatchReader()).thenReturn(batchReader);
    when(context.getNumTxns()).thenReturn(transactionIds.length);
    when(context.getTransactions()).thenReturn(transactions);
    when(context.getTransactionIDs()).thenReturn(Sets.newHashSet(ids));
    when(context.getSourceNodeID()).thenReturn(new ClientID(clientId));
    return context;
  }

  private ServerTransactionID serverTransactionID(long clientID, long transactionID) {
    return new ServerTransactionID(new ClientID(clientID), new TransactionID(transactionID));
  }
}
