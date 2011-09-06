/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of {@link Command} objects.
 * 
 * @author Jason Voegele (jvoegele@terracotta.org)
 */
public class CommandRegistry {
  private final Map<String, Command> commands = new HashMap<String, Command>();

  public void addCommand(Command command) {
    commands.put(command.name(), command);
  }

  public Command getCommand(String commandName) throws UnknownCommandException {
    Command command = commands.get(commandName);
    if (command == null) throw new UnknownCommandException(commandName);
    return command;
  }

  public Set<String> commandNames() {
    return commands.keySet();
  }

  public void executeCommand(String commandName, String[] args) throws CommandException {
    Command cmd = getCommand(commandName);
    if (cmd == null) { throw new CommandException("Unknown command: " + commandName); }

    CommandLineParser parser = new GnuParser();
    try {
      CommandLine cli = parser.parse(cmd.options(), args);
      if (cli.hasOption(AbstractCommand.OPTION_HELP) || cli.hasOption(AbstractCommand.LONGOPT_HELP)) {
        cmd.printHelp();
        return;
      } else if (cli.hasOption(AbstractCommand.OPTION_UPDATE_INDEX) || cli.hasOption(AbstractCommand.LONGOPT_UDPATE_INDEX)) {
        cmd.forceIndexUpdate();
      }
      cmd.execute(cli);
    } catch (ParseException e) {
      throw new CommandException(e.getMessage(), e);
    }
  }

  public void executeCommand(String commandName, List<String> args) throws CommandException {
    executeCommand(commandName, args.toArray(new String[args.size()]));
  }

  public Collection<Command> commands() {
    return Collections.unmodifiableCollection(commands.values());
  }
}
