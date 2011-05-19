/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.util;

import com.tc.objectserver.storage.api.DBEnvironment;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class DBUsage extends BaseUtility {
  protected List oidlogsStatsList = new ArrayList();

  public DBUsage(File dir) throws Exception {
    this(dir, new OutputStreamWriter(System.out));
  }

  public DBUsage(File dir, Writer writer) throws Exception {
    super(writer, new File[] { dir });
  }

  public void report() throws Exception {
    if (dbEnvironmentsMap.size() != 1) { throw new AssertionError(
                                                                  "DB Environments created should be 1 only. DB Env in map = "
                                                                      + dbEnvironmentsMap.size()); }
    DBEnvironment env = (DBEnvironment) dbEnvironmentsMap.get(1);
    env.printDatabaseStats(this.writer);
  }

  public static void main(String[] args) {
    if (args == null || args.length < 1) {
      usage();
      System.exit(1);
    }

    try {
      File dir = new File(args[0]);
      validateDir(dir);
      DBUsage usage = new DBUsage(dir);
      usage.report();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(2);
    }
  }

  private static void validateDir(File dir) {
    if (!dir.exists() || !dir.isDirectory()) { throw new RuntimeException("Not a valid directory : " + dir); }
  }

  private static void usage() {
    System.out.println("Usage: DB Usage <environment home directory>");
  }
}
