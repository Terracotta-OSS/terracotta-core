/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

public class TransactionStoreTest extends TCTestCase {

  private TestTransactionPersistor persistor;
  private TransactionStore         store;

  public void testDeleteByGlobalTransactionID() throws Exception {
    persistor = new TestTransactionPersistor();
    store = new TransactionStoreImpl(persistor, persistor);
    List gtxs = new LinkedList();
    for (int i = 0; i < 100; i++) {
      ServerTransactionID sid1 = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      GlobalTransactionDescriptor desc = store.getOrCreateTransactionDescriptor(sid1);
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

  public void testLeastGlobalTransactionID() throws Exception {

    persistor = new TestTransactionPersistor();
    store = new TransactionStoreImpl(persistor, persistor);

    assertEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());

    ServerTransactionID stx1 = new ServerTransactionID(new ChannelID(1), new TransactionID(1));

    GlobalTransactionDescriptor gtx1 = store.getOrCreateTransactionDescriptor(stx1);
    assertNotEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    store.commitTransactionDescriptor(null, gtx1);
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    int min = 100;
    int max = 200;
    for (int i = min; i < max; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      GlobalTransactionDescriptor gtxi = store.getOrCreateTransactionDescriptor(stxid);
      store.commitTransactionDescriptor(null, gtxi);
    }

    // Still the least Global Txn ID is the same
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    // now remove some from the the middle
    Set toDelete = new HashSet();
    for (int i = min + 25; i < max - 50; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      toDelete.add(stxid);
    }
    store.removeAllByServerTransactionID(null, toDelete);

    // Still the least Global Txn ID is the same
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    // now remove some from the beginning
    toDelete.clear();
    for (int i = min; i < min + 25; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      toDelete.add(stxid);
    }
    toDelete.add(stx1);
    store.removeAllByServerTransactionID(null, toDelete);

    // the least Global Txn ID is not the same
    assertNotEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());
    assertTrue(getGlobalTransactionID(gtx1).toLong() < store.getLeastGlobalTransactionID().toLong());
    
    GlobalTransactionID least1 = store.getLeastGlobalTransactionID();

    // RESTART scenario
    store = new TransactionStoreImpl(persistor, persistor);

    GlobalTransactionID least2 = store.getLeastGlobalTransactionID();
    assertEquals(least1, least2); 
    
    // now remove some from the the middle
    toDelete.clear();
    for (int i = min + 75; i < max ; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      toDelete.add(stxid);
    }
    store.removeAllByServerTransactionID(null, toDelete);

    // Still the least Global Txn ID is the same
    assertEquals(least1, store.getLeastGlobalTransactionID());

    // now remove each of the remaining ones
    for (int i = min + 50; i < max - 25; i++) {
      toDelete.clear();
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      toDelete.add(stxid);
      store.removeAllByServerTransactionID(null, toDelete);
    }

    assertNotEquals(least1, store.getLeastGlobalTransactionID());
    least2 = store.getLeastGlobalTransactionID();
    assertTrue(least2.isNull());
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
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i % 2), new TransactionID(i));
      persistor.persisted.put(stxid, new GlobalTransactionDescriptor(stxid, new GlobalTransactionID(persistor.next())));
    }
    store = new TransactionStoreImpl(persistor, persistor);
    GlobalTransactionID lowmk1 = store.getLeastGlobalTransactionID();

    // create more
    for (int i = initialMax; i < laterMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i % 2), new TransactionID(i));
      store.getGlobalTransactionID(stxid);
      store.commitTransactionDescriptor(null, store.getOrCreateTransactionDescriptor(stxid));
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
    int initialMin = 200;
    int initialMax = 300;
    persistor = new TestTransactionPersistor();
    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i), new TransactionID(i));
      persistor.persisted.put(stxid, new GlobalTransactionDescriptor(stxid, new GlobalTransactionID(persistor.next())));
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
    assertEquals(GlobalTransactionID.NULL_ID, store.getGlobalTransactionID(stxid1));
    GlobalTransactionDescriptor gtx1 = store.getOrCreateTransactionDescriptor(stxid1);
    assertEquals(gtx1, store.getTransactionDescriptor(stxid1));

    assertSame(gtx1, store.getTransactionDescriptor(stxid1));

    assertNull(store.getTransactionDescriptor(stxid2));
    GlobalTransactionDescriptor gtx2 = store.getOrCreateTransactionDescriptor(stxid2);
    store.getGlobalTransactionID(stxid2);
    assertEquals(gtx2, store.getTransactionDescriptor(stxid2));

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
  
  public void testSameGIDAssignedOnRestart() throws Exception {
    int initialMin = 200;
    int initialMax = 300;
    int laterMax = 400;
    persistor = new TestTransactionPersistor();
    store = new TransactionStoreImpl(persistor, persistor);
    Map sid2Gid = new HashMap();
    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i % 2), new TransactionID(i));
      GlobalTransactionDescriptor desc = store.getOrCreateTransactionDescriptor(stxid);
      store.commitTransactionDescriptor(null, desc);
      assertEquals(stxid, desc.getServerTransactionID());
      sid2Gid.put(stxid, desc);
    }

    //RESTART
    store = new TransactionStoreImpl(persistor, persistor);
    
    //test if we get the same gid
    GlobalTransactionID maxID = GlobalTransactionID.NULL_ID;
    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i % 2), new TransactionID(i));
      GlobalTransactionDescriptor desc = (GlobalTransactionDescriptor) sid2Gid.get(stxid);
      assertEquals(desc, store.getTransactionDescriptor(stxid));
      assertEquals(desc.getGlobalTransactionID(), store.getGlobalTransactionID(stxid));
      if(desc.getGlobalTransactionID().toLong() > maxID.toLong()) {
        maxID = desc.getGlobalTransactionID();
      }
    }
    
    // create more
    for (int i = initialMax; i < laterMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ChannelID(i % 2), new TransactionID(i));
      GlobalTransactionDescriptor desc;
      store.commitTransactionDescriptor(null, desc = store.getOrCreateTransactionDescriptor(stxid));
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

    public void deleteAllByServerTransactionID(PersistenceTransaction tx, Collection toDelete) {
      deleteQueue.put(toDelete);
      for (Iterator i = toDelete.iterator(); i.hasNext();) {
        persisted.remove(i.next());
      }
    }
  }
}
