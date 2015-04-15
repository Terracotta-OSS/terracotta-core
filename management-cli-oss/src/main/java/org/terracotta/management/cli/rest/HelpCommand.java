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
import org.terracotta.management.cli.CommandInvocationException;

/**
 * @author Ludovic Orban
 */
class HelpCommand implements Command<Context> {

  @Override
  public void execute(final Context context) throws CommandInvocationException {
    System.out.println("Usage : ");
    System.out.println("rest-client <flags> <url> <POST data> <json query 1> <json query 2> ... <json query n>");
    for (CliCommand cliCommand : CliCommand.values()) {
      if (!cliCommand.isHidden() && (cliCommand.getCommand()!=null)) {
        System.out.println("  -" + cliCommand.getSwitchChar() + " " + cliCommand.getCommand().helpMessage());
      }
    }
    String exampleUsage = "Example usages:\n" +
                          "  rest-client -e -g http://localhost:9540/tc-management-api/v2/agents ";


    System.out.println(exampleUsage);
  }

  @Override
  public String helpMessage() {
    return "Prints this help message";
  }
}
