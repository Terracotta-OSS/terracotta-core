/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.util;

import com.tc.l2.ha.ClusterState;
import com.tc.l2.state.StateManager;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCStringToStringDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.State;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class SetDbClean extends BaseUtility {
  protected List oidlogsStatsList = new ArrayList();

  public SetDbClean(File dir) throws Exception {
    this(dir, new OutputStreamWriter(System.out));
  }

  public SetDbClean(File dir, Writer writer) throws Exception {
    super(writer, new File[] { dir });
  }

  public void setDbClean() throws Exception {
    if (dbEnvironmentsMap.size() != 1) { throw new AssertionError(
                                                                  "DB Environments created should be 1 only. DB Env in map = "
                                                                      + dbEnvironmentsMap.size()); }
    DBEnvironment env = (DBEnvironment) dbEnvironmentsMap.get(1);
    PersistenceTransactionProvider ptp = env.getPersistenceTransactionProvider();

    TCStringToStringDatabase db = env.getClusterStateStoreDatabase();
    TCDatabaseEntry<String, String> entry = new TCDatabaseEntry<String, String>();
    entry.setKey(ClusterState.getL2StateKey());

    PersistenceTransaction tx = ptp.newTransaction();
    Status status = db.get(entry, tx);

    if (!Status.SUCCESS.equals(status)) {
      log("Failed to read state!");
      tx.commit();
      env.close();
      return;
    }
    tx.commit();

    String stateStr = entry.getValue();
    if (stateStr == null) {
      log("Failed to set DB clean for empty state");
      env.close();
      return;
    }

    State state = new State(stateStr);
    if (!StateManager.PASSIVE_STANDBY.equals(state)) {
      log("Failed to set DB clean for " + state);
      env.close();
      return;
    }

    tx = ptp.newTransaction();
    status = db.put(ClusterState.getL2StateKey(), StateManager.ACTIVE_COORDINATOR.getName(), tx);
    if (Status.SUCCESS.equals(status)) {
      log("SetDbClean success!");
    } else {
      log("Failed to setDbClean");
    }
    tx.commit();
    env.close();
  }

  public static void main(String[] args) {
    if (args == null || args.length < 1) {
      usage();
      System.exit(1);
    }

    try {
      File dir = new File(args[0]);
      validateDir(dir);
      SetDbClean cleaner = new SetDbClean(dir);
      cleaner.setDbClean();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(2);
    }
  }

  private static void validateDir(File dir) {
    if (!dir.exists() || !dir.isDirectory()) { throw new RuntimeException("Not a valid directory : " + dir); }
  }

  private static void usage() {
    System.out.println("Usage: SetDbClean <environment home directory>");
  }

}
