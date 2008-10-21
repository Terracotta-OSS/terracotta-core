/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli;

import com.tc.statistics.cli.commands.CommandCaptureStatistic;
import com.tc.statistics.cli.commands.CommandClearAllStatistics;
import com.tc.statistics.cli.commands.CommandClearStatistics;
import com.tc.statistics.cli.commands.CommandCloseSession;
import com.tc.statistics.cli.commands.CommandCreateSession;
import com.tc.statistics.cli.commands.CommandEnableStatistics;
import com.tc.statistics.cli.commands.CommandGetActiveSessionId;
import com.tc.statistics.cli.commands.CommandGetAvailableSessionIds;
import com.tc.statistics.cli.commands.CommandGetGlobalParam;
import com.tc.statistics.cli.commands.CommandGetSessionParam;
import com.tc.statistics.cli.commands.CommandGetSupportedStatistics;
import com.tc.statistics.cli.commands.CommandReinitialize;
import com.tc.statistics.cli.commands.CommandRetrieveStatistics;
import com.tc.statistics.cli.commands.CommandSetGlobalParam;
import com.tc.statistics.cli.commands.CommandSetSessionParam;
import com.tc.statistics.cli.commands.CommandShutdown;
import com.tc.statistics.cli.commands.CommandStartCapturing;
import com.tc.statistics.cli.commands.CommandStartup;
import com.tc.statistics.cli.commands.CommandStopCapturing;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererException;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.management.RuntimeMBeanException;
import javax.naming.ServiceUnavailableException;

public class CliCommands {
  private GathererConnection connection = new GathererConnection();

  private final Map registeredCommands;

  public CliCommands() {
    registeredCommands = Collections.unmodifiableMap(new LinkedHashMap() {{
      addCommand(this, new CommandStartup());
      addCommand(this, new CommandShutdown());
      addCommand(this, new CommandCreateSession());
      addCommand(this, new CommandCloseSession());
      addCommand(this, new CommandGetActiveSessionId());
      addCommand(this, new CommandGetAvailableSessionIds());
      addCommand(this, new CommandGetSupportedStatistics());
      addCommand(this, new CommandEnableStatistics());
      addCommand(this, new CommandCaptureStatistic());
      addCommand(this, new CommandStartCapturing());
      addCommand(this, new CommandStopCapturing());
      addCommand(this, new CommandRetrieveStatistics());
      addCommand(this, new CommandSetGlobalParam());
      addCommand(this, new CommandGetGlobalParam());
      addCommand(this, new CommandSetSessionParam());
      addCommand(this, new CommandGetSessionParam());
      addCommand(this, new CommandClearStatistics());
      addCommand(this, new CommandClearAllStatistics());
      addCommand(this, new CommandReinitialize());
    }});
  }
  
  private static void addCommand(final Map map, final CliCommand command) {
    Assert.assertNotNull("map", map);
    Assert.assertNotNull("command", command);
    map.put(command.getCommandName(), command);
  }

  public GathererConnection getConnection() {
    return connection;
  }

  public Collection getSupportedCommands() {
    return registeredCommands.values();
  }

  public boolean processCommandList(final List commands) throws IOException {
    if (null == commands ||
        0 == commands.size()) {
      return false;
    } else {
      try {
        connection.connect();
      } catch (IOException e) {
        if (e.getCause() != null &&
            e.getCause() instanceof ServiceUnavailableException) {
          System.out.println("Unable to connect to host '" + connection.getHost() + "' and port '" + connection.getPort() + "'.");
          System.out.println("Are you sure there is a Terracotta server instance running there?");
          return false;
        } else {
          throw e;
        }
      }

      // create the commands that have to be executed together with their arguments
      final Map commands_to_execute = new LinkedHashMap();

      for (final Iterator it = commands.iterator(); it.hasNext(); ) {
        String command_name = (String)it.next();
        CliCommand command = (CliCommand)registeredCommands.get(command_name);
        if (null == command) {
          printUnknownCommand(command_name);
        } else {
          String[] argument_names = command.getArgumentNames();
          if (null == argument_names) {
            argument_names = CliCommand.NO_ARGUMENTS;
          }
          final String[] arguments = new String[argument_names.length];
          for (int i = 0; i < arguments.length; i++) {
            if (!it.hasNext()) {
              System.out.println("The command '" + command_name + "' requires the argument '" + argument_names[i] + "'.");
              return false;
            } else {
              arguments[i] = (String)it.next();
            }
          }

          commands_to_execute.put(command, arguments);
        }
      }

      // iterate over the commands and execute them
      for (Iterator it = commands_to_execute.entrySet().iterator(); it.hasNext(); ) {
        final Map.Entry command_entry = (Map.Entry)it.next();

        final CliCommand command = (CliCommand)command_entry.getKey();
        final String[] arguments = (String[])command_entry.getValue();
        try {

          // execute a single command
          command.execute(connection, arguments);
          
        } catch (RuntimeMBeanException e) {
          // handle meaningful backend exceptions that are bubbling up through JMX
          Throwable cause1 = e.getCause();
          if (cause1 != null &&
              cause1 instanceof RuntimeException) {
            Throwable cause2 = cause1.getCause();
            if (cause2 != null) {
             if (cause2 instanceof StatisticsGathererException) {
               System.out.println("> "+command.getCommandName() + " : " + cause2.getMessage());
               return false;
             }
            }
          }
          throw e;
        }
      }

      return true;
    }
  }

  private void printUnknownCommand(final String command) {
    System.out.println("Unknown command '" + command + "'.");
  }
}
