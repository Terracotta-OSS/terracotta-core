/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cli.command;

import com.tc.objectserver.storage.util.FastLoadOidlogAnalysis;

import java.io.File;
import java.io.Writer;

public class FastLoadOidLogAnalysisCommand extends BaseCommand {

  public FastLoadOidLogAnalysisCommand(Writer writer) {
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
        FastLoadOidlogAnalysis fastLoadOidlogAnalysis = new FastLoadOidlogAnalysis(dir, writer);
        fastLoadOidlogAnalysis.report();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      println("invalid sleepycat database source.");
    }

  }

  public String name() {
    return "FastLoad Oid Log Analysis Report";
  }

  public String optionName() {
    return "fastload-oid-log";
  }

  public String description() {
    return "This utility prints key statistics of the oidlog database located at <sleepycat source directory>.";
  }

  public void printUsage() {
    println("\tUsage: " + optionName() + " <sleepycat source directory>");
    println("\t" +description());
  }
}
