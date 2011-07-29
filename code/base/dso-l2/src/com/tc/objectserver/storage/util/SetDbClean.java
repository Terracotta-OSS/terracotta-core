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
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.objectserver.storage.api.TCStringToStringDatabase;
import com.tc.util.State;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class SetDbClean extends BaseUtility {
  protected List        oidlogsStatsList = new ArrayList();
  private final OPTIONS option;

  private enum OPTIONS {
    S, C
  }

  public SetDbClean(File dir, String opt) throws Exception {
    this(dir, new OutputStreamWriter(System.out), opt);
  }

  public SetDbClean(File dir, Writer writer, String opt) throws Exception {
    super(writer, new File[] { dir });
    option = validateOption(opt);
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
    switch (option) {
      case S:
        log("This server last staus was " + stateStr);
        break;
      case C:
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
        break;
      default:
        break;
    }

    getPersistor(1).close();
  }

  public static void main(String[] args) {
    if (args == null || args.length < 2) {
      usage();
      System.exit(1);
    }

    try {
      String opt = args[0];
      File dir = new File(args[1]);
      validateDir(dir);
      SetDbClean cleaner = new SetDbClean(dir, opt);
      cleaner.setDbClean();
    } catch (Exception e) {
      e.printStackTrace();
      usage();
      System.exit(2);
    }
  }

  private OPTIONS validateOption(String opt) {
    if (opt == null || (opt.length() != 2) || !opt.startsWith("-")) { throw new RuntimeException("invalid option \""
                                                                                                 + opt + "\""); }
    OPTIONS opts;
    try {
      opts = OPTIONS.valueOf(opt.substring(1).toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("invalid option \"" + opt + "\"");
    }
    return opts;
  }

  private static void validateDir(File dir) {
    if (!dir.exists() || !dir.isDirectory()) { throw new RuntimeException("Not a valid directory : " + dir); }
  }

  private static void usage() {
    System.out.println("Usage: SetDbClean [-s status] [-c clean] <environment home directory>");
  }

}
