/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.InstallListener;
import org.terracotta.modules.tool.InstallOption;
import org.terracotta.modules.tool.Module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InstallCommand extends OneOrAllCommand {

  private static final String             LONGOPT_OVERWRITE = "overwrite";
  private static final String             LONGOPT_FORCE     = "force";
  private static final String             LONGOPT_DRYRUN   = "dry-run";
  private static final String             LONGOPT_NOVERIFY  = "no-verify";

  private final Collection<InstallOption> installOptions;

  public InstallCommand() {
    options.addOption(buildOption(LONGOPT_ALL, "Install all compatible TIMs,  all other arguments are ignored"));
    options.addOption(buildOption(LONGOPT_OVERWRITE, "Install anyway, even if already installed"));
    options.addOption(buildOption(LONGOPT_FORCE, "Synonym to overwrite"));
    options.addOption(buildOption(LONGOPT_DRYRUN, "Do not perform actual installation"));
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
      module.install(listener, actionLog(), installOptions);
    }
    printEpilogue(true);
  }

  @Override
  protected void handleOne(Module module) {
    InstallListener listener = new DefaultInstallListener(report, out);
    module.install(listener, actionLog(), installOptions);
    printEpilogue(module.installsAsModule());
  }

  public void execute(CommandLine cli) {
    if (cli.hasOption(LONGOPT_FORCE)) installOptions.add(InstallOption.FORCE);
    if (cli.hasOption(LONGOPT_OVERWRITE) || cli.hasOption(LONGOPT_FORCE)) installOptions.add(InstallOption.OVERWRITE);
    if (cli.hasOption(LONGOPT_DRYRUN)) installOptions.add(InstallOption.DRYRUN);
    if (cli.hasOption(LONGOPT_NOVERIFY)) installOptions.add(InstallOption.SKIP_VERIFY);
    process(cli, modules);
  }

  private void printEpilogue(boolean updateConfig) {
    if(updateConfig) {
      out.println("\nDone. (Make sure to update your tc-config.xml with the new/updated version if necessary)");
    } else {
      out.println("\nDone.");
    }
  }

}
