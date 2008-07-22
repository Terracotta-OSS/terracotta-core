/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.tools.cli;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.terracotta.modules.tool.GuiceModule;
import org.terracotta.modules.tool.commands.CommandException;
import org.terracotta.modules.tool.commands.CommandRegistry;
import org.terracotta.modules.tool.commands.HelpCommand;
import org.terracotta.modules.tool.commands.InfoCommand;
import org.terracotta.modules.tool.commands.InstallCommand;
import org.terracotta.modules.tool.commands.ListCommand;
import org.terracotta.modules.tool.commands.UpdateCommand;
import org.terracotta.modules.tool.config.Config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.tc.util.ProductInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class TUCApp {

  private static Config createConfig() throws IOException {
    Properties props = new Properties();
    props.load(TUCApp.class.getResourceAsStream("/org/terracotta/modules/tool/tim-get.properties"));
    return new Config(props);
  }

  private static Throwable rootCause(Throwable throwable) {
    Throwable rootCause = throwable;
    while (rootCause.getCause() != null) {
      rootCause = rootCause.getCause();
    }
    return rootCause;
  }

  public static void main(String args[]) {
    ProductInfo pInfo = ProductInfo.getInstance();
    System.out.println(pInfo.toLongString());
    if (pInfo.isPatched()) System.out.println(pInfo.toLongPatchString());
    System.out.println();
    
    Config config = null;
    try {
      config = createConfig();
    }
    catch (Exception e) {
      System.err.println("Could not read configuration: " + e.getMessage());
      System.exit(1);
    }

    Injector injector = null;
    CommandRegistry commandRegistry = null;
    try {
      injector = Guice.createInjector(new GuiceModule(config));

      commandRegistry = injector.getInstance(CommandRegistry.class);
      commandRegistry.addCommand(injector.getInstance(HelpCommand.class));
      commandRegistry.addCommand(injector.getInstance(InfoCommand.class));
      commandRegistry.addCommand(injector.getInstance(InstallCommand.class));
      commandRegistry.addCommand(injector.getInstance(ListCommand.class));
      commandRegistry.addCommand(injector.getInstance(UpdateCommand.class));
    } catch (Exception e) {
      Throwable rootCause = rootCause(e);
      System.err.println("Initialization error: " + rootCause.getClass() + ": " + rootCause.getMessage());
      System.exit(2);
    }

    try {
      String commandName = "help";
      List<String> commandArgs = new ArrayList<String>();
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
      commandRegistry.executeCommand(commandName, commandArgs);
    } catch (CommandException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(2);
    }
  }

}
