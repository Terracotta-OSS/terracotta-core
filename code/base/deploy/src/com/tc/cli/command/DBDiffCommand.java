/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cli.command;

import com.tc.objectserver.storage.util.DBDiff;

import java.io.File;
import java.io.Writer;

public class DBDiffCommand extends BaseCommand {

  public DBDiffCommand(Writer writer) {
    super(writer);
  }

  public void execute(String[] args) {

    if (args.length < 2) {
      println("database sources (2) required.");
      printUsage();
      return;
    }

    File db1 = new File(args[0]);
    File db2 = new File(args[1]);

    if (db1.equals(db2)) {
      println("database source 1 and 2 cannot be the same database");
    }
    if (db1.exists() && db2.exists()) {
      try {
        DBDiff dbDiff = new DBDiff(db1, db2, false, writer);
        dbDiff.diff();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      println("invalid database sources.");
    }

  }

  public String name() {
    return "DB Diff Report";
  }

  public String optionName() {
    return "db-diff";
  }

  public String description() {
    return "This utility print a diff report on the databases <database source directory 1> <database source directory 2>."
           + "The report consists of differences in generated classes, transactions, client states and managed objects.";
  }

  public void printUsage() {
    println("\tUsage: " + optionName() + " <database source directory 1> <database source directory 2>");
    println("\t" + description());
  }

}
