/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.heap.HeapStorageManager;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.TransactionStore;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.TransactionPersistor;
import com.tc.objectserver.persistence.TransactionPersistorImpl;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
import com.tc.test.TCTestCase;
import com.tc.util.sequence.Sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

public class TransactionStoreTest extends TCTestCase {

  private TransactionPersistor transactionPersistor;
  private Sequence gidSequence;
  private TransactionStore store;

  @Override
  public void setUp() throws ExecutionException, InterruptedException {
    Map<String, KeyValueStorageConfig<?,?>> configMap = new HashMap<String, KeyValueStorageConfig<?, ?>>();
    TransactionPersistorImpl.addConfigsTo(configMap);
    StorageManager storageManager = new HeapStorageManager(configMap);
    storageManager.start().get();
    transactionPersistor = spy(new TransactionPersistorImpl(storageManager));
    gidSequence = spy(new TestMutableSequence());
    store = new TransactionStoreImpl(transactionPersistor, gidSequence);
  }

  public void testDeleteByGlobalTransactionID() throws Exception {
    List<GlobalTransactionDescriptor> gtxs = new LinkedList<GlobalTransactionDescriptor>();
    for (int i = 0; i < 100; i++) {
      ServerTransactionID sid1 = new ServerTransactionID(new ClientID(i), new TransactionID(i));
      store.getOrCreateTransactionDescriptor(sid1);
      store.commitTransactionDescriptor(sid1);
      GlobalTransactionDescriptor desc = store.getTransactionDescriptor(sid1);
      assertNotNull(desc);
      gtxs.add(desc);
    }
    final GlobalTransactionDescriptor originalMin = gtxs.get(0);

    assertEquals(getGlobalTransactionID(originalMin), store.getLeastGlobalTransactionID());

    // create a set of transactions to delete
    Collection<GlobalTransactionDescriptor> toDelete = new HashSet<GlobalTransactionDescriptor>();
    for (int i = 0; i < 10; i++) {

      GlobalTransactionDescriptor o = gtxs.remove(0);
      toDelete.add(o);
    }
    assertFalse(originalMin == gtxs.get(0));

    // delete the set
    store.clearCommitedTransactionsBelowLowWaterMark(getGlobalTransactionID(gtxs.get(0)));

    GlobalTransactionDescriptor currentMin = gtxs.get(0);
    // make sure the min has been adjusted properly
    assertEquals(getGlobalTransactionID(currentMin), store.getLeastGlobalTransactionID());
    // make sure the deleted set has actually been deleted
    Set<GlobalTransactionID> deletedGids = new HashSet<GlobalTransactionID>();
    for (GlobalTransactionDescriptor desc : toDelete) {
      assertNull(store.getTransactionDescriptor(desc.getServerTransactionID()));
      deletedGids.add(desc.getGlobalTransactionID());
    }

    // make sure the persistor is told to delete them all
    verify(transactionPersistor).deleteAllGlobalTransactionDescriptors(new TreeSet<GlobalTransactionID>(deletedGids));
  }

  public void testLeastGlobalTransactionID() throws Exception {
    assertEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());

    ServerTransactionID stx1 = new ServerTransactionID(new ClientID(1), new TransactionID(1));

