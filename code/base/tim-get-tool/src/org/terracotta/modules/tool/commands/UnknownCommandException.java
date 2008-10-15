/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool.commands;

public class UnknownCommandException extends CommandException {

  public UnknownCommandException(String commandName) {
    super("Unknown command: " + commandName);
  }
  
}
