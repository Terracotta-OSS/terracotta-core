/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.EnvironmentConfig;
import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBPersistenceTransactionProvider;
import com.tc.test.TCTestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class TransactionPersistorTest extends TCTestCase {

  private File              envHome;
  private EnvironmentConfig ecfg;
  private DatabaseConfig    dbcfg;
  private static int        count = 0;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ecfg = new EnvironmentConfig();
    ecfg.setAllowCreate(true);
    ecfg.setReadOnly(false);
    ecfg.setTransactional(true);

    dbcfg = new DatabaseConfig();
    dbcfg.setAllowCreate(true);
    dbcfg.setReadOnly(false);
    dbcfg.setTransactional(true);
    while ((envHome = new File(this.getTempDirectory(), ++count + "")).exists()) {
      //
    }
    System.out.println("DB home: " + envHome);
  }

  private BerkeleyDBEnvironment newEnv(boolean paranoid) throws IOException {
    return newEnv(new HashMap(), new ArrayList(), paranoid);
  }

  private BerkeleyDBEnvironment newEnv(Map map, List list, boolean paranoid) throws IOException {
    return new BerkeleyDBEnvironment(map, list, paranoid, envHome, ecfg, dbcfg);
  }

  /**
   * This test was written just so that I can understand sleepycat behavior under various conditions. Things that I
   * noticed are 1) If save happens before delete, delete doesnt go thru until save is committed. (record level lock) 2)
   * If commit doesnt happen with the default(500ms) time sleepcat throws a deadlock exception.
   */
  public void testSimultaneousSaveAndDeletes() throws Exception {
    BerkeleyDBEnvironment env = newEnv(true);
    assertTrue(env.open());
    final BerkeleyDBPersistenceTransactionProvider persistenceTransactionProvider = new BerkeleyDBPersistenceTransactionProvider(
                                                                                                                                 env
                                                                                                                                     .getEnvironment());
    final TransactionPersistorImpl tpl = new TransactionPersistorImpl(env.getTransactionDatabase(),
                                                                      persistenceTransactionProvider);
    final ServerTransactionID sid = new ServerTransactionID(new ClientID(9), new TransactionID(10));
    final GlobalTransactionDescriptor gtd = new GlobalTransactionDescriptor(sid, new GlobalTransactionID(909));
    final CyclicBarrier cb = new CyclicBarrier(2);
    final Exception ex[] = new Exception[2];

    Thread t1 = new Thread() {
      @Override
      public void run() {
        try {
          Collection gdts = tpl.loadAllGlobalTransactionDescriptors();
          assertEquals(0, gdts.size());
          PersistenceTransaction pt = persistenceTransactionProvider.newTransaction();
          tpl.saveGlobalTransactionDescriptor(pt, gtd);
          System.err.println("T1 : save done.");
          cb.barrier();
          pt.commit();
          System.err.println("T1 : save commit done.");
          gdts = tpl.loadAllGlobalTransactionDescriptors();
          assertEquals(0, gdts.size());
        } catch (Exception e) {
          e.printStackTrace();
          ex[0] = e;
        }
      }
    };

    Thread t2 = new Thread() {
      @Override
      public void run() {
        try {
          PersistenceTransaction pt = persistenceTransactionProvider.newTransaction();
          SortedSet ss = new TreeSet();
          ss.add(gtd.getGlobalTransactionID());
          cb.barrier();
          tpl.deleteAllGlobalTransactionDescriptors(pt, ss);
          System.err.println("T2 : Delete done");
          pt.commit();
          System.err.println("T2 : Delete commit done");
        } catch (Exception e) {
          e.printStackTrace();
          ex[1] = e;
        }
      }
    };

    t1.start();
    t2.start();

    t1.join();
    t2.join();
    if (ex[0] != null) throw ex[0];
    if (ex[1] != null) throw ex[1];
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

}
