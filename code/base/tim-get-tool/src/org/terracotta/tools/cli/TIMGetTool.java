/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.tools.cli;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.terracotta.modules.tool.commands.ActionLog;
import org.terracotta.modules.tool.commands.CommandException;
import org.terracotta.modules.tool.commands.CommandRegistry;
import org.terracotta.modules.tool.commands.HelpCommand;
import org.terracotta.modules.tool.commands.InfoCommand;
import org.terracotta.modules.tool.commands.InstallCommand;
import org.terracotta.modules.tool.commands.InstallForCommand;
import org.terracotta.modules.tool.commands.ListCommand;
import org.terracotta.modules.tool.commands.UpdateCommand;
import org.terracotta.modules.tool.commands.UpgradeCommand;
import org.terracotta.modules.tool.config.Config;
import org.terracotta.modules.tool.exception.ModuleNotFoundException;
import org.terracotta.modules.tool.exception.RemoteIndexIOException;
import org.terracotta.modules.tool.util.CommandUtil;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.tc.util.ProductInfo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class TIMGetTool {

  public static void main(String[] args) {
    new TIMGetTool(args);
  }

  private String          commandName;
  private List<String>    commandArgs;
  private CommandRegistry commandRegistry;
  private ActionLog       actionLog;

  public TIMGetTool(String[] args) {
    prologue();
    try {
      mainWithExceptions(args, null);
    } catch (CommandException e1) {
      System.out.println(e1.getMessage());
      System.out.println();
      try {
        commandRegistry.executeCommand(CommandUtil.deductNameFromClass(HelpCommand.class), new String[0]);
      } catch (CommandException e2) {
        System.out.println(e2.getMessage());
      }
      System.exit(1);
    } catch (RemoteIndexIOException e) {
      System.out.println("There were some error trying to resolve the index file.");
      System.out.println("Error Message: " + e.getMessage());
      System.out.println("   1) Cannot load remote index file from '" + getRemoteURLString(e) + "'.");
      if (e.getLocalDataFile() != null) {
        System.out.println("   2) Cannot resolve local cached copy at '" + e.getLocalDataFile().getAbsolutePath()
                           + "' either.");
      }
      System.out.println("Please make sure you are connected to the internet.");
      System.out.println();
      System.out.flush();
      System.exit(2);
    } catch (ModuleNotFoundException e) {
      System.exit(3);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(100);
    }
  }

  /**
   * Useful for testing to control the environment a bit more.
   */
  public TIMGetTool(String commandLine, Properties timGetProps) throws Exception {
    String[] args = commandLine.split("\\s"); // Split on whitespace
    mainWithExceptions(args, timGetProps);
  }

  private void prologue() {
    ProductInfo pInfo = ProductInfo.getInstance();
    System.out.println(pInfo.toLongString());
    if (pInfo.isPatched()) System.out.println(pInfo.toLongPatchString());
    System.out.println();
  }

  public void mainWithExceptions(String args[], Properties timGetProps) throws Exception {
    parse(args);

    actionLog = new ActionLog();
    if (timGetProps == null) {
      configure(createConfig());
    } else {
      configure(new Config(timGetProps));
    }
    execute();
  }

  private void parse(String args[]) throws Exception {
    commandName = CommandUtil.deductNameFromClass(HelpCommand.class);
    commandArgs = new ArrayList<String>();
    if (args.length != 0) {
      if (args[0].startsWith("-")) {
        Options options = new Options();
        options.addOption("h", "help", false, "Display help information.");
        CommandLineParser parser = new GnuParser();
        parser.parse(options, args);
      } else {
        commandName = args[0];
        commandArgs = new ArrayList<String>(Arrays.asList(args));
        commandArgs.remove(0);
      }
    }
  }

  private Config createConfig() throws Exception {
    InputStream in = TIMGetTool.class.getResourceAsStream("/tim-get.properties");
    if (in == null) {
      System.err.println("Can't locate tim-get.properties file. Did you have a complete kit?");
      System.exit(1);
    }
    Properties props = new Properties();
    props.load(in);
    return new Config(props);
  }

  private void configure(Config config) throws Exception {
    Injector injector = null;
    try {
      injector = Guice.createInjector(new AppContext(config, actionLog));
      commandRegistry = injector.getInstance(CommandRegistry.class);
      commandRegistry.addCommand(injector.getInstance(HelpCommand.class));
      commandRegistry.addCommand(injector.getInstance(InfoCommand.class));
      commandRegistry.addCommand(injector.getInstance(InstallCommand.class));
      commandRegistry.addCommand(injector.getInstance(InstallForCommand.class));
      commandRegistry.addCommand(injector.getInstance(ListCommand.class));
      commandRegistry.addCommand(injector.getInstance(UpdateCommand.class));
      commandRegistry.addCommand(injector.getInstance(UpgradeCommand.class));
    } catch (Exception e) {
      e.printStackTrace();
      Throwable rootCause = rootCause(e);
      throw new Exception("Initialization error: " + rootCause.getClass() + ": " + rootCause.getMessage());
    }
  }

  private Throwable rootCause(Throwable throwable) {
    Throwable rootCause = throwable;
    while (rootCause.getCause() != null) {
      rootCause = rootCause.getCause();
    }
    return rootCause;
  }

  private void execute() throws CommandException {
    commandRegistry.executeCommand(commandName, commandArgs);
  }

  private String getRemoteURLString(RemoteIndexIOException e) {
    return e.getRemoteDataUrl() == null ? "UNKNOWN" : e.getRemoteDataUrl().toString();
  }

  public ActionLog getActionLog() {
    return this.actionLog;
  }

}
