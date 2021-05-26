/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */

package com.tc.net.groups;

import com.tc.test.TCTestCase;

// TODO: Fix test to use test message type
public class TCGroupSendLargeMessageTest extends TCTestCase {
//  private final static String LOCALHOST   = "localhost";
//  private static final long   millionOids = 1024 * 1024;
//
  public TCGroupSendLargeMessageTest() {
    
  }
//
  
  public void testNothing() {
//  make unit test framework happy
  }
  
  public void baseTestSendingReceivingMessagesStatic(long oidsCount) throws Exception {
//    System.out.println("Test with ObjectIDs size " + oidsCount);
//    PortChooser pc = new PortChooser();
//    final int p1 = pc.chooseRandom2Port();
//    final int p2 = pc.chooseRandom2Port();
//    final Node[] allNodes = new Node[] { new Node(LOCALHOST, p1, p1 + 1), new Node(LOCALHOST, p2, p2 + 1) };
//
//    StageManager stageManager1 = new StageManagerImpl(new TCThreadGroup(new ThrowableHandlerImpl(null)), new QueueFactory<EventContext>());
//    TCGroupManagerImpl gm1 = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, p1, p1 + 1, stageManager1, null);
//    ConfigurationContext context1 = new ConfigurationContextImpl(stageManager1);
//    stageManager1.startAll(context1, Collections.emptyList());
//    gm1.setDiscover(new TCGroupMemberDiscoveryStatic(gm1));
//    MyListener l1 = new MyListener();
//    gm1.registerForMessages(GCResultMessage.class, l1);
//
//    StageManager stageManager2 = new StageManagerImpl(new TCThreadGroup(new ThrowableHandlerImpl(null)), new QueueFactory<EventContext>());
//    TCGroupManagerImpl gm2 = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, p2, p2 + 1, stageManager2, null);
//    ConfigurationContext context2 = new ConfigurationContextImpl(stageManager2);
//    stageManager2.startAll(context2, Collections.emptyList());
//    gm2.setDiscover(new TCGroupMemberDiscoveryStatic(gm2));
//    MyListener l2 = new MyListener();
//    gm2.registerForMessages(GCResultMessage.class, l2);
//
//    Set<Node> nodeSet = new HashSet<>();
//    Collections.addAll(nodeSet, allNodes);
//    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
//    NodeID n1 = gm1.join(allNodes[0], nodeStore);
//    NodeID n2 = gm2.join(allNodes[1], nodeStore);
//
//    ThreadUtil.reallySleep(1000);
//
//    assertNotEquals(n1, n2);
//    checkSendingReceivingMessages(gm1, l1, gm2, l2, oidsCount);
//
//    gm1.shutdown();
//    gm2.shutdown();
//  }
//
//  public void testSendingReceivingMessagesStatic4M() throws Exception {
//    baseTestSendingReceivingMessagesStatic(millionOids * 4);
//  }
//
//  public void testSendingReceivingMessagesStatic5M() throws Exception {
//    baseTestSendingReceivingMessagesStatic(millionOids * 5);
//  }
//
//  public void testSendingReceivingMessagesStatic10M() throws Exception {
//    baseTestSendingReceivingMessagesStatic(millionOids * 10);
//  }
//
//  public void testSendingReceivingMessagesStatic20M() throws Exception {
//    baseTestSendingReceivingMessagesStatic(millionOids * 20);
//  }
//
//  public void testSendingReceivingMessagesStatic40M() throws Exception {
//    baseTestSendingReceivingMessagesStatic(millionOids * 40);
//  }
//
//  private void checkSendingReceivingMessages(TCGroupManagerImpl gm1, MyListener l1, TCGroupManagerImpl gm2,
//                                             MyListener l2, long oidsCount) {
//    ThreadUtil.reallySleep(5 * 1000);
//
//    ObjectIDSet oidSet = new BitSetObjectIDSet();
//    for (long i = 1; i <= oidsCount; ++i) {
//      oidSet.add(new ObjectID(i));
//    }
//    final GCResultMessage msg1 = new GCResultMessage(createGarbageCollectionInfo(1), oidSet);
//    gm1.sendAll(msg1);
//
//    GCResultMessage msg2 = (GCResultMessage) l2.take();
//
//    assertEquals(msg1.getGCedObjectIDs(), msg2.getGCedObjectIDs());
//    assertEquals(msg1.getGCIterationCount(), msg2.getGCIterationCount());
//
//    oidSet = new BitSetObjectIDSet();
//    for (long i = (oidsCount + 1); i <= (oidsCount * 2); ++i) {
//      oidSet.add(new ObjectID(i));
//    }
//    final GCResultMessage msg3 = new GCResultMessage(createGarbageCollectionInfo(2), oidSet);
//    gm2.sendAll(msg3);
//
//    GCResultMessage msg4 = (GCResultMessage) l1.take();
//
//    assertEquals(msg3.getGCedObjectIDs(), msg4.getGCedObjectIDs());
//    assertEquals(msg3.getGCIterationCount(), msg4.getGCIterationCount());
//
//  }
//
//  private GarbageCollectionInfo createGarbageCollectionInfo(long iteration) {
//    return new GarbageCollectionInfo(new GarbageCollectionID(iteration, UUID.getUUID().toString()),
//                                     GarbageCollectionInfo.Type.FULL_GC);
//  }
//
//  private static final class MyListener implements GroupMessageListener {
//
//    NoExceptionLinkedQueue<GroupMessage> queue = new NoExceptionLinkedQueue<>();
//
//    @Override
//    public void messageReceived(NodeID fromNode, GroupMessage msg) {
//      queue.put(msg);
//    }
//
//    public GroupMessage take() {
//      return queue.take();
//    }
  }
}
