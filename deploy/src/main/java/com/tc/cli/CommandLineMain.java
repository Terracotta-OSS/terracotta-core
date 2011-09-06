/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cli;

import com.tc.cli.command.BaseCommand;
import com.tc.cli.command.Command;
import com.tc.cli.command.DBDiffCommand;
import com.tc.cli.command.DBUsageCommand;
import com.tc.cli.command.FastLoadOidLogAnalysisCommand;
import com.tc.cli.command.ManagedObjectReportCommand;
import com.tc.cli.command.ReviveClassFilesCommand;
import com.tc.cli.command.SetDbCleanCommand;
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
    registerCommand(new ManagedObjectReportCommand(writer));
    registerCommand(new FastLoadOidLogAnalysisCommand(writer));
    registerCommand(new ReviveClassFilesCommand(writer));
    registerCommand(new DBUsageCommand(writer));
    registerCommand(new DBDiffCommand(writer));
    registerCommand(new SetDbCleanCommand(writer));
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

    public String description() {
      return "command lists all the commands available on the Terracotta command-line utility";
    }

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

    public String name() {
      return "Help";
    }

    public String optionName() {
      return "help";
    }

    public void printUsage() {
      println("");
    }

  }

}
