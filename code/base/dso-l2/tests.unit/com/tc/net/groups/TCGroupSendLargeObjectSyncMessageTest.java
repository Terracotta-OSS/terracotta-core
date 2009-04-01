/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.net.groups;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.async.impl.MockSink;
import com.tc.async.impl.StageManagerImpl;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.msg.ObjectSyncMessageFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.NullObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.core.api.TestDNACursor;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.persistence.impl.InMemoryPersistor;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collections;
import java.util.HashMap;

public class TCGroupSendLargeObjectSyncMessageTest extends TCTestCase {
  private final static String LOCALHOST   = "localhost";
  private static final long   millionOids = 1024 * 1024;

  public TCGroupSendLargeObjectSyncMessageTest() {
    //
  }

  public void baseTestSendingReceivingMessagesStatic(long oidsCount) throws Exception {
    System.out.println("Test with ObjectIDs size " + oidsCount);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), new InMemoryPersistor());
    PortChooser pc = new PortChooser();
    final int p1 = pc.chooseRandom2Port();
    final int p2 = pc.chooseRandom2Port();
    final Node[] allNodes = new Node[] { new Node(LOCALHOST, p1, p1 + 1, TCSocketAddress.WILDCARD_IP),
        new Node(LOCALHOST, p2, p2 + 1, TCSocketAddress.WILDCARD_IP) };

    StageManager stageManager1 = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(null)), new QueueFactory());
    TCGroupManagerImpl gm1 = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, p1, p1 + 1, stageManager1);
    ConfigurationContext context1 = new ConfigurationContextImpl(stageManager1);
    stageManager1.startAll(context1, Collections.EMPTY_LIST);
    gm1.setDiscover(new TCGroupMemberDiscoveryStatic(gm1));
    MyListener l1 = new MyListener();
    gm1.registerForMessages(ObjectSyncMessage.class, l1);

    StageManager stageManager2 = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(null)), new QueueFactory());
    TCGroupManagerImpl gm2 = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, p2, p2 + 1, stageManager2);
    ConfigurationContext context2 = new ConfigurationContextImpl(stageManager2);
    stageManager2.startAll(context2, Collections.EMPTY_LIST);
    gm2.setDiscover(new TCGroupMemberDiscoveryStatic(gm2));
    MyListener l2 = new MyListener();
    gm2.registerForMessages(ObjectSyncMessage.class, l2);

    NodeID n1 = gm1.join(allNodes[0], allNodes);
    NodeID n2 = gm2.join(allNodes[1], allNodes);

    ThreadUtil.reallySleep(1000);

    assertNotEquals(n1, n2);
    checkSendingReceivingMessages(gm1, l1, gm2, l2, oidsCount);

    gm1.shutdown();
    gm2.shutdown();
  }

  public void testSendingReceivingMessagesStatic4M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 1);
  }

  private ObjectSyncMessage createObjectSyncMessage(long oidsCount) {

    NodeID nodeID = new ServerID("foo", "foobar".getBytes());
    HashMap rootsMap = new HashMap();
    for (long i = 0; i < 10; ++i) {
      rootsMap.put("root" + i, new ObjectID(i));
    }
    Sink sink = new MockSink();
    ObjectStringSerializer objectStringSerializer = new ObjectStringSerializer();
    ManagedObjectSyncContext managedObjectSyncContext = new ManagedObjectSyncContext(
                                                                                     nodeID,
                                                                                     rootsMap,
                                                                                     new ObjectIDSet(rootsMap.values()),
                                                                                     true, sink, 100, 10);
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    for (long i = 0; i < oidsCount; ++i) {
      ManagedObject m = new ManagedObjectImpl(new ObjectID(i));
      m.apply(new TestDNA(new TestDNACursor()), new TransactionID(i), new BackReferences(),
              new NullObjectInstanceMonitor(), true);
      m.toDNA(out, objectStringSerializer);
    }
    managedObjectSyncContext.setDehydratedBytes(out.toArray(), (int) oidsCount, objectStringSerializer);
    managedObjectSyncContext.setSequenceID(11);

    ObjectSyncMessage osm = ObjectSyncMessageFactory
        .createObjectSyncMessageFrom(managedObjectSyncContext, new ServerTransactionID(new ServerID("xyz", new byte[] {
            3, 4, 5 }), new TransactionID(99)));
    return osm;
  }

  private void checkSendingReceivingMessages(TCGroupManagerImpl gm1, MyListener l1, TCGroupManagerImpl gm2,
                                             MyListener l2, long oidsCount) {
    ThreadUtil.reallySleep(5 * 1000);

    final ObjectSyncMessage msg1 = createObjectSyncMessage(oidsCount);
    gm1.sendAll(msg1);

    ObjectSyncMessage msg2 = (ObjectSyncMessage) l2.take();

    assertEquals(msg1.getDnaCount(), msg2.getDnaCount());
    assertEquals(msg1.getOids().size(), msg2.getOids().size());

    final ObjectSyncMessage msg3 = createObjectSyncMessage(oidsCount);
    gm2.sendAll(msg3);

    ObjectSyncMessage msg4 = (ObjectSyncMessage) l1.take();

    assertEquals(msg3.getDnaCount(), msg4.getDnaCount());
    assertEquals(msg3.getOids().size(), msg4.getOids().size());
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
