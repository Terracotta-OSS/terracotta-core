/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;
import com.tc.util.runtime.Os;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HelpCommand extends AbstractCommand {
  private final CommandRegistry commandRegistry;

  @Inject
  public HelpCommand(CommandRegistry registry) {
    this.commandRegistry = registry;
    arguments.put("command-names", "(OPTIONAL) Space delimited list of command names to get a help on");
  }

  /** The syntax of this command. */
  @Override
  public String syntax() {
    return "[command-names] {options}";
  }

  @Override
  public String description() {
    return "Display help information";
  }

  public void execute(CommandLine cli) {
    List<String> topics = cli.getArgList();
    if (topics.isEmpty()) {
      List<String> list = new ArrayList<String>(commandRegistry.commandNames());
      Collections.sort(list);
      out.println("This is the Terracotta Integration Modules manager.");
      out.println("Below is a list of all the commands that are available.");
      out.println();
      out.println("General syntax:");
      out.println();
      String scriptName = "tim-get" + (Os.isWindows() ? ".bat" : ".sh");
      out.println(StringUtils.leftPad(scriptName + " [command] [arguments] {options}", 46));
      out.println();
      out.println("Commands:");
      for (String name : list) {
        try {
          Command cmd = commandRegistry.getCommand(name);
          out.println(StringUtils.leftPad(cmd.name(), 10) + "   " + cmd.description());
        } catch (UnknownCommandException e) {
          //
        }
      }
      out.println();
      out.println("Further help:");
      out.println("   Each command accepts a --help option that will display additional");
      out.println("   usage information for the command. Specifying the command name after");
      out.println("   the \"help\" command does the same thing.");
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
