/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cli.command;

import com.tc.object.ObjectID;
import com.tc.objectserver.storage.util.ManagedObjectReport;

import java.io.File;
import java.io.Writer;

public class ManagedObjectReportCommand extends BaseCommand {
  private static final String LIST_ALL        = "list-all-objects";
  private static final String SHOW_OBJECT     = "show-object";
  private static final int    UNKNOWN_CMD     = 0;
  private static final int    LIST_ALL_CMD    = 1;
  private static final int    SHOW_OBJECT_CMD = 2;

  public ManagedObjectReportCommand(Writer writer) {
    super(writer);
  }

  public void execute(String[] args) {

    if (args.length < 1) {
      println("database source required.");
      printUsage();
      return;
    }

    File dir = new File(args[0]);
    if (dir.exists()) {
      try {
        ManagedObjectReport managedObjectReport = new ManagedObjectReport(dir, writer);
        if (args.length == 1) {
          managedObjectReport.report();
          managedObjectReport.printReport();
        } else if (args.length >= 2) {
          switch (subcmd(args[1])) {
            case LIST_ALL_CMD:
              managedObjectReport.listAllObjectIDs();
              break;
            case SHOW_OBJECT_CMD:
              if (args.length >= 3) {
                managedObjectReport.listSpecificObjectByID(new ObjectID(Long.valueOf(args[2])));
              } else {
                println("missing object-ID");
              }
              break;
            default:
              println("invalid sub-command " + args[1]);
              break;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      println("invalid database source.");
    }

  }

  private final int subcmd(String cmd) {
    if (cmd.equalsIgnoreCase(LIST_ALL)) return LIST_ALL_CMD;
    if (cmd.equalsIgnoreCase(SHOW_OBJECT)) return SHOW_OBJECT_CMD;
    return UNKNOWN_CMD;
  }

  public String name() {
    return "Managed Object DB Report";
  }

  public String optionName() {
    return "managed-object-report";
  }

  public String description() {
    return "This utility prints the state of managed objects in the database located at <database source directory>.";
  }

  public void printUsage() {
    println("\tUsage: " + optionName() + " <database source directory> [[" + LIST_ALL + "]|[" + SHOW_OBJECT
            + " <objectID>]]");
    println("\t" + description());
  }

}
