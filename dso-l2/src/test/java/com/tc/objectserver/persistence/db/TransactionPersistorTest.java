/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.DBFactory;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.test.TCTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class TransactionPersistorTest extends TCTestCase {

  private DBEnvironment                  dbEnv;
  private PersistenceTransactionProvider ptp;
  private TransactionPersistor           transactionPersistor;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    File envHome = null;
    int count = 0;
    while ((envHome = new File(this.getTempDirectory(), ++count + "")).exists()) {
      //
    }
    dbEnv = DBFactory.getInstance().createEnvironment(true, envHome);
    assertTrue(dbEnv.open());
    ptp = dbEnv.getPersistenceTransactionProvider();
    transactionPersistor = new TransactionPersistorImpl(dbEnv.getTransactionDatabase(), ptp);
  }

  public void testSave() throws Exception {
    assertEquals(0, transactionPersistor.loadAllGlobalTransactionDescriptors().size());

    Set<GlobalTransactionDescriptor> expectedGTDs = new HashSet<GlobalTransactionDescriptor>();
    PersistenceTransaction tx = ptp.newTransaction();
    for (int i = 0; i < 100; i++) {
      GlobalTransactionDescriptor gtd = createGlobalTransactionDescriptor(0, i, i);
      transactionPersistor.saveGlobalTransactionDescriptor(tx, gtd);
      expectedGTDs.add(gtd);
    }
    tx.commit();

    Collection<GlobalTransactionDescriptor> gtds = transactionPersistor.loadAllGlobalTransactionDescriptors();
    assertEquals(expectedGTDs.size(), gtds.size());
    assertTrue(gtds.containsAll(expectedGTDs));
  }

  public void testDelete() throws Exception {
    assertEquals(0, transactionPersistor.loadAllGlobalTransactionDescriptors().size());

    List<GlobalTransactionDescriptor> expectedGTDs = new ArrayList<GlobalTransactionDescriptor>();
    PersistenceTransaction tx = ptp.newTransaction();
    for (int i = 0; i < 100; i++) {
      GlobalTransactionDescriptor gtd = createGlobalTransactionDescriptor(0, i, i);
      transactionPersistor.saveGlobalTransactionDescriptor(tx, gtd);
      expectedGTDs.add(gtd);
    }
    tx.commit();

    int toDelete = 20;
    SortedSet<GlobalTransactionID> gtdsToDelete = new TreeSet<GlobalTransactionID>();
    for (int i = 0; i < toDelete; i++) {
      gtdsToDelete.add(expectedGTDs.remove(0).getGlobalTransactionID());
    }
    tx = ptp.newTransaction();
    transactionPersistor.deleteAllGlobalTransactionDescriptors(tx, gtdsToDelete);
    tx.commit();

    Collection<GlobalTransactionDescriptor> gtds = transactionPersistor.loadAllGlobalTransactionDescriptors();
    assertEquals(expectedGTDs.size(), gtds.size());
    assertTrue(gtds.containsAll(expectedGTDs));

    SortedSet<GlobalTransactionID> remainingGTDs = new TreeSet<GlobalTransactionID>();
    for (GlobalTransactionDescriptor gtd : expectedGTDs) {
      remainingGTDs.add(gtd.getGlobalTransactionID());
    }
    tx = ptp.newTransaction();
    transactionPersistor.deleteAllGlobalTransactionDescriptors(tx, remainingGTDs);
    tx.commit();
    assertEquals(0, transactionPersistor.loadAllGlobalTransactionDescriptors().size());
  }

  private GlobalTransactionDescriptor createGlobalTransactionDescriptor(long clientId, long transactionId,
                                                                        long globalTransactionId) {
    return new GlobalTransactionDescriptor(new ServerTransactionID(new ClientID(clientId),
                                                                   new TransactionID(transactionId)),
                                           new GlobalTransactionID(globalTransactionId));
  }

  @Override
  protected void tearDown() throws Exception {
    dbEnv.close();
    super.tearDown();
  }
}
