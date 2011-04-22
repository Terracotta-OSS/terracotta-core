/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.objectserver.persistence.db.TransactionStoreImpl;
import com.tc.objectserver.persistence.impl.TestPersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.sequence.Sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class TransactionStoreTest extends TCTestCase {

  private TestTransactionPersistor persistor;
  private TransactionStore         store;

  public void testDeleteByGlobalTransactionID() throws Exception {
    persistor = new TestTransactionPersistor();
    store = new TransactionStoreImpl(persistor, persistor);
    List gtxs = new LinkedList();
    for (int i = 0; i < 100; i++) {
      ServerTransactionID sid1 = new ServerTransactionID(new ClientID(i), new TransactionID(i));
      store.getOrCreateTransactionDescriptor(sid1);
      store.commitTransactionDescriptor(null, sid1);
      GlobalTransactionDescriptor desc = store.getTransactionDescriptor(sid1);
      assertNotNull(desc);
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
    store.clearCommitedTransactionsBelowLowWaterMark(null, getGlobalTransactionID((GlobalTransactionDescriptor) gtxs
        .get(0)));

    GlobalTransactionDescriptor currentMin = (GlobalTransactionDescriptor) gtxs.get(0);
    // make sure the min has been adjusted properly
    assertEquals(getGlobalTransactionID(currentMin), store.getLeastGlobalTransactionID());
    // make sure the deleted set has actually been deleted
    Set deletedSids = new HashSet();
    for (Iterator i = toDelete.iterator(); i.hasNext();) {
      GlobalTransactionDescriptor desc = (GlobalTransactionDescriptor) i.next();
      assertNull(store.getTransactionDescriptor(desc.getServerTransactionID()));
      deletedSids.add(desc.getServerTransactionID());
    }

    // make sure the persistor is told to delete them all
    assertEquals(deletedSids, new HashSet((Collection) persistor.deleteQueue.poll(1)));
  }

  public void testLeastGlobalTransactionID() throws Exception {

    persistor = new TestTransactionPersistor();
    store = new TransactionStoreImpl(persistor, persistor);

    assertEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());

    ServerTransactionID stx1 = new ServerTransactionID(new ClientID(1), new TransactionID(1));

    GlobalTransactionDescriptor gtx1 = store.getOrCreateTransactionDescriptor(stx1);
    assertNotEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    store.commitTransactionDescriptor(null, stx1);
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    int min = 10;
    int max = 20;
    List gds = new ArrayList();
    for (int i = min; i < max; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(2), new TransactionID(i));
      GlobalTransactionDescriptor gd = store.getOrCreateTransactionDescriptor(stxid);
      gds.add(gd);
      store.commitTransactionDescriptor(null, stxid);
    }

    // Still the least Global Txn ID is the same
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    store.clearCommitedTransactionsBelowLowWaterMark(null, ((GlobalTransactionDescriptor) gds.get(0))
        .getServerTransactionID());

    // Still the least Global Txn ID is the same, since CLIENT(1) is still holding the low water mark
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    ServerTransactionID stx2 = new ServerTransactionID(stx1.getSourceID(), stx1.getClientTransactionID().next());
    store.clearCommitedTransactionsBelowLowWaterMark(null, stx2);

    // least Global Txn ID is not the same
    assertNotEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    GlobalTransactionID currentLWM = store.getLeastGlobalTransactionID();

    // send LWM of the next txn
    store.clearCommitedTransactionsBelowLowWaterMark(null, ((GlobalTransactionDescriptor) gds.get(1))
        .getServerTransactionID());

    // least Global Txn ID is not the same
    assertNotEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertTrue(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());
    currentLWM = store.getLeastGlobalTransactionID();

    // send LWM of the last txn
    store.clearCommitedTransactionsBelowLowWaterMark(null, ((GlobalTransactionDescriptor) gds.get(gds.size() - 1))
        .getServerTransactionID());

    // least Global Txn ID is not the same
    assertNotEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertTrue(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());
    currentLWM = store.getLeastGlobalTransactionID();

    // send LWM above the last txn
    ServerTransactionID sid = ((GlobalTransactionDescriptor) gds.get(gds.size() - 1)).getServerTransactionID();
    sid = new ServerTransactionID(sid.getSourceID(), sid.getClientTransactionID().next());
    store.clearCommitedTransactionsBelowLowWaterMark(null, sid);

    // least Global Txn ID is not the same, its null
    assertNotEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());
  }

  public void testLeastGlobalTransactionIDInPassiveServer() throws Exception {

    persistor = new TestTransactionPersistor();
    store = new TransactionStoreImpl(persistor, persistor);

    assertEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());

    ServerTransactionID stx1 = new ServerTransactionID(new ClientID(1), new TransactionID(1));

    GlobalTransactionDescriptor gtx1 = store.getOrCreateTransactionDescriptor(stx1);
    assertNotEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    store.commitTransactionDescriptor(null, stx1);
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    int min = 10;
    int max = 20;
    List gds = new ArrayList();
    for (int i = min; i < max; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(2), new TransactionID(i));
      GlobalTransactionDescriptor gd = store.getOrCreateTransactionDescriptor(stxid);
      gds.add(gd);
    }

    // Still the least Global Txn ID is the same
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    // No need to send LWM per client in the PASSIVE, Client(1)'s txn is cleared
    store.clearCommitedTransactionsBelowLowWaterMark(null, ((GlobalTransactionDescriptor) gds.get(0))
        .getGlobalTransactionID());

    // least Global Txn ID is not the same
    assertNotEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    GlobalTransactionID currentLWM = store.getLeastGlobalTransactionID();

    // send LWM of the next txn
    store.clearCommitedTransactionsBelowLowWaterMark(null, ((GlobalTransactionDescriptor) gds.get(1))
        .getGlobalTransactionID());

    // least Global Txn ID is STILL the same, since the transactions are not commited.
    assertEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertFalse(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());

    // commit transaction
    store.commitTransactionDescriptor(null, ((GlobalTransactionDescriptor) gds.get(0)).getServerTransactionID());

    // least Global Txn ID is STILL the same, only when the next LWM msg comes along it clears the data structures.
    assertEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertFalse(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());

    // send LWM again
    store.clearCommitedTransactionsBelowLowWaterMark(null, ((GlobalTransactionDescriptor) gds.get(1))
        .getGlobalTransactionID());

    // Now LWM should have moved up
    assertNotEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertTrue(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());
    currentLWM = store.getLeastGlobalTransactionID();

    // send LWM of the last txn
    store.clearCommitedTransactionsBelowLowWaterMark(null, ((GlobalTransactionDescriptor) gds.get(gds.size() - 1))
        .getGlobalTransactionID());

    // least Global Txn ID is STILL the same, since the transactions are not commited.
    assertEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertFalse(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());

    for (int i = 1; i < gds.size(); i++) {
      GlobalTransactionDescriptor gd = (GlobalTransactionDescriptor) gds.get(i);
      store.commitTransactionDescriptor(null, gd.getServerTransactionID());
    }

    // least Global Txn ID is STILL the same, only when the next LWM msg comes along it clears the data structures.
    assertEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertFalse(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());

    // send LWM again
    store.clearCommitedTransactionsBelowLowWaterMark(null, ((GlobalTransactionDescriptor) gds.get(gds.size() - 1))
        .getGlobalTransactionID());

    // least Global Txn ID is not the same
    assertNotEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertTrue(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());
    currentLWM = store.getLeastGlobalTransactionID();

    // send LWM above the last txn - we use SID instead of GID here, not a real case in passive
    ServerTransactionID sid = ((GlobalTransactionDescriptor) gds.get(gds.size() - 1)).getServerTransactionID();
    sid = new ServerTransactionID(sid.getSourceID(), sid.getClientTransactionID().next());
    store.clearCommitedTransactionsBelowLowWaterMark(null, sid);

    // least Global Txn ID is not the same, its null
    assertNotEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());
  }

  private GlobalTransactionID getGlobalTransactionID(GlobalTransactionDescriptor gtx) {
    return gtx.getGlobalTransactionID();
  }

  public void testClientShutdown() throws Exception {
    long sequence = 0;
    int initialMin = 200;
    int initialMax = 300;
    int laterMax = 400;
    persistor = new TestTransactionPersistor();
    for (int i = initialMin; i < initialMax; i++) {
      sequence++;
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i % 2), new TransactionID(i));
      persistor.persisted.put(stxid, new GlobalTransactionDescriptor(stxid, new GlobalTransactionID(persistor.next())));
    }
    store = new TransactionStoreImpl(persistor, persistor);
    GlobalTransactionID lowmk1 = store.getLeastGlobalTransactionID();

    // create more
    for (int i = initialMax; i < laterMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i % 2), new TransactionID(i));
      store.getOrCreateTransactionDescriptor(stxid);
      store.commitTransactionDescriptor(null, stxid);
    }
    GlobalTransactionID lowmk2 = store.getLeastGlobalTransactionID();

    assertEquals(lowmk1, lowmk2);

    ClientID cid0 = new ClientID(0);
    store.shutdownNode(null, cid0);

    // Check if all channel1 IDs are gone
    for (int i = initialMin; i < laterMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i % 2), new TransactionID(i));
      if (i % 2 == 0) {
        assertNull(store.getTransactionDescriptor(stxid));
      } else {
        assertNotNull(store.getTransactionDescriptor(stxid));
      }
    }
  }

  public void tests() throws Exception {
    int initialMin = 200;
    int initialMax = 300;
    persistor = new TestTransactionPersistor();
    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i), new TransactionID(i));
      persistor.persisted.put(stxid, new GlobalTransactionDescriptor(stxid, new GlobalTransactionID(persistor.next())));
    }
    store = new TransactionStoreImpl(persistor, persistor);

    // make sure that the persisted transaction ids get loaded in the
    // constructor.

    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i), new TransactionID(i));
      assertNotNull(store.getTransactionDescriptor(stxid));
    }

    ChannelID channel1 = new ChannelID(1);
    ChannelID channel2 = new ChannelID(2);
    TransactionID tx1 = new TransactionID(1);
    TransactionID tx2 = new TransactionID(2);
    ServerTransactionID stxid1 = new ServerTransactionID(new ClientID(channel1.toLong()), tx1);
    ServerTransactionID stxid2 = new ServerTransactionID(new ClientID(channel2.toLong()), tx2);

    assertNull(store.getTransactionDescriptor(stxid1));
    GlobalTransactionDescriptor gtx1 = store.getOrCreateTransactionDescriptor(stxid1);
    assertEquals(gtx1, store.getTransactionDescriptor(stxid1));

    assertSame(gtx1, store.getTransactionDescriptor(stxid1));

    assertNull(store.getTransactionDescriptor(stxid2));
    GlobalTransactionDescriptor gtx2 = store.getOrCreateTransactionDescriptor(stxid2);
    assertEquals(gtx2, store.getTransactionDescriptor(stxid2));

    PersistenceTransaction ptx = new TestPersistenceTransaction();
    store.commitTransactionDescriptor(ptx, stxid1);
    Object[] args = (Object[]) persistor.storeQueue.poll(1);
    assertTrue(persistor.storeQueue.isEmpty());
    assertSame(ptx, args[0]);
    assertSame(gtx1, args[1]);

    store.commitTransactionDescriptor(ptx, stxid2);
    args = (Object[]) persistor.storeQueue.poll(1);
    assertTrue(persistor.storeQueue.isEmpty());
    assertSame(ptx, args[0]);
    assertSame(gtx2, args[1]);
  }

  public void testSameGIDAssignedOnRestart() throws Exception {
    int initialMin = 200;
    int initialMax = 300;
    int laterMax = 400;
    persistor = new TestTransactionPersistor();
    store = new TransactionStoreImpl(persistor, persistor);
    Map sid2Gid = new HashMap();
    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i % 2), new TransactionID(i));
      GlobalTransactionDescriptor desc = store.getOrCreateTransactionDescriptor(stxid);
      store.commitTransactionDescriptor(null, stxid);
      assertEquals(stxid, desc.getServerTransactionID());
      sid2Gid.put(stxid, desc);
    }

    // RESTART
    store = new TransactionStoreImpl(persistor, persistor);

    // test if we get the same gid
    GlobalTransactionID maxID = GlobalTransactionID.NULL_ID;
    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i % 2), new TransactionID(i));
      GlobalTransactionDescriptor desc = (GlobalTransactionDescriptor) sid2Gid.get(stxid);
      assertEquals(desc, store.getTransactionDescriptor(stxid));
      if (desc.getGlobalTransactionID().toLong() > maxID.toLong()) {
        maxID = desc.getGlobalTransactionID();
      }
    }

    // create more
    for (int i = initialMax; i < laterMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i % 2), new TransactionID(i));
      GlobalTransactionDescriptor desc;
      desc = store.getOrCreateTransactionDescriptor(stxid);
      store.commitTransactionDescriptor(null, stxid);
      assertTrue(maxID.toLong() < desc.getGlobalTransactionID().toLong());
    }
  }

  private static final class TestTransactionPersistor implements TransactionPersistor, Sequence {

    public final NoExceptionLinkedQueue deleteQueue = new NoExceptionLinkedQueue();
    public final LinkedHashMap          persisted   = new LinkedHashMap();
    public final NoExceptionLinkedQueue storeQueue  = new NoExceptionLinkedQueue();
    public long                         sequence    = 0;

    public Collection loadAllGlobalTransactionDescriptors() {
      return getNewGlobalTransactionDescs(persisted.values());
    }

    private Collection getNewGlobalTransactionDescs(Collection c) {
      Collection newList = new ArrayList(c.size());
      for (Iterator i = c.iterator(); i.hasNext();) {
        GlobalTransactionDescriptor oldGD = (GlobalTransactionDescriptor) i.next();
        newList.add(new GlobalTransactionDescriptor(oldGD.getServerTransactionID(), oldGD.getGlobalTransactionID()));
      }
      return newList;
    }

    public void saveGlobalTransactionDescriptor(PersistenceTransaction tx, GlobalTransactionDescriptor gtx) {
      storeQueue.put(new Object[] { tx, gtx });
      persisted.put(gtx.getServerTransactionID(), gtx);
    }

    public long next() {
      return ++sequence;
    }

    public long current() {
      return sequence;
    }

    public void deleteAllGlobalTransactionDescriptors(PersistenceTransaction tx, SortedSet<ServerTransactionID> toDelete) {
      deleteQueue.put(toDelete);
      for (Iterator<ServerTransactionID> i = toDelete.iterator(); i.hasNext();) {
        ServerTransactionID sid = i.next();
        persisted.remove(sid);
      }
    }
  }
}
