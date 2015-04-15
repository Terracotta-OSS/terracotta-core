/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package org.terracotta.management.cli.rest;

import org.terracotta.management.cli.Command;
import org.terracotta.management.cli.CommandProvider;

public enum CliCommand implements CommandProvider<Context> {

  HELP('h', RestCommandManagerProvider.getCommandManager().getHelpCommand()), 
  GET('g', RestCommandManagerProvider.getCommandManager().getGetCommand()), 
  POST('p', RestCommandManagerProvider.getCommandManager().getPostCommand()), 
  ENCODE('e', RestCommandManagerProvider.getCommandManager().getUrlEncodingCommand()), 
  IGNORE_SSL_ERRORS('k', RestCommandManagerProvider.getCommandManager().getUnsafeSSLCommand()),
  FAIL_ON_EMPTY('f', new FailOnEmptyCommand(), true);

  private final char c;
  private final Command<Context> command;
  private final boolean hidden;

  CliCommand(final char h, final Command<Context> command) {
    this(h, command, false);
  }

  CliCommand(final char h, final Command<Context> command, boolean hidden) {
    this.c = h;
    this.command = command;
    this.hidden = hidden;
  }

  @Override
  public Command<Context> getCommand() {
    return command;
  }

  @Override
  public char getSwitchChar() {
    return c;
  }

  public boolean isHidden() {
    return hidden;
  }
}
