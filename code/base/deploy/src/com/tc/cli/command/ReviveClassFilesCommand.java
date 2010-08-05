/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cli.command;

import com.tc.objectserver.storage.util.ReviveClassFiles;

import java.io.File;
import java.io.Writer;

public class ReviveClassFilesCommand extends BaseCommand {

  public ReviveClassFilesCommand(Writer writer) {
    super(writer);
  }

  public void execute(String[] args) {

    if (args.length < 2) {
      println("sleepycat database source required, and destination directory required.");
      printUsage();
      return;
    }

    File source = new File(args[0]);
    File dest = new File(args[1]);
    if (source.exists() && dest.exists()) {
      try {
        ReviveClassFiles reviveClassFiles = new ReviveClassFiles(source, dest, writer);
        reviveClassFiles.reviveClassesFiles();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      println("invalid sleepycat database source and/or destination directory.");
    }

  }

  public String name() {
    return "Revive Class Files Report";
  }

  public String optionName() {
    return "revive-class-files";
  }

  public String description() {
    return "This utility revives class files found a the database located <sleepycat source directory>, " +
    		"and places those files at the <destination directory>.";

  }

  public void printUsage() {
    println("\tUsage: " + optionName() + " <sleepycat source directory> <destination directory>");
    println("\t" + description());
  }

}
