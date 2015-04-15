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

import com.tc.cli.command.BaseCommand;
import com.tc.cli.command.Command;
import com.tc.util.ProductInfo;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is the entry point for all TC command line utilities.
 */
public class CommandLineMain {

  private Writer      writer   = new OutputStreamWriter(System.out);

  private Map         commands = new HashMap();

  private HelpCommand helpCommand;

  public CommandLineMain() {
    initialize();
    helpCommand = new HelpCommand(commands, writer);
  }
  
   CommandLineMain(Writer writer) {
    this.writer = writer;
    initialize();
    helpCommand = new HelpCommand(commands, writer);
  }

  private void initialize() {
//    registerCommand(new ManagedObjectReportCommand(writer));
//    registerCommand(new FastLoadOidLogAnalysisCommand(writer));
//    registerCommand(new ReviveClassFilesCommand(writer));
//    registerCommand(new DBUsageCommand(writer));
//    registerCommand(new DBDiffCommand(writer));
//    registerCommand(new SetDbCleanCommand(writer));
  }

  void registerCommand(Command command) {
    commands.put(command.optionName(), command);
  }

  public void executeCommand(String commandName, String[] args) {
    if (commandName.equals(helpCommand.optionName())) {
      helpCommand.execute(args);
    } else {
      Command command = (Command) commands.get(commandName);
      if (command == null) {
        println("not a valid command: " + commandName);
      } else {
        command.execute(args);
      }
    }
  }

  private void println(String message) {
    try {
      writer.write(message);
      writer.write("\n");
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
      writer = new OutputStreamWriter(System.out);
    }
  }

  public void printUsage() {
    println("Type 'help' for usage.");
  }

  public static void main(String[] args) {
    CommandLineMain commandLineMain = new CommandLineMain();

    if (args.length < 1) {
      commandLineMain.printUsage();
      System.exit(1);
    }

    String commandName = args[0];
    String[] commandArgs = new String[0];
    if (args.length > 1) {
      commandArgs = Arrays.asList(args).subList(1, args.length).toArray(new String[0]);
    }
    commandLineMain.executeCommand(commandName, commandArgs);
  }

  private static final class HelpCommand extends BaseCommand {

    private final Map commands;

    public HelpCommand(Map commands, Writer writer) {
      super(writer);
      this.commands = commands;
    }

    @Override
    public String description() {
      return "command lists all the commands available on the Terracotta command-line utility";
    }

    @Override
    public void execute(String[] args) {
      println("Usage: <command> [args]");   
      println("Terracotta command-line utility, version " + ProductInfo.getInstance().version());
      println("Type 'help <command>' for help on a specific command.");
      println("\n");
      if (args.length > 0) {
        String commandName = args[0];
        Command command = (Command) commands.get(commandName);
        if (command != null) {
          println(command.name() + "\n");
          command.printUsage();
          println("\n\n");
        } else {
          println("invalid command: " + commandName);
        }
      } else {
        println("Available commands:");
        for (Iterator iter = commands.values().iterator(); iter.hasNext();) {
          Command command = (Command) iter.next();
          println("\t\t " + command.optionName());
        }
        println("\n");
        for (Iterator iter = commands.values().iterator(); iter.hasNext();) {
          Command command = (Command) iter.next();
          println(command.name() + "\n");
          command.printUsage();
          println("\n\n");
        }
      }
    }

    @Override
    public String name() {
      return "Help";
    }

    @Override
    public String optionName() {
      return "help";
    }

    @Override
    public void printUsage() {
      println("");
    }

  }

}
