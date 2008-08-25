/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.ModuleId;
import org.terracotta.modules.tool.Modules;
import org.terracotta.modules.tool.Module.InstallOption;

import com.google.inject.Inject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InstallCommand extends AbstractCommand {

  private static final String             LONGOPT_ALL       = "all";
  private static final String             LONGOPT_OVERWRITE = "overwrite";
  private static final String             LONGOPT_FORCE     = "force";
  private static final String             LONGOPT_PRETEND   = "pretend";
  private static final String             LONGOPT_NOVERIFY  = "no-verify";

  private final Modules                   modules;

  private final Collection<InstallOption> installOptions;

  @Inject
  public InstallCommand(Modules modules) {
    this.modules = modules;
    assert modules != null : "modules is null";
    options.addOption(buildOption(LONGOPT_ALL,
                                  "Install all compatible TIMs, ignoring the name and version arguments if specified"));
    options.addOption(buildOption(LONGOPT_OVERWRITE, "Install anyway, even if already installed"));
    options.addOption(buildOption(LONGOPT_FORCE, "Synonym to overwrite"));
    options.addOption(buildOption(LONGOPT_PRETEND, "Do not perform actual installation"));
    options.addOption(buildOption(LONGOPT_NOVERIFY, "Skip checksum verification"));
    arguments.put("name", "The name of the integration module");
    arguments.put("version", "(OPTIONAL) The version used to qualify the name");
    arguments.put("group-id", "(OPTIONAL) The group-id used to qualify the name");

    installOptions = new ArrayList<InstallOption>();
  }

  @Override
  public String syntax() {
    return "<name> [version] [group-id] {options}";
  }

  @Override
  public String description() {
    return "Install an integration module";
  }

  private void printEpilogue() {
    out.println();
    out.println("Done.");
  }

  private void install(Module module, boolean verbose) {
    StringWriter sw = new StringWriter();
    module.printDigest(new PrintWriter(sw));
    module.install(out, installOptions);
    if (verbose) printEpilogue();
  }

  private void install(Module module) {
    install(module, true);
  }

  private void installAll() {
    out.println("*** Installing all of the latest integration modules for TC " + modules.tcVersion() + " ***\n");
    List<Module> latest = modules.listLatest();
    for (Module module : latest) {
      install(module, false);
    }
    printEpilogue();
  }

  public void execute(CommandLine cli) {
    if (cli.hasOption(LONGOPT_FORCE)) installOptions.add(InstallOption.FORCE);
    if (cli.hasOption(LONGOPT_OVERWRITE) || cli.hasOption(LONGOPT_FORCE)) installOptions.add(InstallOption.OVERWRITE);
    if (cli.hasOption(LONGOPT_PRETEND)) installOptions.add(InstallOption.PRETEND);
    if (cli.hasOption(LONGOPT_NOVERIFY)) installOptions.add(InstallOption.SKIP_VERIFY);

    // --all was specified, install everything
    if (cli.hasOption(LONGOPT_ALL)) {
      installAll();
      return;
    }

    // no args and --all not specified, ask user to be more specific
    List<String> args = cli.getArgList();
    if (args.isEmpty()) {
      out.println("You need to at least specify the name of the integration module.");
      out.println("You could also use the --all option to install the latest of everything that is available.");
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

    // several candidates found, see if we can figure out which one we can install
    module = modules.getLatest(candidates);
    if (module != null) {
      install(module);
      return;
    }

    // we can't figure out which one to update/install
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
