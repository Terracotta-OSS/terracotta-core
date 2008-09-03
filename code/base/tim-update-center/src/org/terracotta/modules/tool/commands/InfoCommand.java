/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.ModuleReport;
import org.terracotta.modules.tool.Modules;

import com.google.inject.Inject;

public class InfoCommand extends OneOrAllCommand {

  private final Modules      modules;
  private final ModuleReport report;

  @Inject
  public InfoCommand(Modules modules, ModuleReport report) {
    this.modules = modules;
    this.report = report;
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
    // try {
    //
    // // no args specified, ask user to be more specific
    // List<String> args = cli.getArgList();
    // if (args.isEmpty()) {
    // out.println("You need to at least specify the name of the integration module.");
    // return;
    // }
    //
    // // given the artifactId and maybe the version and groupId - find some candidates
    // // get candidates
    // Module module = null;
    // List<Module> candidates = modules.find(args);
    //
    // // no candidates found, inform the user
    // if (candidates.isEmpty()) {
    // out.println("No module found matching the arguments you specified.");
    // out.println("Check that you've spelled them correctly.");
    // return;
    // }
    //
    // // several candidates found, see if we can figure out which one we can retrieve
    // module = ModuleHelper.getLatest(candidates);
    // if (module != null) {
    // report.printSummary(module, out);
    // out.println();
    // report.printFooter(null, out);
    // return;
    // }
    //
    // // we can't figure out which one to retrieve so ask the user to be more specific
    // out.println("There's more than one integration module found matching the name '" + args.get(0) + "':");
    // out.println();
    // for (Module candidate : candidates) {
    // out.println("  * " + candidate.artifactId() + " " + candidate.version() + " " + candidate.groupId());
    // }
    // out.println();
    // out.println("Try to use both version and group-id arguments in the command to be more specific.");
    // } catch (Throwable t) {
    // t.printStackTrace();
    // }
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
