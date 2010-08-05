/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cli.command;

import com.tc.objectserver.storage.util.SetDbClean;

import java.io.File;
import java.io.Writer;

public class SetDbCleanCommand extends BaseCommand {

  public SetDbCleanCommand(Writer writer) {
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
        SetDbClean cleaner = new SetDbClean(dir, writer);
        cleaner.setDbClean();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      println("invalid sleepycat database source.");
    }

  }

  public String name() {
    return "Clean DB dirty state";
  }

  public String optionName() {
    return "set-db-clean";
  }

  public String description() {
    return "This utility clean up passive dirty DB at <sleepycat source directory>.";
  }

  public void printUsage() {
    println("\tUsage: " + optionName() + " <sleepycat source directory>");
    println("\t" +description());
  }
}
