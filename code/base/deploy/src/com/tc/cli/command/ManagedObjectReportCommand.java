/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cli.command;

import com.tc.objectserver.persistence.sleepycat.util.ManagedObjectReport;

import java.io.File;
import java.io.Writer;

public class ManagedObjectReportCommand extends BaseCommand {

  public ManagedObjectReportCommand(Writer writer) {
    super(writer);
  }

  public void execute(String[] args) {

    if (args.length < 1) {
      println("sleepycat database source required.");
      printUsage();
      return;
    }

    File dir = new File(args[0]);
    if (dir.exists()) {
      try {
        ManagedObjectReport managedObjectReport = new ManagedObjectReport(dir, writer);
        managedObjectReport.printReport();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      println("invalid sleepycat database source.");
    }

  }

  public String name() {
    return "Managed Object SleepycatDB Report";
  }

  public String optionName() {
    return "managed-object-report";   
  }

  public String description() {
    return "This utility prints the state of managed objects in the database located at <sleepycat source directory>.";
  }

  public void printUsage() {
    println("\tUsage: " + optionName() + " <sleepycat source directory>");
    println("\t" + description());
  }

}
