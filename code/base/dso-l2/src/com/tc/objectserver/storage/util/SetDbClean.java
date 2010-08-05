/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.util;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.l2.ha.ClusterState;
import com.tc.l2.state.StateManager;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.util.Conversion;
import com.tc.util.State;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class SetDbClean extends BaseUtility {
  private final EnvironmentConfig enc;
  private final Environment       env;
  private final DatabaseConfig    dbc;
  protected List                  oidlogsStatsList = new ArrayList();

  public SetDbClean(File dir) throws Exception {
    this(dir, new OutputStreamWriter(System.out));
  }

  public SetDbClean(File dir, Writer writer) throws Exception {
    super(writer, new File[] {});
    this.enc = new EnvironmentConfig();
    this.enc.setReadOnly(false);
    this.env = new Environment(dir, enc);
    this.dbc = new DatabaseConfig();
    this.dbc.setReadOnly(false);
  }

  public void setDbClean() throws DatabaseException {
    Database db = null;
    try {
      db = env.openDatabase(null, BerkeleyDBEnvironment.getClusterStateStoreName(), dbc);
    } catch (DatabaseException e) {
      log("Probably not running in persistent mode!");
      env.close();
      throw e;
    }

    DatabaseEntry dkey = new DatabaseEntry();
    dkey.setData(Conversion.string2Bytes(ClusterState.getL2StateKey()));
    DatabaseEntry dvalue = new DatabaseEntry();
    OperationStatus status = db.get(null, dkey, dvalue, LockMode.DEFAULT);
    if (!OperationStatus.SUCCESS.equals(status)) {
      log("Failed to read state!");
      db.close();
      env.close();
      return;
    }

    String stateStr = Conversion.bytes2String(dvalue.getData());
    if (stateStr == null) {
      log("Failed to set DB clean for empty state");
      db.close();
      env.close();
      return;
    }

    State state = new State(stateStr);
    if (!StateManager.PASSIVE_STANDBY.equals(state)) {
      log("Failed to set DB clean for " + state);
      db.close();
      env.close();
      return;
    }
    
    dvalue.setData(Conversion.string2Bytes(StateManager.ACTIVE_COORDINATOR.getName()));
    status = db.put(null, dkey, dvalue);
    if (OperationStatus.SUCCESS.equals(status)) {
      log("SetDbClean success!");
    } else {
      log("Failed to setDbClean");
    }
    
    db.close();
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
