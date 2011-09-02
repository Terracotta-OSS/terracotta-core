/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Base interface implemented by all commands.
 * 
 * @author Jason Voegele (jvoegele@terracotta.org)
 */
public interface Command {

  /** The name of this command. */
  public String name();

  /** The syntax of this command. */
  public String syntax();

  /** The description of this command. */
  public String description();
  
  /**
   * The command-line options that this command accepts.
   */
  public Options options();

  /**
   * Descriptive help text for this command.  The help text should only contain
   * descriptive information about the command and general usage, and should not
   * contain any information about the available command-line options since that
   * information will be derived from the information obtained from the
   * {@link #options()} method.
   */
  public String help();

  /**
   * Execute whatever actions that this command performs.
   * 
   * @param commandLine The {@link CommandLine} object containing the parsed
   *        options and any remaining command-line arguments.
   */
  public void execute(CommandLine commandLine) throws CommandException;

  /**
   * Print the help text for this command.
   */
  public void printHelp();
  
  /**
   * Force tim-get to pull down fresh index
   */
  public void forceIndexUpdate();
}
