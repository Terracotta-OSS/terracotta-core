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

  // private static final String LONGOPT_GROUPID = "group-id";

  private final Modules modules;

  @Inject
  public InfoCommand(Modules modules) {
    this.modules = modules;
    assert modules != null : "modules is null";
    arguments.put("name", "The name of the Integration Module");
    arguments.put("version", "OPTIONAL. The version used to qualify the name");
    arguments.put("group-id", "OPTIONAL. The group-id used to qualify the name");
  }

  public String syntax() {
    return "<name> [version] [group-id] [options]";
  }

  public String description() {
    return "Display detailed information about an Integration Module";
  }

  public void execute(CommandLine cli) {
    List<String> args = cli.getArgList();
    if (args.isEmpty()) {
      out.println("You need to at least specify the name of the integration module.");
      return;
    }

    String artifactId = args.remove(0);
    String version = args.isEmpty() ? null : args.remove(0);
    String groupId = args.isEmpty() ? null : args.remove(0);
    List<Module> candidates = modules.find(artifactId, version, groupId);
    if (candidates.isEmpty() || (candidates.size() > 1)) {
      if (candidates.isEmpty()) {
        out.println("No module found matching the arguments you specified.");
        out.println("Check that you've spelled them correctly.");
      } else {
        out.println("There's more than one integration module found matching the name '" + artifactId + "':");
        out.println();
        for (Module candidate : candidates) {
          ModuleId id = candidate.getId();
          out.println("  * " + id.getArtifactId() + " " + id.getVersion() + " " + id.getGroupId());
        }
        out.println("Try to use both version and group-id arguments in the command to be more specific.");
      }
      return;
    }
    
    Module module = candidates.remove(0);
    module.printDetails(out);
  }

}
