/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.terracotta.test.categories.CheckShorts;

import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.tx.RemoteTransactionManagerImpl.BatchManager;
import com.tc.util.concurrent.Timer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Category(CheckShorts.class)
public class RemoteTransactionManagerImplTest {
  @Mock
  private ClientTransactionBatch      batch;
  private RemoteTransactionManager    manager;
  @Mock
  private TransactionBuffer           txnBuffer;
  @Mock
  private BatchManager                batchManager;
  private final TCLogger              logger             = TCLogging.getLogger(RemoteTransactionManagerImplTest.class);
  private final long                  ackOnExitTimeoutMs = 1 * 1000;
  @Mock
  private TransactionCompleteListener txnCompleteListener;
  @Mock
  private LockAccounting              lockAccounting;
  @Mock
  private Timer                       flusherTimer;
  @Mock
  private SessionManager              sessionManager;
  @Mock
  private TxnBatchID                  txnBatchID;
  @Mock
  private TransactionBatchAccounting  batchAccounting;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    Mockito.doNothing().when(lockAccounting).shutdown();
    Mockito.doNothing().when(flusherTimer).cancel();
    when(sessionManager.isCurrentSession((NodeID) any(), (SessionID) any())).thenReturn(true);
//    when(batchManager.isBlocked()).thenReturn(true);
    when(batchManager.sendNextBatch(false)).thenReturn(null);
    when(batchManager.sendNextBatch(true)).thenReturn(null);
    when(batchManager.isEmpty()).thenReturn(false);
    when(batchManager.size()).thenReturn(Integer.MAX_VALUE);
    when(txnBatchID.isNull()).thenReturn(false);
    when(batchAccounting.acknowledge((TxnBatchID)any(),(Collection)any())).thenReturn(true);
    when(batchManager.removeBatch((TxnBatchID) any())).thenReturn(null);

    when(batchAccounting.getBatchByTransactionID((TransactionID) any())).thenReturn(txnBatchID);

    List<TransactionCompleteListener> listenerList = new ArrayList<TransactionCompleteListener>();
    listenerList.add(txnCompleteListener);
    doThrow(new TCNotRunningException()).when(txnCompleteListener).transactionComplete(null);
    when(batchManager.getBatch((TxnBatchID) any())).thenReturn(batch);
    when(txnBuffer.getFoldedTransactionID()).thenReturn(null);
    when(txnBuffer.getTransactionCompleteListeners()).thenReturn(listenerList);
    when(batch.removeTransaction((TransactionID) any())).thenReturn(txnBuffer);

    manager = new RemoteTransactionManagerImpl(batchManager, batchAccounting, lockAccounting, logger,
                                               ackOnExitTimeoutMs, null, sessionManager, null, null, null, false, null,
                                               flusherTimer, null);
  }

  // dev9061
  @Test
  public void test_Stop_Waits_For_Timeout_When_Immediate_Shutdown_Not_Requested() {
    long timeTaken = stopManagerAndReturnTimeTaken();
    Assert.assertTrue(timeTaken > ackOnExitTimeoutMs);
    System.out.println("Test Finished, Time taken : " + (timeTaken) + " ms");
  }

  // dev9061
  @Test
  public void test_Stop_Method_Doesnot_Wait_For_Timeout_When_NodeError() {
    this.whenNodeError().assertStopExitsImmediately();
  }

  @Test
  public void test_receivedAcknowledgement_doesnot_ignore_TCNotRunningException_when_RemoteTxnMgr_not_shutdown() {

    try {
      manager.receivedAcknowledgement(null, null, null);
    } catch (TCNotRunningException e) {
      // expected
      System.out.println("Got expected exception" + e);
    }
  }

  @Test
  public void test_receivedAcknowledgement_ignores_TCNotRunningException_when_RemoteTxnMgr_shutdown() {

    manager.shutdown(false);
    manager.receivedAcknowledgement(null, null, null);
  }

  private long stopManagerAndReturnTimeTaken() {
    long startTime = System.currentTimeMillis();
    manager.stop();
    long timeTaken = System.currentTimeMillis() - startTime;
    return timeTaken;
  }

  private void assertStopExitsImmediately() {
    long timeTaken = stopManagerAndReturnTimeTaken();
    Assert.assertTrue(timeTaken < ackOnExitTimeoutMs);
    System.out.println("Test Finished, Time taken : " + (timeTaken) + " ms");
  }

  private RemoteTransactionManagerImplTest whenNodeError() {
    ClusterEventListener listener = new ClusterEventListener(manager);
    listener.nodeError(null);
    return this;
  }
}
