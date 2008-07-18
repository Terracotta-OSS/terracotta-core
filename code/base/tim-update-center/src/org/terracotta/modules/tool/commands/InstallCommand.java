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
  private static final String LONGOPT_GROUPID   = "group-id";

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
    options.addOption(buildOption(LONGOPT_GROUPID,
                                  "Use this option to qualify the name of the TIM you are looking for. Ignored if the "
                                      + LONGOPT_ALL + " option is specified", String.class));
    arguments.put("name", "The name of the Integration Module");
    arguments.put("version", "OPTIONAL. The version used to qualify the name");
  }

  public String syntax() {
    return "<name> [version] [options]";
  }
  
  public String description() {
    return "Install an Integration Module";
  }

  private void install(Module module) {
    StringWriter sw = new StringWriter();
    module.printDigest(new PrintWriter(sw));
    module.install(overwrite, pretend, out);
  }

  private void install(String groupId, String artifactId, String version) {
    Module module = null;

    if (version == null) module = modules.getLatest(groupId, artifactId);
    else module = modules.get(ModuleId.create(groupId, artifactId, version));

    if (module == null) {
      out.println("Integration Module '" + artifactId + "' not found");
      out.println("It might be using a groupId other than '" + groupId + "'");
      return;
    }
    install(module);
  }

  private void installAll() {
    out.println("\n*** Installing all of the latest Integration Modules for TC " + modules.tcVersion() + " ***\n");
    List<Module> latest = modules.listLatest();
    for (Module module : latest) {
      install(module);
    }
  }

  public void execute(CommandLine cli) throws CommandException {
    overwrite = cli.hasOption(LONGOPT_OVERWRITE);
    pretend = cli.hasOption(LONGOPT_PRETEND);

    if (cli.hasOption(LONGOPT_ALL)) {
      installAll();
      return;
    }

    List<String> args = cli.getArgList();
    if (args.isEmpty()) {
      String msg = "You need to at least specify the name of the Integration Module you wish to install";
      throw new CommandException(msg);
    }

    String artifactId = args.remove(0);
    String version = args.isEmpty() ? null : args.remove(0);
    String groupId = cli.getOptionValue(LONGOPT_GROUPID, ModuleId.DEFAULT_GROUPID);
    install(groupId, artifactId, version);
  }

}
