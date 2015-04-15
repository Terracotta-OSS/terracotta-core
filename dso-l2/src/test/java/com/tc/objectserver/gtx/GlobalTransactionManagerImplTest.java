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
package com.tc.objectserver.gtx;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.Assert;

import com.tc.async.api.EventContext;
import com.tc.async.impl.MockSink;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.context.LowWaterMarkCallbackContext;
import com.tc.objectserver.event.ServerEventBuffer;
import com.tc.objectserver.handler.GlobalTransactionIDBatchRequestHandler;
import com.tc.objectserver.persistence.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
import com.tc.objectserver.persistence.impl.TestTransactionStore;
import com.tc.util.SequenceValidator;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase;

public class GlobalTransactionManagerImplTest extends TestCase {

  private TestTransactionStore               transactionStore;
  private ServerGlobalTransactionManager     gtxm;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Sequence sequence = new SimpleSequence();
    transactionStore = new TestTransactionStore(sequence);
    GlobalTransactionIDSequenceProvider gsp = new GlobalTransactionIDBatchRequestHandler(new TestMutableSequence());
    PersistenceTransactionProvider transactionProvider = mock(PersistenceTransactionProvider.class);
    Transaction transaction = mock(Transaction.class);
    doReturn(transaction).when(transactionProvider).newTransaction();
    ServerEventBuffer serverEventBuffer = mock(ServerEventBuffer.class);
    gtxm = new ServerGlobalTransactionManagerImpl(new SequenceValidator(0), transactionStore, gsp, sequence,
                                                  new LWMCallbackMockSink(), transactionProvider, serverEventBuffer);
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
    gtxm.commit(stxID1);
    gtxm.commit(stxID2);

    assertFalse(gtxm.initiateApply(stxID1));
    assertFalse(gtxm.initiateApply(stxID2));

    // now try to commit again
    try {
      gtxm.commit(stxID1);
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

    gtxm.commit(stxid);
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
      gtxm.commit(stxid);
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
    gtxm.commit(stxid2);

    assertFalse(gtxm.initiateApply(stxid2));

    ServerTransactionID stxid3 = new ServerTransactionID(stxid2.getSourceID(), stxid2.getClientTransactionID().next());
    gtxm.clearCommitedTransactionsBelowLowWaterMark(stxid3);
  }

  public void testLWMCallbacks() throws Exception {
    // Test the empty GlobalTransactionID system case
    FutureTask<Void> callback = noopFuture();
    gtxm.registerCallbackOnLowWaterMarkReached(callback);
    callback.get(10, TimeUnit.SECONDS);

    // Test to see if callbacks get executed right away even when there is a non-null GID
    gtxm.getOrCreateGlobalTransactionID(newServerTransactionID(1, 1));
    callback = noopFuture();
    gtxm.registerCallbackOnLowWaterMarkReached(callback);
    try {
      callback.get(2, TimeUnit.SECONDS);
      Assert.fail("Expecting callback to stay queued");
    } catch (TimeoutException e) {
      // expected;
    }
    gtxm.clearCommitedTransactionsBelowLowWaterMark(newServerTransactionID(1, 2));
    callback.get(10, TimeUnit.SECONDS);

    // Test clearing insufficient GID's to execute the callback.
    callback = noopFuture();
    gtxm.getOrCreateGlobalTransactionID(newServerTransactionID(1, 3));
    gtxm.getOrCreateGlobalTransactionID(newServerTransactionID(1, 4));
    gtxm.registerCallbackOnLowWaterMarkReached(callback);
    gtxm.clearCommitedTransactionsBelowLowWaterMark(newServerTransactionID(1, 4));
    try {
      callback.get(2, TimeUnit.SECONDS);
      Assert.fail("Expecting callback to stay queued");
    } catch (TimeoutException e) {
      // expected;
    }
    gtxm.clearCommitedTransactionsBelowLowWaterMark(newServerTransactionID(1, 5));
    callback.get(10, TimeUnit.SECONDS);
  }

  private static ServerTransactionID newServerTransactionID(long clientId, long txnId) {
    return new ServerTransactionID(new ClientID(clientId), new TransactionID(txnId));
  }

  private static FutureTask<Void> noopFuture() {
    return new FutureTask<Void>(new Callable<Void>() {
      @Override
      public Void call() {
        return null;
      }
    });
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

  private class LWMCallbackMockSink extends MockSink {
    @Override
    public void add(EventContext context) {
      if (context instanceof LowWaterMarkCallbackContext) {
        ((LowWaterMarkCallbackContext) context).run();
      } else {
        super.add(context);
      }
    }
  }
}
