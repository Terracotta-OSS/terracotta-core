/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.net.groups;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.async.impl.StageManagerImpl;
import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.config.NodesStore;
import com.tc.config.NodesStoreImpl;
import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.l2.msg.RelayedCommitTransactionMessageFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.msg.TestTransactionBatch;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.persistence.inmemory.InMemoryPersistor;
import com.tc.objectserver.tx.TestCommitTransactionMessage;
import com.tc.objectserver.tx.TestCommitTransactionMessageFactory;
import com.tc.objectserver.tx.TestServerTransaction;
import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * This test is to verify fix for DEV-2149 reallocating big memory. After test, grep "large" to see if big memory block
 * allocated by both listen thread and hydrate thread.
 */
public class TCGroupSendLargeRelayedCommitTransactionMessageTest extends TCTestCase {
  private final static String LOCALHOST   = "localhost";
  private static final int    millionOids = 1024 * 1024;

  public TCGroupSendLargeRelayedCommitTransactionMessageTest() {
    //
  }

  public void baseTestSendingReceivingMessagesStatic(int batchSize) throws Exception {
    System.out.println("Test batch data size " + batchSize);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), new InMemoryPersistor());
    PortChooser pc = new PortChooser();
    final int p1 = pc.chooseRandom2Port();
    final int p2 = pc.chooseRandom2Port();
    final Node[] allNodes = new Node[] { new Node(LOCALHOST, p1, p1 + 1), new Node(LOCALHOST, p2, p2 + 1) };

    StageManager stageManager1 = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(null)), new QueueFactory());
    TCGroupManagerImpl gm1 = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, p1, p1 + 1, stageManager1);
    ConfigurationContext context1 = new ConfigurationContextImpl(stageManager1);
    stageManager1.startAll(context1, Collections.EMPTY_LIST);
    gm1.setDiscover(new TCGroupMemberDiscoveryStatic(gm1));
    MyListener l1 = new MyListener();
    gm1.registerForMessages(RelayedCommitTransactionMessage.class, l1);

    StageManager stageManager2 = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(null)), new QueueFactory());
    TCGroupManagerImpl gm2 = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, p2, p2 + 1, stageManager2);
    ConfigurationContext context2 = new ConfigurationContextImpl(stageManager2);
    stageManager2.startAll(context2, Collections.EMPTY_LIST);
    gm2.setDiscover(new TCGroupMemberDiscoveryStatic(gm2));
    MyListener l2 = new MyListener();
    gm2.registerForMessages(RelayedCommitTransactionMessage.class, l2);

    Set<Node> nodeSet = new HashSet<Node>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    NodeID n1 = gm1.join(allNodes[0], nodeStore);
    NodeID n2 = gm2.join(allNodes[1], nodeStore);

    ThreadUtil.reallySleep(1000);

    assertNotEquals(n1, n2);
    checkSendingReceivingMessages(gm1, l1, gm2, l2, batchSize);

    gm1.shutdown();
    gm2.shutdown();
  }

  public void testSendingReceivingMessagesStatic4M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 12);
  }

  private RelayedCommitTransactionMessage createRealyedCommitTransactionMessage(int batchSize) {
    TestCommitTransactionMessage testCommitTransactionMessage;
    List transactions;
    List serverTransactionIDs;
    int channelId = 2;

    testCommitTransactionMessage = (TestCommitTransactionMessage) new TestCommitTransactionMessageFactory()
        .newCommitTransactionMessage(GroupID.NULL_ID);
    testCommitTransactionMessage.setChannelID(new ClientID(channelId));
    testCommitTransactionMessage.setBatch(new TestTransactionBatch(new TCByteBuffer[] { TCByteBufferFactory
                                              .getInstance(false, batchSize) }), new ObjectStringSerializerImpl());

    serverTransactionIDs = new ArrayList();
    transactions = new ArrayList();
    ClientID cid = new ClientID(channelId);
    for (long i = 10; i < 20; ++i) {
      ServerTransactionID stid = new ServerTransactionID(cid, new TransactionID(i));
      serverTransactionIDs.add(stid);
      transactions.add(new TestServerTransaction(stid, new TxnBatchID(i), new GlobalTransactionID(i)));
    }

    RelayedCommitTransactionMessage osm = RelayedCommitTransactionMessageFactory
        .createRelayedCommitTransactionMessage(testCommitTransactionMessage.getSourceNodeID(),
                                               testCommitTransactionMessage.getBatchData(), transactions, 420,
                                               new GlobalTransactionID(49),
                                               testCommitTransactionMessage.getSerializer());
    return osm;
  }

  private void checkSendingReceivingMessages(TCGroupManagerImpl gm1, MyListener l1, TCGroupManagerImpl gm2,
                                             MyListener l2, int batchSize) {
    ThreadUtil.reallySleep(5 * 1000);

    final RelayedCommitTransactionMessage msg1 = createRealyedCommitTransactionMessage(batchSize);
    gm1.sendAll(msg1);

    RelayedCommitTransactionMessage msg2 = (RelayedCommitTransactionMessage) l2.take();

    int size1 = 0;
    TCByteBuffer[] buf1 = msg1.getBatchData();
    for (int i = 0; i < buf1.length; ++i) {
      size1 += buf1[i].limit();
    }

    int size2 = 0;
    TCByteBuffer[] buf2 = msg2.getBatchData();
    for (int i = 0; i < buf2.length; ++i) {
      size2 += buf2[i].limit();
    }

    assertEquals(size1, size2);
    assertEquals(msg1.getClientID(), msg2.getClientID());

    final RelayedCommitTransactionMessage msg3 = createRealyedCommitTransactionMessage(batchSize);
    gm2.sendAll(msg3);

    RelayedCommitTransactionMessage msg4 = (RelayedCommitTransactionMessage) l1.take();

    int size3 = 0;
    TCByteBuffer[] buf3 = msg3.getBatchData();
    for (int i = 0; i < buf3.length; ++i) {
      size3 += buf3[i].limit();
    }

    int size4 = 0;
    TCByteBuffer[] buf4 = msg4.getBatchData();
    for (int i = 0; i < buf4.length; ++i) {
      size4 += buf4[i].limit();
    }

    assertEquals(size3, size4);
    assertEquals(msg3.getClientID(), msg4.getClientID());
  }

  private static final class MyListener implements GroupMessageListener {

    NoExceptionLinkedQueue queue = new NoExceptionLinkedQueue();

    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      this.queue.put(msg);
    }

    public GroupMessage take() {
      return (GroupMessage) this.queue.take();
    }
  }
}
