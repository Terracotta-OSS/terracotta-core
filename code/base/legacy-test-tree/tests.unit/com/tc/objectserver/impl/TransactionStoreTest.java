/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.objectserver.persistence.impl.TestPersistenceTransaction;
import com.tc.objectserver.persistence.impl.TransactionStoreImpl;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.sequence.Sequence;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransactionStoreTest extends TCTestCase {

  private TestTransactionPersistor persistor;
  private TransactionStore         store;
  private Map                      sid2Gid;

  public void testDeleteByGlobalTransactionID() throws Exception {
    persistor = new TestTransactionPersistor();
    store = new TransactionStoreImpl(persistor, persistor);
    sid2Gid = new HashMap();
    List gtxs = new LinkedList();
    for (int i = 0; i < 100; i++) {
      ServerTransactionID sid1 = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      createAndAddGIDFor(sid1);
      GlobalTransactionDescriptor desc = store.createTransactionDescriptor(sid1);
      store.commitTransactionDescriptor(null, desc);
      assertNotNull(store.getTransactionDescriptor(new ServerTransactionID(desc.getChannelID(), desc
          .getClientTransactionID())));
      gtxs.add(desc);
    }
    final GlobalTransactionDescriptor originalMin = (GlobalTransactionDescriptor) gtxs.get(0);

    assertEquals(getGlobalTransactionID(originalMin), store.getLeastGlobalTransactionID());

    // create a set of transactions to delete
    Collection toDelete = new HashSet();
    Collection toDeleteIDs = new HashSet();
    for (int i = 0; i < 10; i++) {

      GlobalTransactionDescriptor o = (GlobalTransactionDescriptor) gtxs.remove(0);
      toDelete.add(o);
      toDeleteIDs.add(o.getServerTransactionID());
    }
    assertFalse(originalMin == gtxs.get(0));

    // delete the set
    store.removeAllByServerTransactionID(null, toDeleteIDs);

    GlobalTransactionDescriptor currentMin = (GlobalTransactionDescriptor) gtxs.get(0);
    // make sure the min has been adjusted properly
    assertEquals(getGlobalTransactionID(currentMin), store.getLeastGlobalTransactionID());
    // make sure the deleted set has actually been deleted
    for (Iterator i = toDelete.iterator(); i.hasNext();) {
      GlobalTransactionDescriptor desc = (GlobalTransactionDescriptor) i.next();
      assertNull(store.getTransactionDescriptor(desc.getServerTransactionID()));
    }

    // make sure the persistor is told to delete them all
    assertEquals(toDeleteIDs, persistor.deleteQueue.poll(1));
  }

  private GlobalTransactionID getGlobalTransactionID(GlobalTransactionDescriptor gtd) {
    return (GlobalTransactionID) sid2Gid.get(gtd.getServerTransactionID());
  }

  private void createAndAddGIDFor(ServerTransactionID sid1) {
    sid2Gid.put(sid1, store.createGlobalTransactionID(sid1));
  }

  public void testLeastGlobalTransactionID() throws Exception {

    persistor = new TestTransactionPersistor();
    store = new TransactionStoreImpl(persistor, persistor);
    sid2Gid = new HashMap();

    assertEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());

    ServerTransactionID stx1 = new ServerTransactionID(new ChannelID(1), new TransactionID(1));

    GlobalTransactionDescriptor gtx1 = store.createTransactionDescriptor(stx1);
    assertEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());
    createAndAddGIDFor(stx1);

    store.commitTransactionDescriptor(null, gtx1);
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    int min = 100;
    int max = 200;
    for (int i = min; i < max; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      GlobalTransactionDescriptor gtxi = store.createTransactionDescriptor(stxid);
      createAndAddGIDFor(stxid);
      store.commitTransactionDescriptor(null, gtxi);
    }

    // Still the least Global Txn ID is the same
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    // now remove some from the the middle
    Set toDelete = new HashSet();
    for (int i = min + 50; i < max - 50; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      toDelete.add(stxid);
    }
    store.removeAllByServerTransactionID(null, toDelete);

    // Still the least Global Txn ID is the same
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    // now remove some from the beginning
    toDelete.clear();
    for (int i = min; i < min + 50; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      toDelete.add(stxid);
    }
    toDelete.add(stx1);
    store.removeAllByServerTransactionID(null, toDelete);

    // the least Global Txn ID is not the same
    assertNotEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());
    assertTrue(getGlobalTransactionID(gtx1).toLong() < store.getLeastGlobalTransactionID().toLong());

    // RESTART scenario
    persistor = new TestTransactionPersistor();
    for (int i = min; i < max; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      persistor.persisted.add(new GlobalTransactionDescriptor(stxid));
      createAndAddGIDFor(stxid);
    }
    store = new TransactionStoreImpl(persistor, persistor);

    // The store should be initialized with the least transaction id set to negative number
    GlobalTransactionID least = store.getLeastGlobalTransactionID();
    assertTrue(least.toLong() < 0);

    // now remove some from the the middle
    toDelete.clear();
    for (int i = min + 50; i < max - 50; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      toDelete.add(stxid);
    }
    store.removeAllByServerTransactionID(null, toDelete);

    // Still the least Global Txn ID is the same
    assertEquals(least, store.getLeastGlobalTransactionID());

    // now remove some more
    toDelete.clear();
    for (int i = min; i < min + 50; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      toDelete.add(stxid);
    }
    store.removeAllByServerTransactionID(null, toDelete);

    // the least Global Txn ID is still negative
    least = store.getLeastGlobalTransactionID();
    assertTrue(least.toLong() < 0);

    // now resend all the remaining txns
    toDelete.clear();
    for (int i = max - 50; i < max; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      createAndAddGIDFor(stxid);
    }

    // now remove each of the remaining ones
    for (int i = max - 50; i < max; i++) {
      // the least Global Txn ID is not the same
      assertNotEquals(least, store.getLeastGlobalTransactionID());
      assertTrue(least.toLong() < store.getLeastGlobalTransactionID().toLong());
      least = store.getLeastGlobalTransactionID();
      assertTrue(least.toLong() > 0);

      toDelete.clear();
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      toDelete.add(stxid);
      store.removeAllByServerTransactionID(null, toDelete);
    }

    assertNotEquals(least, store.getLeastGlobalTransactionID());
    least = store.getLeastGlobalTransactionID();
    assertTrue(least.isNull());
  }

  public void testClientShutdown() throws Exception {
    long sequence = 0;
    int initialMin = 200;
    int initialMax = 300;
    int laterMax = 400;
    persistor = new TestTransactionPersistor();
    for (int i = initialMin; i < initialMax; i++) {
      sequence++;
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i % 2), new TransactionID(i));
      persistor.persisted.add(new GlobalTransactionDescriptor(stxid));
    }
    store = new TransactionStoreImpl(persistor, persistor);
    GlobalTransactionID lowmk1 = store.getLeastGlobalTransactionID();

    // create more
    for (int i = initialMax; i < laterMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i % 2), new TransactionID(i));
      store.createGlobalTransactionID(stxid);
      store.commitTransactionDescriptor(null, store.createTransactionDescriptor(stxid));
    }
    GlobalTransactionID lowmk2 = store.getLeastGlobalTransactionID();

    assertEquals(lowmk1, lowmk2);

    ChannelID channel0 = new ChannelID(0);
    store.shutdownClient(null, channel0);

    // Check if all channel1 IDs are gone
    for (int i = initialMin; i < laterMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i % 2), new TransactionID(i));
      if (i % 2 == 0) {
        assertNull(store.getTransactionDescriptor(stxid));
      } else {
        assertNotNull(store.getTransactionDescriptor(stxid));
      }
    }
  }

  public void tests() throws Exception {
    long sequence = 0;
    int initialMin = 200;
    int initialMax = 300;
    persistor = new TestTransactionPersistor();
    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      persistor.persisted.add(new GlobalTransactionDescriptor(stxid));
    }
    store = new TransactionStoreImpl(persistor, persistor);

    // make sure that the persisted transaction ids get loaded in the
    // constructor.

    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      assertNotNull(store.getTransactionDescriptor(stxid));
    }

    ChannelID channel1 = new ChannelID(1);
    ChannelID channel2 = new ChannelID(2);
    TransactionID tx1 = new TransactionID(1);
    TransactionID tx2 = new TransactionID(2);
    ServerTransactionID stxid1 = new ServerTransactionID(channel1, tx1);
    ServerTransactionID stxid2 = new ServerTransactionID(channel2, tx2);

    assertNull(store.getTransactionDescriptor(stxid1));
    store.createGlobalTransactionID(stxid1);
    GlobalTransactionDescriptor gtx1 = store.createTransactionDescriptor(stxid1);
    assertEquals(gtx1, store.getTransactionDescriptor(stxid1));

    assertSame(gtx1, store.getTransactionDescriptor(stxid1));

    assertEquals(++sequence, persistor.sequence);

    assertNull(store.getTransactionDescriptor(stxid2));
    GlobalTransactionDescriptor gtx2 = store.createTransactionDescriptor(stxid2);
    store.createGlobalTransactionID(stxid2);
    assertEquals(gtx2, store.getTransactionDescriptor(stxid2));
    assertEquals(++sequence, persistor.sequence);

    PersistenceTransaction ptx = new TestPersistenceTransaction();
    store.commitTransactionDescriptor(ptx, gtx1);
    Object[] args = (Object[]) persistor.storeQueue.poll(1);
    assertTrue(persistor.storeQueue.isEmpty());
    assertSame(ptx, args[0]);
    assertSame(gtx1, args[1]);

    store.commitTransactionDescriptor(ptx, gtx2);
    args = (Object[]) persistor.storeQueue.poll(1);
    assertTrue(persistor.storeQueue.isEmpty());
    assertSame(ptx, args[0]);
    assertSame(gtx2, args[1]);
  }

  private static final class TestTransactionPersistor implements TransactionPersistor, Sequence {

    public final NoExceptionLinkedQueue deleteQueue = new NoExceptionLinkedQueue();
    public final List                   persisted   = new LinkedList();
    public final NoExceptionLinkedQueue storeQueue  = new NoExceptionLinkedQueue();
    public long                         sequence    = 0;

    public Collection loadAllGlobalTransactionDescriptors() {
      return persisted;
    }

    public void saveGlobalTransactionDescriptor(PersistenceTransaction tx, GlobalTransactionDescriptor gtx) {
      storeQueue.put(new Object[] { tx, gtx });
    }

    public long next() {
      return ++sequence;
    }

    public void deleteAllByServerTransactionID(PersistenceTransaction tx, Collection toDelete) {
      deleteQueue.put(toDelete);
    }

  }
}
