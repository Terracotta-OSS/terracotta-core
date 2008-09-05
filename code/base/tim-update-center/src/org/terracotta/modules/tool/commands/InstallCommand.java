/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.InstallListener;
import org.terracotta.modules.tool.InstallOption;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.ModuleReport;
import org.terracotta.modules.tool.Modules;

import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InstallCommand extends OneOrAllCommand {

  // private static final String LONGOPT_ALL = "all";
  private static final String             LONGOPT_OVERWRITE = "overwrite";
  private static final String             LONGOPT_FORCE     = "force";
  private static final String             LONGOPT_PRETEND   = "pretend";
  private static final String             LONGOPT_NOVERIFY  = "no-verify";

  private final Modules                   modules;
  private final ModuleReport              report;
  private final Collection<InstallOption> installOptions;

  @Inject
  public InstallCommand(Modules modules, ModuleReport report) {
    this.modules = modules;
    this.report = report;
    options.addOption(buildOption(LONGOPT_ALL, "Install all compatible TIMs,  all other arguments are ignored"));
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

  @Override
  protected void handleAll() {
    out.println("*** Installing all of the latest integration modules for TC " + modules.tcVersion() + " ***\n");
    List<Module> latest = modules.listLatest();
    InstallListener listener = new DefaultInstallListener(report, out);
    for (Module module : latest) {
      module.install(listener, installOptions);
    }
    printEpilogue();
  }

  @Override
  protected void handleOne(Module module) {
    InstallListener listener = new DefaultInstallListener(report, out);
    module.install(listener, installOptions);
    printEpilogue();
  }

  public void execute(CommandLine cli) {
    if (cli.hasOption(LONGOPT_FORCE)) installOptions.add(InstallOption.FORCE);
    if (cli.hasOption(LONGOPT_OVERWRITE) || cli.hasOption(LONGOPT_FORCE)) installOptions.add(InstallOption.OVERWRITE);
    if (cli.hasOption(LONGOPT_PRETEND)) installOptions.add(InstallOption.PRETEND);
    if (cli.hasOption(LONGOPT_NOVERIFY)) installOptions.add(InstallOption.SKIP_VERIFY);
    process(cli, modules);
  }

  private void printEpilogue() {
    out.println("\nDone.");
  }

}
