/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.cli;

import com.tc.cli.command.Command;

import java.io.StringWriter;

import junit.framework.TestCase;

public class CommandLineMainTest extends TestCase {

  public void testCommandsNull() {
    StringWriter sw = new StringWriter();
    CommandLineMain clm = new CommandLineMain(sw);
    clm.executeCommand("", null);
    assertEquals("not a valid command: \n", sw.toString());

  }

//  public void testManagedObjectReportCommandNoArgs() throws Exception {
//
//    StringWriter sw = new StringWriter();
//    CommandLineMain clm = new CommandLineMain(sw);
//    ManagedObjectReportCommand command = new ManagedObjectReportCommand(sw);
//    clm.executeCommand(command.optionName(), new String[0]);
//    String assertString = "database source required.\n"
//                          + "\tUsage: managed-object-report <database source directory> [[list-all-objects]|[show-object <objectID>]]\n"
//                          + "\tThis utility prints the state of managed objects in the database located at <database source directory>.\n";
//    assertEquals(assertString, sw.toString());
//  }
//
//  public void testFastLoadOidLogAnalysisCommandNoArgs() throws Exception {
//
//    StringWriter sw = new StringWriter();
//    CommandLineMain clm = new CommandLineMain(sw);
//    FastLoadOidLogAnalysisCommand command = new FastLoadOidLogAnalysisCommand(sw);
//    clm.executeCommand(command.optionName(), new String[0]);
//    String assertString = "sleepycat database source required.\n"
//                          + "\tUsage: fastload-oid-log <sleepycat source directory>\n"
//                          + "\tThis utility prints key statistics of the oidlog database located at <sleepycat source directory>.\n";
//    assertEquals(assertString, sw.toString());
//  }
//
//  public void testReviveClassFilesCommandNoArgs() throws Exception {
//
//    StringWriter sw = new StringWriter();
//    CommandLineMain clm = new CommandLineMain(sw);
//    ReviveClassFilesCommand command = new ReviveClassFilesCommand(sw);
//    clm.executeCommand(command.optionName(), new String[0]);
//
//    String assertString = "database source required, and destination directory required.\n"
//                          + "\tUsage: revive-class-files <database source directory> <destination directory>\n"
//                          + "\tThis utility revives class files found a the database located <database source directory>, and places those files at the <destination directory>.\n";
//    assertEquals(assertString, sw.toString());
//
//  }
//
//  public void testDBUsageCommandNoArgs() throws Exception {
//
//    StringWriter sw = new StringWriter();
//    CommandLineMain clm = new CommandLineMain(sw);
//
//    DBUsageCommand command = new DBUsageCommand(sw);
//    clm.executeCommand(command.optionName(), new String[0]);
//
//    String assertString = "database source required.\n"
//                          + "\tUsage: db-usage <database source directory>\n"
//                          + "\tThis utility prints key statistics of the database located at <database source directory>.\n";
//    assertEquals(assertString, sw.toString());
//  }
//
//  public void testDBDiffCommandNoArgs() throws Exception {
//
//    StringWriter sw = new StringWriter();
//    CommandLineMain clm = new CommandLineMain(sw);
//
//    DBDiffCommand command = new DBDiffCommand(sw);
//    clm.executeCommand(command.optionName(), new String[0]);
//
//    String assertString = "database sources (2) required.\n"
//                          + "\tUsage: db-diff <database source directory 1> <database source directory 2>\n"
//                          + "\tThis utility print a diff report on the databases <database source directory 1> <database source directory 2>.The report consists of differences in generated classes, transactions, client states and managed objects.\n";
//    assertEquals(assertString, sw.toString());
//  }

  public void testCommand() {
    TestCommand command = new TestCommand();

    CommandLineMain commandLineMain = new CommandLineMain();
    commandLineMain.registerCommand(command);
    commandLineMain.executeCommand("test", new String[0]);
    assertTrue(command.executeCall);
  }

  private static class TestCommand implements Command {

    boolean executeCall = false;

    @Override
    public String description() {
      return "test command";
    }

    @Override
    public void execute(String[] args) {
      executeCall = true;
    }

    @Override
    public String name() {
      return "Test Command";
    }

    @Override
    public String optionName() {
      return "test";
    }

    @Override
    public void printUsage() {
      System.out.println("Usage: test command object");
    }

  }
}