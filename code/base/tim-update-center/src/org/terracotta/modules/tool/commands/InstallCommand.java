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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class InstallCommand extends AbstractCommand {

  private static final String LONGOPT_ALL       = "all";
  private static final String LONGOPT_OVERWRITE = "overwrite";
  private static final String LONGOPT_PRETEND   = "pretend";

  private final Modules       modules;

  private boolean             overwrite;
  private boolean             pretend;

  @Inject
  public InstallCommand(Modules modules) {
    this.modules = modules;
    assert modules != null : "modules is null";
    options.addOption(buildOption(LONGOPT_ALL,
                                  "Install all compatible TIMs, ignoring the name and version arguments if specified"));
    options.addOption(buildOption(LONGOPT_OVERWRITE, "Overwrite if already installed"));
    options.addOption(buildOption(LONGOPT_PRETEND, "Do not perform actual installation"));
    arguments.put("name", "The name of the integration module");
    arguments.put("version", "OPTIONAL. The version used to qualify the name");
    arguments.put("group-id", "OPTIONAL. The group-id used to qualify the name");
  }

  public String syntax() {
    return "<name> [version] [group-id] {options}";
  }

  public String description() {
    return "Install an integration module";
  }

  private void install(Module module) {
    StringWriter sw = new StringWriter();
    module.printDigest(new PrintWriter(sw));
    module.install(overwrite, pretend, out);
  }

  private void installAll() {
    out.println("*** Installing all of the latest integration modules for TC " + modules.tcVersion() + " ***\n");
    List<Module> latest = modules.listLatest();
    for (Module module : latest) {
      install(module);
    }
  }

  public void execute(CommandLine cli) {
    overwrite = cli.hasOption(LONGOPT_OVERWRITE);
    pretend = cli.hasOption(LONGOPT_PRETEND);

    if (cli.hasOption(LONGOPT_ALL)) {
      installAll();
      return;
    }

    List<String> args = cli.getArgList();
    if (args.isEmpty()) {
      out.println("You need to at least specify the name of the integration module.");
      out.println("You could also use the --all option to install the latest of everything that is available.");
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
        out.println();
        out.println("Try to use both version and group-id arguments in the command to be more specific.");
      }
      return;
    }

    Module module = candidates.remove(0);
    install(module);
  }

}
