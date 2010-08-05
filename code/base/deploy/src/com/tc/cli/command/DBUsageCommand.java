/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cli.command;

import com.tc.objectserver.storage.util.DBUsage;

import java.io.File;
import java.io.Writer;

public class DBUsageCommand extends BaseCommand {

  public DBUsageCommand(Writer writer) {
    super(writer);
  }

  public void execute(String[] args) {

    if (args.length < 1) {
      println("sleepycat database source required.");
      printUsage();
      return;
    }

    File source = new File(args[0]);
    if (source.exists()) {
      try {
        DBUsage dbUsage = new DBUsage(source, writer);
        dbUsage.report();
      } catch (Exception e) {
        e.printStackTrace();
      }

    } else {
      println("invalid sleepycat database source.");
    }
  }

  public String name() {
    return "Sleepycat DB Usage Report";
  }

  public String optionName() {
    return "db-usage";
  }

  public String description() {
    return "This utility prints key statistics of the database located at <sleepycat source directory>.";
  }

  public void printUsage() {
    println("\tUsage: " + optionName() + " <sleepycat source directory>");
    println("\t"+description());
    		
  }

}