    GlobalTransactionDescriptor gtx1 = store.getOrCreateTransactionDescriptor(stx1);
    assertNotEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    store.commitTransactionDescriptor(stx1);
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    int min = 10;
    int max = 20;
    List<GlobalTransactionDescriptor> gds = new ArrayList<GlobalTransactionDescriptor>();
    for (int i = min; i < max; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(2), new TransactionID(i));
      GlobalTransactionDescriptor gd = store.getOrCreateTransactionDescriptor(stxid);
      gds.add(gd);
      store.commitTransactionDescriptor(stxid);
    }

    // Still the least Global Txn ID is the same
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    store.clearCommitedTransactionsBelowLowWaterMark((gds.get(0))
        .getServerTransactionID());

    // Still the least Global Txn ID is the same, since CLIENT(1) is still holding the low water mark
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    ServerTransactionID stx2 = new ServerTransactionID(stx1.getSourceID(), stx1.getClientTransactionID().next());
    store.clearCommitedTransactionsBelowLowWaterMark(stx2);

    // least Global Txn ID is not the same
    assertNotEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    GlobalTransactionID currentLWM = store.getLeastGlobalTransactionID();

    // send LWM of the next txn
    store.clearCommitedTransactionsBelowLowWaterMark((gds.get(1))
        .getServerTransactionID());

    // least Global Txn ID is not the same
    assertNotEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertTrue(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());
    currentLWM = store.getLeastGlobalTransactionID();

    // send LWM of the last txn
    store.clearCommitedTransactionsBelowLowWaterMark((gds.get(gds.size() - 1))
        .getServerTransactionID());

    // least Global Txn ID is not the same
    assertNotEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertTrue(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());
    currentLWM = store.getLeastGlobalTransactionID();

    // send LWM above the last txn
    ServerTransactionID sid = (gds.get(gds.size() - 1)).getServerTransactionID();
    sid = new ServerTransactionID(sid.getSourceID(), sid.getClientTransactionID().next());
    store.clearCommitedTransactionsBelowLowWaterMark(sid);

    // least Global Txn ID is not the same, its null
    assertNotEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());
  }

  public void testLeastGlobalTransactionIDInPassiveServer() throws Exception {
    assertEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());

    ServerTransactionID stx1 = new ServerTransactionID(new ClientID(1), new TransactionID(1));

    GlobalTransactionDescriptor gtx1 = store.getOrCreateTransactionDescriptor(stx1);
    assertNotEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    store.commitTransactionDescriptor(stx1);
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    int min = 10;
    int max = 20;
    List<GlobalTransactionDescriptor> gds = new ArrayList<GlobalTransactionDescriptor>();
    for (int i = min; i < max; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(2), new TransactionID(i));
      GlobalTransactionDescriptor gd = store.getOrCreateTransactionDescriptor(stxid);
      gds.add(gd);
    }

    // Still the least Global Txn ID is the same
    assertEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    // No need to send LWM per client in the PASSIVE, Client(1)'s txn is cleared
    store.clearCommitedTransactionsBelowLowWaterMark((gds.get(0))
        .getGlobalTransactionID());

    // least Global Txn ID is not the same
    assertNotEquals(getGlobalTransactionID(gtx1), store.getLeastGlobalTransactionID());

    GlobalTransactionID currentLWM = store.getLeastGlobalTransactionID();

    // send LWM of the next txn
    store.clearCommitedTransactionsBelowLowWaterMark((gds.get(1))
        .getGlobalTransactionID());

    // least Global Txn ID is STILL the same, since the transactions are not commited.
    assertEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertFalse(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());

    // commit transaction
    store.commitTransactionDescriptor((gds.get(0)).getServerTransactionID());

    // least Global Txn ID is STILL the same, only when the next LWM msg comes along it clears the data structures.
    assertEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertFalse(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());

    // send LWM again
    store.clearCommitedTransactionsBelowLowWaterMark((gds.get(1))
        .getGlobalTransactionID());

    // Now LWM should have moved up
    assertNotEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertTrue(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());
    currentLWM = store.getLeastGlobalTransactionID();

    // send LWM of the last txn
    store.clearCommitedTransactionsBelowLowWaterMark((gds.get(gds.size() - 1))
        .getGlobalTransactionID());

    // least Global Txn ID is STILL the same, since the transactions are not commited.
    assertEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertFalse(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());

    for (int i = 1; i < gds.size(); i++) {
      GlobalTransactionDescriptor gd = gds.get(i);
      store.commitTransactionDescriptor(gd.getServerTransactionID());
    }

    // least Global Txn ID is STILL the same, only when the next LWM msg comes along it clears the data structures.
    assertEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertFalse(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());

    // send LWM again
    store.clearCommitedTransactionsBelowLowWaterMark((gds.get(gds.size() - 1))
        .getGlobalTransactionID());

    // least Global Txn ID is not the same
    assertNotEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertTrue(currentLWM.toLong() < store.getLeastGlobalTransactionID().toLong());
    currentLWM = store.getLeastGlobalTransactionID();

    // send LWM above the last txn - we use SID instead of GID here, not a real case in passive
    ServerTransactionID sid = (gds.get(gds.size() - 1)).getServerTransactionID();
    sid = new ServerTransactionID(sid.getSourceID(), sid.getClientTransactionID().next());
    store.clearCommitedTransactionsBelowLowWaterMark(sid);

    // least Global Txn ID is not the same, its null
    assertNotEquals(currentLWM, store.getLeastGlobalTransactionID());
    assertEquals(GlobalTransactionID.NULL_ID, store.getLeastGlobalTransactionID());
  }

  private GlobalTransactionID getGlobalTransactionID(GlobalTransactionDescriptor gtx) {
    return gtx.getGlobalTransactionID();
  }

  public void testClientShutdown() throws Exception {
    int initialMin = 200;
    int initialMax = 300;
    int laterMax = 400;
    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i % 2), new TransactionID(i));
      GlobalTransactionDescriptor gtx = new GlobalTransactionDescriptor(stxid,
                                                                        new GlobalTransactionID(gidSequence.next()));
      transactionPersistor.saveGlobalTransactionDescriptor(gtx);
    }
    store = new TransactionStoreImpl(transactionPersistor, gidSequence);
    GlobalTransactionID lowmk1 = store.getLeastGlobalTransactionID();

    // create more
    for (int i = initialMax; i < laterMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i % 2), new TransactionID(i));
      store.getOrCreateTransactionDescriptor(stxid);
      store.commitTransactionDescriptor(stxid);
    }
    GlobalTransactionID lowmk2 = store.getLeastGlobalTransactionID();

    assertEquals(lowmk1, lowmk2);

    ClientID cid0 = new ClientID(0);
    store.shutdownNode(cid0);

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
    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i), new TransactionID(i));
      GlobalTransactionDescriptor gtx = new GlobalTransactionDescriptor(stxid,
                                                                        new GlobalTransactionID(gidSequence.next()));
      transactionPersistor.saveGlobalTransactionDescriptor(gtx);
    }
    store = new TransactionStoreImpl(transactionPersistor, gidSequence);

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

    store.commitTransactionDescriptor(stxid1);
    verify(transactionPersistor).saveGlobalTransactionDescriptor(gtx1);

    store.commitTransactionDescriptor(stxid2);
    verify(transactionPersistor).saveGlobalTransactionDescriptor(gtx2);
  }

  public void testSameGIDAssignedOnRestart() throws Exception {
    int initialMin = 200;
    int initialMax = 300;
    int laterMax = 400;
    Map<ServerTransactionID, GlobalTransactionDescriptor> sid2Gid = new HashMap<ServerTransactionID, GlobalTransactionDescriptor>();
    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i % 2), new TransactionID(i));
      GlobalTransactionDescriptor desc = store.getOrCreateTransactionDescriptor(stxid);
      // We're using an in-memory map of transactions, so we need to avoid marking the global transaction descriptors as
      // committed up front.
      transactionPersistor.saveGlobalTransactionDescriptor(desc);
      assertEquals(stxid, desc.getServerTransactionID());
      sid2Gid.put(stxid, desc);
    }

    // RESTART
    store = new TransactionStoreImpl(transactionPersistor, gidSequence);

    // test if we get the same gid
    GlobalTransactionID maxID = GlobalTransactionID.NULL_ID;
    for (int i = initialMin; i < initialMax; i++) {
      ServerTransactionID stxid = new ServerTransactionID(new ClientID(i % 2), new TransactionID(i));
      GlobalTransactionDescriptor desc = sid2Gid.get(stxid);
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
      store.commitTransactionDescriptor(stxid);
      assertTrue(maxID.toLong() < desc.getGlobalTransactionID().toLong());
    }
  }
}
