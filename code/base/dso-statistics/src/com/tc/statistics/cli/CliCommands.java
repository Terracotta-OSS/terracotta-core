/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli;

import com.tc.exception.TCRuntimeException;
import com.tc.statistics.cli.commands.CommandConnect;
import com.tc.statistics.cli.commands.CommandDisconnect;
import com.tc.statistics.cli.commands.CommandGetSupportedStatistics;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CliCommands {
  private GathererConnection connection = new GathererConnection();

  private final Map registeredCommands;

  public CliCommands() {
    registeredCommands = Collections.unmodifiableMap(new TreeMap() {{
      addCommand(this, new CommandConnect());
      addCommand(this, new CommandDisconnect());
      addCommand(this, new CommandGetSupportedStatistics());
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
      connection.connect();

      for (final Iterator it = commands.iterator(); it.hasNext(); ) {
        String command_name = (String)it.next();
        CliCommand command = (CliCommand)registeredCommands.get(command_name);
        if (null == command) {
          printUnknownCommand(command_name);
        } else {
          try {
            command.execute(connection, null);
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      }

      return true;
    }
  }

  private void printUnknownCommand(final String command) {
    System.out.println("Unknown command '" + command + "'.");
  }
}
