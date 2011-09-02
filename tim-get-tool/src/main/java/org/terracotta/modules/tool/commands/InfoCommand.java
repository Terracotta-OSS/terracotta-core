/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.Module;

public class InfoCommand extends OneOrAllCommand {

  public InfoCommand() {
    arguments.put("name", "The name of the integration module");
    arguments.put("version", "(OPTIONAL) The version used to qualify the name");
    arguments.put("group-id", "(OPTIONAL) The group-id used to qualify the name");
  }

  @Override
  public String syntax() {
    return "<name> [version] [group-id] {options}";
  }

  @Override
  public String description() {
    return "Display detailed information about an integration module";
  }

  public void execute(CommandLine cli) {
    process(cli, modules);
  }

  @Override
  protected void handleAll() {
    // -- nothing to do here the --all option is not supported by this command --
  }

  @Override
  protected void handleOne(Module module) {
    report.printSummary(module, out);
    out.println();
    report.printFooter(null, out);
  }

}
