/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.net.groups;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.async.impl.StageManagerImpl;
import com.tc.l2.msg.GCResultMessage;
import com.tc.l2.msg.GCResultMessageFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet2;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

public class TCGroupSendLargeMessageTest extends TCTestCase {
  private final static String LOCALHOST   = "localhost";
  private static final long   millionOids = 1024 * 1024;

  public TCGroupSendLargeMessageTest() {
    //
  }

  public void baseTestSendingReceivingMessagesStatic(long oidsCount) throws Exception {
    System.out.println("Test with ObjectIDs size " + oidsCount);
    PortChooser pc = new PortChooser();
    final int p1 = pc.chooseRandomPort();
    final int p2 = pc.chooseRandomPort();
    final Node[] allNodes = new Node[] { new Node(LOCALHOST, p1, TCSocketAddress.WILDCARD_IP),
        new Node(LOCALHOST, p2, TCSocketAddress.WILDCARD_IP) };

    StageManager stageManager1 = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(null)), new QueueFactory());
    TCGroupManagerImpl gm1 = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, p1, stageManager1);
    ConfigurationContext context1 = new ConfigurationContextImpl(stageManager1);
    stageManager1.startAll(context1);
    gm1.setDiscover(new TCGroupMemberDiscoveryStatic(gm1));
    MyListener l1 = new MyListener();
    gm1.registerForMessages(GCResultMessage.class, l1);

    StageManager stageManager2 = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(null)), new QueueFactory());
    TCGroupManagerImpl gm2 = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, p2, stageManager2);
    ConfigurationContext context2 = new ConfigurationContextImpl(stageManager2);
    stageManager2.startAll(context2);
    gm2.setDiscover(new TCGroupMemberDiscoveryStatic(gm2));
    MyListener l2 = new MyListener();
    gm2.registerForMessages(GCResultMessage.class, l2);

    NodeID n1 = gm1.join(allNodes[0], allNodes);
    NodeID n2 = gm2.join(allNodes[1], allNodes);

    ThreadUtil.reallySleep(1000);

    assertNotEquals(n1, n2);
    checkSendingReceivingMessages(gm1, l1, gm2, l2, oidsCount);

    gm1.shutdown();
    gm2.shutdown();
  }

  public void testSendingReceivingMessagesStatic4M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 4);
  }

  public void testSendingReceivingMessagesStatic5M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 5);
  }

  public void testSendingReceivingMessagesStatic10M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 10);
  }

  public void testSendingReceivingMessagesStatic20M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 20);
  }

  public void testSendingReceivingMessagesStatic40M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 40);
  }

  private void checkSendingReceivingMessages(TCGroupManagerImpl gm1, MyListener l1, TCGroupManagerImpl gm2,
                                             MyListener l2, long oidsCount) throws GroupException {
    ThreadUtil.reallySleep(5 * 1000);

    ObjectIDSet2 oidSet = new ObjectIDSet2();
    for (long i = 1; i <= oidsCount; ++i) {
      oidSet.add(new ObjectID(i));
    }
    final GCResultMessage msg1 = GCResultMessageFactory.createGCResultMessage(oidSet);
    gm1.sendAll(msg1);

    GCResultMessage msg2 = (GCResultMessage) l2.take();

    assertEquals(msg1.getGCedObjectIDs(), msg2.getGCedObjectIDs());

    oidSet = new ObjectIDSet2();
    for (long i = (oidsCount + 1); i <= (oidsCount * 2); ++i) {
      oidSet.add(new ObjectID(i));
    }
    final GCResultMessage msg3 = GCResultMessageFactory.createGCResultMessage(oidSet);
    gm2.sendAll(msg3);

    GCResultMessage msg4 = (GCResultMessage) l1.take();

    assertEquals(msg3.getGCedObjectIDs(), msg4.getGCedObjectIDs());

  }

  private static final class MyListener implements GroupMessageListener {

    NoExceptionLinkedQueue queue = new NoExceptionLinkedQueue();

    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      queue.put(msg);
    }

    public GroupMessage take() {
      return (GroupMessage) queue.take();
    }
  }
}
