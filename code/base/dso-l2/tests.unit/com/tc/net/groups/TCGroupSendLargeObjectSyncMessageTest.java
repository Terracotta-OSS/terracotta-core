/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.net.groups;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.async.impl.StageManagerImpl;
import com.tc.config.NodesStore;
import com.tc.config.NodesStoreImpl;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.msg.ObjectSyncMessageFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.NullObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.persistence.inmemory.InMemoryPersistor;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet;
import com.tc.util.PortChooser;
import com.tc.util.TCCollections;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TCGroupSendLargeObjectSyncMessageTest extends TCTestCase {
  private final static String LOCALHOST   = "localhost";
  private static final long   millionOids = 1024 * 1024;

  public TCGroupSendLargeObjectSyncMessageTest() {
    //
  }

  public void baseTestSendingReceivingMessagesStatic(final long oidsCount) throws Exception {
    System.out.println("Test with ObjectIDs size " + oidsCount);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), new InMemoryPersistor());
    final PortChooser pc = new PortChooser();
    final int p1 = pc.chooseRandom2Port();
    final int p2 = pc.chooseRandom2Port();
    final Node[] allNodes = new Node[] { new Node(LOCALHOST, p1, p1 + 1), new Node(LOCALHOST, p2, p2 + 1) };

    final StageManager stageManager1 = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(null)),
                                                            new QueueFactory());
    final TCGroupManagerImpl gm1 = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, p1, p1 + 1,
                                                          stageManager1);
    final ConfigurationContext context1 = new ConfigurationContextImpl(stageManager1);
    stageManager1.startAll(context1, Collections.EMPTY_LIST);
    gm1.setDiscover(new TCGroupMemberDiscoveryStatic(gm1));
    final MyListener l1 = new MyListener();
    gm1.registerForMessages(ObjectSyncMessage.class, l1);

    final StageManager stageManager2 = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(null)),
                                                            new QueueFactory());
    final TCGroupManagerImpl gm2 = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, p2, p2 + 1,
                                                          stageManager2);
    final ConfigurationContext context2 = new ConfigurationContextImpl(stageManager2);
    stageManager2.startAll(context2, Collections.EMPTY_LIST);
    gm2.setDiscover(new TCGroupMemberDiscoveryStatic(gm2));
    final MyListener l2 = new MyListener();
    gm2.registerForMessages(ObjectSyncMessage.class, l2);

    final Set<Node> nodeSet = new HashSet<Node>();
    Collections.addAll(nodeSet, allNodes);
    final NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    final NodeID n1 = gm1.join(allNodes[0], nodeStore);
    final NodeID n2 = gm2.join(allNodes[1], nodeStore);

    ThreadUtil.reallySleep(1000);

    assertNotEquals(n1, n2);
    checkSendingReceivingMessages(gm1, l1, gm2, l2, oidsCount);

    gm1.shutdown();
    gm2.shutdown();
  }

  public void testSendingReceivingMessagesStatic4M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 1);
  }

  private ObjectSyncMessage createObjectSyncMessage(final long oidsCount) {

    final NodeID nodeID = new ServerID("foo", "foobar".getBytes());
    final HashMap rootsMap = new HashMap();
    for (long i = 0; i < 10; ++i) {
      rootsMap.put("root" + i, new ObjectID(i));
    }
    final ObjectStringSerializer objectStringSerializer = new ObjectStringSerializerImpl();
    final ManagedObjectSyncContext managedObjectSyncContext = new ManagedObjectSyncContext(nodeID, rootsMap,
                                                                                           new ObjectIDSet(rootsMap
                                                                                               .values()), true, 100,
                                                                                           10);
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    ObjectIDSet oidSet = new ObjectIDSet();
    for (long i = 0; i < oidsCount; ++i) {
      ObjectID oid = new ObjectID(i);
      oidSet.add(oid);
      final ManagedObject m = new ManagedObjectImpl(oid);
      m.apply(new TestDNA(new TestDNACursor()), new TransactionID(i), new ApplyTransactionInfo(),
              new NullObjectInstanceMonitor(), true);
      m.toDNA(out, objectStringSerializer, DNAType.L2_SYNC);
    }
    managedObjectSyncContext.setDehydratedBytes(oidSet, TCCollections.EMPTY_OBJECT_ID_SET, out.toArray(),
                                                (int) oidsCount, objectStringSerializer);
    managedObjectSyncContext.setSequenceID(11);

    final ObjectSyncMessage osm = ObjectSyncMessageFactory
        .createObjectSyncMessageFrom(managedObjectSyncContext, new ServerTransactionID(new ServerID("xyz", new byte[] {
            3, 4, 5 }), new TransactionID(99)));
    return osm;
  }

  private void checkSendingReceivingMessages(final TCGroupManagerImpl gm1, final MyListener l1,
                                             final TCGroupManagerImpl gm2, final MyListener l2, final long oidsCount) {
    ThreadUtil.reallySleep(5 * 1000);

    final ObjectSyncMessage msg1 = createObjectSyncMessage(oidsCount);
    gm1.sendAll(msg1);

    final ObjectSyncMessage msg2 = (ObjectSyncMessage) l2.take();

    assertEquals(msg1.getDnaCount(), msg2.getDnaCount());
    assertEquals(msg1.getOids().size(), msg2.getOids().size());

    final ObjectSyncMessage msg3 = createObjectSyncMessage(oidsCount);
    gm2.sendAll(msg3);

    final ObjectSyncMessage msg4 = (ObjectSyncMessage) l1.take();

    assertEquals(msg3.getDnaCount(), msg4.getDnaCount());
    assertEquals(msg3.getOids().size(), msg4.getOids().size());
  }

  private static final class MyListener implements GroupMessageListener {

    NoExceptionLinkedQueue queue = new NoExceptionLinkedQueue();

    public void messageReceived(final NodeID fromNode, final GroupMessage msg) {
      this.queue.put(msg);
    }

    public GroupMessage take() {
      return (GroupMessage) this.queue.take();
    }
  }
}
