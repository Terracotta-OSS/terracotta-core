/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.EventContext;
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
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.net.NoSuchChannelException;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.objectserver.lockmanager.api.TestLockManager;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.tx.NullTransactionalObjectManager;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionImpl;
import com.tc.objectserver.tx.TestServerTransactionManager;
import com.tc.objectserver.tx.TestTransactionBatchManager;
import com.tc.test.TCTestCase;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BroadcastChangeHandlerTest extends TCTestCase {

  private TestTransactionBatchManager  transactionBatchManager;
  private BroadcastChangeHandler       handler;
  private TestServerTransactionManager transactionManager;
  private TestChannelManager           channelManager;
  private TestGlobalTransactionManager gtxm;
  private TestLockManager              lockManager;

  public void setUp() throws Exception {
    transactionBatchManager = new TestTransactionBatchManager();
    transactionManager = new TestServerTransactionManager();
    channelManager = new TestChannelManager(2);
    lockManager = new TestLockManager();
    gtxm = new TestGlobalTransactionManager();
    handler = new BroadcastChangeHandler(transactionBatchManager);

    MockStage stageMOR = new MockStage("MOR");
    MockStage stageRTO = new MockStage("RTO");
    TestServerConfigurationContext context = new TestServerConfigurationContext();
    context.transactionManager = transactionManager;
    context.channelManager = channelManager;
    context.txnObjectManager = new NullTransactionalObjectManager();
    context.clientStateManager = new TestClientStateManager();
    context.addStage(ServerConfigurationContext.MANAGED_OBJECT_REQUEST_STAGE, stageMOR);
    context.addStage(ServerConfigurationContext.RESPOND_TO_OBJECT_REQUEST_STAGE, stageRTO);
    context.lockManager = lockManager;

    handler.initialize(context);
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
                                                     newRoots, txnType, new LinkedList(), DmiDescriptor.EMPTY_ARRAY);

    assertTrue(transactionBatchManager.batchComponentCompleteCalls.isEmpty());
    assertFalse(transactionBatchManager.isBatchComponentComplete.get());
    assertTrue(channelManager.newBatchTransactionAcknowledgeMessageCalls.isEmpty());
    assertFalse(channelManager.shouldThrowNoSuchChannelException);
    handler.handleEvent(getBroadcastTxnContext(tx));

    // make sure it called batchComponentComplete on TransactionBatchManager.
    assertBatchComponentCompleteCalled(batchID, txID, channelID);

    // make sure it didn't try to send a message
    assertTrue(channelManager.newBatchTransactionAcknowledgeMessageCalls.isEmpty());

    // now try it and make the tx batch manager report that the batch is complete.
    transactionBatchManager.isBatchComponentComplete.set(true);
    channelManager.btamsg = new TestBatchTransactionAcknowledgeMessage();
    assertNotNull(channelManager.btamsg);
    handler.handleEvent(getBroadcastTxnContext(tx));
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

    handler.handleEvent(getBroadcastTxnContext(tx));
    assertNotNull(channelManager.newBatchTransactionAcknowledgeMessageCalls.poll(1));
  }

  private EventContext getBroadcastTxnContext(ServerTransaction tx) {
    ServerTransactionID stxID = tx.getServerTransactionID();
    GlobalTransactionID gid = gtxm.getGlobalTransactionID(stxID);
    return new BroadcastChangeContext(gid, tx, gtxm.getLowGlobalTransactionIDWatermark(), new NotifiedWaiters(),
                                      new BackReferences());
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

    Map allChannels = new HashMap();

    public TestChannelManager(int noOfChannels) {
      while (noOfChannels-- > 0) {
        ChannelID cid = new ChannelID(noOfChannels);
        allChannels.put(cid, new TestMessageChannel(cid));
      }
    }

    public void closeAll(Collection channelIDs) {
      throw new ImplementMe();
    }

    public MessageChannel getActiveChannel(ChannelID id) {
      return (MessageChannel) allChannels.get(id);
    }

    public MessageChannel[] getActiveChannels() {
      return (MessageChannel[]) allChannels.values().toArray(new MessageChannel[0]);
    }

    public boolean isActiveID(ChannelID channelID) {
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

    public Collection getAllActiveChannelIDs() {
      throw new ImplementMe();
    }

    public void addEventListener(DSOChannelManagerEventListener listener) {
      throw new ImplementMe();
    }

    public void makeChannelActive(ChannelID channelID, long startIDs, long endIDs, boolean persistent) {
      throw new ImplementMe();
    }

    public Collection getRawChannelIDs() {
      throw new ImplementMe();
    }

    public void makeChannelActiveNoAck(MessageChannel channel) {
      throw new ImplementMe();
    }
  }

  private static final class TestMessageChannel implements MessageChannel {

    private final ChannelID cid;

    public TestMessageChannel(ChannelID cid) {
      this.cid = cid;
    }

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
      return cid;
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

  }
}
