/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.ModuleId;
import org.terracotta.modules.tool.Modules;

import com.google.inject.Inject;

import java.util.List;

public class InfoCommand extends AbstractCommand {

  private final Modules modules;

  @Inject
  public InfoCommand(Modules modules) {
    this.modules = modules;
    assert modules != null : "modules is null";
    arguments.put("name", "The name of the integration module");
    arguments.put("version", "(OPTIONAL) The version used to qualify the name");
    arguments.put("group-id", "(OPTIONAL) The group-id used to qualify the name");
  }

  public String syntax() {
    return "<name> [version] [group-id] {options}";
  }

  public String description() {
    return "Display detailed information about an integration module";
  }

  public void execute(CommandLine cli) {
    // no args specified, ask user to be more specific
    List<String> args = cli.getArgList();
    if (args.isEmpty()) {
      out.println("You need to at least specify the name of the integration module.");
      return;
    }

    // given the artifactId and maybe the version and groupId - find some candidates
    Module module = null;
    String artifactId = args.remove(0);
    String version = args.isEmpty() ? null : args.remove(0);
    String groupId = args.isEmpty() ? null : args.remove(0);
    
    // get candidates
    List<Module> candidates = modules.find(artifactId, version, groupId);
    
    // no candidates found, inform the user
    if (candidates.isEmpty()) {
      out.println("No module found matching the arguments you specified.");
      out.println("Check that you've spelled them correctly.");
      return;
    }

    // several candidates found, see if we can figure out which one we can retrieve
    module = modules.getLatest(candidates);
    if (module != null) {
      module.printDetails(out);
      return;
    }

    // we can't figure out which one to retrieve
    // so ask the user to be more specific
    out.println("There's more than one integration module found matching the name '" + artifactId + "':");
    out.println();
    for (Module candidate : candidates) {
      ModuleId id = candidate.getId();
      out.println("  * " + id.getArtifactId() + " " + id.getVersion() + " " + id.getGroupId());
    }
    out.println();
    out.println("Try to use both version and group-id arguments in the command to be more specific.");
  }

}
