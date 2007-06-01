/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.EnvironmentConfig;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.test.TCTestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionPersistorTest extends TCTestCase {

  private File              envHome;
  private EnvironmentConfig ecfg;
  private DatabaseConfig    dbcfg;
  private static int        count = 0;

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

  private DBEnvironment newEnv(boolean paranoid) throws IOException {
    return newEnv(new HashMap(), new ArrayList(), paranoid);
  }

  private DBEnvironment newEnv(Map map, List list, boolean paranoid) throws IOException {
    return new DBEnvironment(map, list, paranoid, envHome, ecfg, dbcfg);
  }

  /**
   * This test was written just so that I can understand sleepycat behavior under various conditions. Things that I noticed are 
   * 1) If save happens before delete, delete doesnt go thru until save is committed. (record level lock)
   * 2) If commit doesnt happen with the default(500ms) time sleepcat throws a deadlock exception.
   */
  public void testSimultaneousSaveAndDeletes() throws Exception {
    DBEnvironment env = newEnv(true);
    assertTrue(env.open().isClean());
    final SleepycatPersistenceTransactionProvider persistenceTransactionProvider = new SleepycatPersistenceTransactionProvider(
                                                                                                                               env
                                                                                                                                   .getEnvironment());
    final TransactionPersistorImpl tpl = new TransactionPersistorImpl(env.getTransactionDatabase(),
                                                                      persistenceTransactionProvider);
    final ServerTransactionID sid = new ServerTransactionID(new ChannelID(9), new TransactionID(10));
    final GlobalTransactionDescriptor gtd = new GlobalTransactionDescriptor(sid, new GlobalTransactionID(909));
    final CyclicBarrier cb = new CyclicBarrier(2);

    Thread t1 = new Thread() {
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
        }
      }
    };

    Thread t2 = new Thread() {
      public void run() {
        try {
          PersistenceTransaction pt = persistenceTransactionProvider.newTransaction();
          ArrayList al = new ArrayList();
          al.add(sid);
          cb.barrier();
          tpl.deleteAllByServerTransactionID(pt, al);
          System.err.println("T2 : Delete done");
          pt.commit();
          System.err.println("T2 : Delete commit done");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    
    t1.start();
    t2.start();
    
    t1.join();
    t2.join();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

}
