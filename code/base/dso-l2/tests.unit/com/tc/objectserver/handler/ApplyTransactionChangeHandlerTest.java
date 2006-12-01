/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.impl.MockSink;
import com.tc.async.impl.MockStage;
import com.tc.exception.ImplementMe;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.context.BatchedTransactionProcessingContext;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.objectserver.lockmanager.api.TestLockManager;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionImpl;
import com.tc.objectserver.tx.TestServerTransaction;
import com.tc.objectserver.tx.TestServerTransactionManager;
import com.tc.objectserver.tx.TestTransactionBatchManager;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class ApplyTransactionChangeHandlerTest extends TestCase {

  private TestTransactionBatchManager   transactionBatchManager;
  private ApplyTransactionChangeHandler handler;
  private ObjectInstanceMonitor         instanceMonitor;
  private TestServerTransactionManager  transactionManager;
  private TestChannelManager            channelManager;
  private TestGlobalTransactionManager  gtxm;
  private TestLockManager               lockManager;
  private MockSink                      broadcastSink;

  public void setUp() throws Exception {
    transactionBatchManager = new TestTransactionBatchManager();
    instanceMonitor = new ObjectInstanceMonitorImpl();
    transactionManager = new TestServerTransactionManager();
    channelManager = new TestChannelManager();
    lockManager = new TestLockManager();
    gtxm = new TestGlobalTransactionManager();
    handler = new ApplyTransactionChangeHandler(instanceMonitor, transactionBatchManager, gtxm);

    MockStage stageBo = new MockStage("Bo");
    MockStage stageCo = new MockStage("Co");
    broadcastSink = stageBo.sink;
    TestServerConfigurationContext context = new TestServerConfigurationContext();
    context.transactionManager = transactionManager;
    context.channelManager = channelManager;
    context.addStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE, stageBo);
    context.addStage(ServerConfigurationContext.COMMIT_CHANGES_STAGE, stageCo);
    context.lockManager = lockManager;

    handler.initialize(context);
  }

  public void testGlobalTransactionIDInteraction() throws Exception {
    TxnBatchID batchID = new TxnBatchID(1);
    ServerTransactionID serverTransactionID = new ServerTransactionID(new ChannelID(1), new TransactionID(1));
    TestServerTransaction serverTransaction = new TestServerTransaction(serverTransactionID, batchID);

    Set appliedServerTransactionIDs = new HashSet();

    // Set the GlobalTransactionManager so that the transaction doesn't need to be applied.
    BatchedTransactionProcessingContext batchedTxnContext = getBatchedTxnContext(serverTransaction);

    appliedServerTransactionIDs.add(serverTransactionID);
    handler.handleEvent(batchedTxnContext);
    assertEquals(appliedServerTransactionIDs, batchedTxnContext.getAppliedServerTransactionIDs());
  }

  public void testLockManagerNotifyIsCalled() throws Exception {
    TxnBatchID batchID = new TxnBatchID(1);
    TransactionID txID = new TransactionID(1);
    LockID[] lockIDs = new LockID[] { new LockID("1") };
    ChannelID channelID = new ChannelID(1);
    List dnas = Collections.unmodifiableList(new LinkedList());
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;
    List notifies = new LinkedList();

    for (int i = 0; i < 10; i++) {
      notifies.add(new Notify(new LockID("" + i), new ThreadID(i), i % 2 == 0));
    }
    SequenceID sequenceID = new SequenceID(1);
    ServerTransaction tx = new ServerTransactionImpl(batchID, txID, sequenceID, lockIDs, channelID, dnas, serializer,
                                                     newRoots, txnType, notifies);
    // call handleEvent with the global transaction reporting that it doesn't need an apply...
    assertTrue(lockManager.notifyCalls.isEmpty());
    assertTrue(broadcastSink.queue.isEmpty());
    handler.handleEvent(getBatchedTxnContext(tx));
    // even if the transaction has already been applied, the notifies must be applied, since they aren't persistent.
    assertEquals(notifies.size(), lockManager.notifyCalls.size());
    lockManager.notifyCalls.clear();
    assertNotNull(broadcastSink.queue.remove(0));

    // call handleEvent with the global transaction reporting that it DOES need an apply...
    handler.handleEvent(getBatchedTxnContext(tx));

    assertEquals(notifies.size(), lockManager.notifyCalls.size());
    NotifiedWaiters notifiedWaiters = null;
    for (Iterator i = notifies.iterator(); i.hasNext();) {
      Notify notify = (Notify) i.next();
      Object[] args = (Object[]) lockManager.notifyCalls.remove(0);
      assertEquals(notify.getLockID(), args[0]);
      assertEquals(channelID, args[1]);
      assertEquals(notify.getThreadID(), args[2]);
      assertEquals(new Boolean(notify.getIsAll()), args[3]);
      if (notifiedWaiters == null) {
        notifiedWaiters = (NotifiedWaiters) args[4];
      }
      assertNotNull(notifiedWaiters);
      assertSame(notifiedWaiters, args[4]);
    }

    // next, check to see that the handler puts the newly pending waiters into the broadcast context.
    BroadcastChangeContext bctxt = (BroadcastChangeContext) broadcastSink.queue.remove(0);
    assertNotNull(bctxt);
    assertEquals(notifiedWaiters, bctxt.getNewlyPendingWaiters());
  }

  public void testBatchAccounting() throws Exception {

    TxnBatchID batchID = new TxnBatchID(1);
    TransactionID txID = new TransactionID(1);
    LockID[] lockIDs = new LockID[0];
    ChannelID channelID = new ChannelID(1);
    List dnas = Collections.unmodifiableList(new LinkedList());
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;
    SequenceID sequenceID = new SequenceID(1);
    ServerTransaction tx = new ServerTransactionImpl(batchID, txID, sequenceID, lockIDs, channelID, dnas, serializer,
                                                     newRoots, txnType, new LinkedList());

    assertTrue(transactionBatchManager.batchComponentCompleteCalls.isEmpty());
    assertFalse(transactionBatchManager.isBatchComponentComplete.get());
    assertTrue(channelManager.newBatchTransactionAcknowledgeMessageCalls.isEmpty());
    assertFalse(channelManager.shouldThrowNoSuchChannelException);
    handler.handleEvent(getBatchedTxnContext(tx));

    // make sure it called batchComponentComplete on TransactionBatchManager.
    assertBatchComponentCompleteCalled(batchID, txID, channelID);

    // make sure it didn't try to send a message
    assertTrue(channelManager.newBatchTransactionAcknowledgeMessageCalls.isEmpty());

    // now try it and make the tx batch manager report that the batch is complete.
    transactionBatchManager.isBatchComponentComplete.set(true);
    channelManager.btamsg = new TestBatchTransactionAcknowledgeMessage();
    assertNotNull(channelManager.btamsg);
    handler.handleEvent(getBatchedTxnContext(tx));
    assertBatchComponentCompleteCalled(batchID, txID, channelID);

    // make sure it sent a message
    assertEquals(channelID, channelManager.newBatchTransactionAcknowledgeMessageCalls.poll(1));
    assertEquals(batchID, channelManager.btamsg.initializeCalls.poll(1));
    assertNotNull(channelManager.btamsg.sendCalls.poll(1));

    // now cause a NoSuchChannelException to be thrown
    channelManager.shouldThrowNoSuchChannelException = true;
    channelManager.btamsg = null;
    assertTrue(transactionBatchManager.batchComponentCompleteCalls.isEmpty());
    assertTrue(transactionBatchManager.isBatchComponentComplete.get());
    assertTrue(channelManager.newBatchTransactionAcknowledgeMessageCalls.isEmpty());
    assertTrue(channelManager.shouldThrowNoSuchChannelException);

    handler.handleEvent(getBatchedTxnContext(tx));
    assertNotNull(channelManager.newBatchTransactionAcknowledgeMessageCalls.poll(1));
  }

  private BatchedTransactionProcessingContext getBatchedTxnContext(ServerTransaction txt) {
    BatchedTransactionProcessingContext btxtContext = new BatchedTransactionProcessingContext();
    btxtContext.addTransaction(txt);
    btxtContext.close(new HashSet());
    return btxtContext;
  }

  private void assertBatchComponentCompleteCalled(TxnBatchID batchID, TransactionID txID, ChannelID channelID) {
    Object[] args = (Object[]) transactionBatchManager.batchComponentCompleteCalls.poll(1);
    assertTrue(transactionBatchManager.batchComponentCompleteCalls.isEmpty());
    assertNotNull(args);
    assertEquals(channelID, args[0]);
    assertEquals(batchID, args[1]);
    assertEquals(txID, args[2]);
  }

  private static class TestBatchTransactionAcknowledgeMessage implements BatchTransactionAcknowledgeMessage {
    public final NoExceptionLinkedQueue initializeCalls = new NoExceptionLinkedQueue();
    public final NoExceptionLinkedQueue sendCalls       = new NoExceptionLinkedQueue();

    public void initialize(TxnBatchID id) {
      initializeCalls.put(id);
    }

    public TxnBatchID getBatchID() {
      throw new ImplementMe();
    }

    public void send() {
      sendCalls.put(new Object());
    }

  }

  private static class TestChannelManager implements DSOChannelManager {

    public void closeAll(Collection channelIDs) {
      throw new ImplementMe();
    }

    public MessageChannel getChannel(ChannelID id) {
      return new MessageChannel() {

        public TCSocketAddress getLocalAddress() {
          throw new ImplementMe();
        }

        public TCSocketAddress getRemoteAddress() {
          throw new ImplementMe();
        }

        public void addListener(ChannelEventListener listener) {
          throw new ImplementMe();
        }

        public ChannelID getChannelID() {
          throw new ImplementMe();
        }

        public boolean isOpen() {
          throw new ImplementMe();
        }

        public boolean isClosed() {
          throw new ImplementMe();
        }

        public TCMessage createMessage(TCMessageType type) {
          throw new ImplementMe();
        }

        public Object getAttachment(String key) {
          throw new ImplementMe();
        }

        public void addAttachment(String key, Object value, boolean replace) {
          throw new ImplementMe();
        }

        public Object removeAttachment(String key) {
          throw new ImplementMe();
        }

        public boolean isConnected() {
          throw new ImplementMe();
        }

        public void send(TCNetworkMessage message) {
          throw new ImplementMe();
        }

        public NetworkStackID open() {
          throw new ImplementMe();
        }

        public void close() {
          throw new ImplementMe();
        }

      };
    }

    public MessageChannel[] getChannels() {
      throw new ImplementMe();
    }

    public boolean isValidID(ChannelID channelID) {
      throw new ImplementMe();
    }

    public String getChannelAddress(ChannelID channelID) {
      throw new ImplementMe();
    }

    public NoExceptionLinkedQueue                 newBatchTransactionAcknowledgeMessageCalls = new NoExceptionLinkedQueue();
    public TestBatchTransactionAcknowledgeMessage btamsg;
    public boolean                                shouldThrowNoSuchChannelException          = false;

    public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(ChannelID channelID)
        throws NoSuchChannelException {
      this.newBatchTransactionAcknowledgeMessageCalls.put(channelID);
      if (shouldThrowNoSuchChannelException) { throw new NoSuchChannelException(); }
      return btamsg;
    }

    public ClientHandshakeAckMessage newClientHandshakeAckMessage(ChannelID channelID) {
      throw new ImplementMe();
    }

    public Collection getAllChannelIDs() {
      throw new ImplementMe();
    }
  }

}
