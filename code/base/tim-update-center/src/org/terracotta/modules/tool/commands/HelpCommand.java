/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;

import com.google.inject.Inject;

import java.util.List;

public class HelpCommand extends AbstractCommand {
  private CommandRegistry commandRegistry;

  @Inject
  public HelpCommand(CommandRegistry registry) {
    this.commandRegistry = registry;
  }

  public void execute(CommandLine cli) {
    List<String> topics = cli.getArgList();

    if (topics.isEmpty()) {
      out.println(loadHelp("GenericHelp"));
      return;
    }

    for (String cmdname : topics) {
      try {
        Command cmd = commandRegistry.getCommand(cmdname);
        cmd.printHelp();
      } catch (UnknownCommandException e) {
        out.println("Command not supported: " + cmdname);
      }
    }
  }

}
