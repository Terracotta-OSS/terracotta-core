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

    if (args.length < 2) {
      println("both option and database source are required.");
      printUsage();
      return;
    }

    String opt = args[0];
    File dir = new File(args[1]);
    if (dir.exists()) {
      try {
        SetDbClean cleaner = new SetDbClean(dir, writer, opt);
        cleaner.setDbClean();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      println("invalid database source.");
    }

  }

  public String name() {
    return "Clean DB dirty state";
  }

  public String optionName() {
    return "set-db-clean";
  }

  public String setDBCleanOptions() {
    return "[-s status] [-c clean]";
  }

  public String description() {
    return "This utility get the last staus or clean up passive dirty DB at <database source directory>.";
  }

  public void printUsage() {
    println("\tUsage: " + optionName() + " " + setDBCleanOptions() + " <database source directory>");
    println("\t" + description());
  }
}
