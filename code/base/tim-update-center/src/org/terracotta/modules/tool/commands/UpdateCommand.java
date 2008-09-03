/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.terracotta.modules.tool.InstallListener;
import org.terracotta.modules.tool.InstallOption;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.ModuleHelper;
import org.terracotta.modules.tool.ModuleReport;
import org.terracotta.modules.tool.Modules;
import org.terracotta.modules.tool.Reference;

import com.google.inject.Inject;
import com.tc.bundles.OSGiToMaven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class UpdateCommand extends OneOrAllCommand {

  // private static final String LONGOPT_ALL = "all";
  private static final String             LONGOPT_OVERWRITE = "overwrite";
  private static final String             LONGOPT_FORCE     = "force";
  private static final String             LONGOPT_PRETEND   = "pretend";
  private static final String             LONGOPT_NOVERIFY  = "no-verify";

  private final Modules                   modules;
  private final ModuleReport              report;
  private final Collection<InstallOption> installOptions;

  @Inject
  public UpdateCommand(Modules modules, ModuleReport report) {
    this.modules = modules;
    this.report = report;
    options.addOption(buildOption(LONGOPT_ALL, "Update all installed TIMs, all other arguments are ignored"));
    options.addOption(buildOption(LONGOPT_FORCE, "Update anyway, even if update is already installed"));
    options.addOption(buildOption(LONGOPT_OVERWRITE, "Overwrite if already installed"));
    options.addOption(buildOption(LONGOPT_PRETEND, "Do not perform actual installation"));
    options.addOption(buildOption(LONGOPT_NOVERIFY, "Skip checksum verification"));
    arguments.put("name", "The name of the integration module");
    arguments.put("group-id", "(OPTIONAL) The group-id used to qualify the name");
    installOptions = new ArrayList<InstallOption>();
  }

  @Override
  public String syntax() {
    return "<name> [group-id] {options}";
  }

  @Override
  public String description() {
    return "Update to the latest version of an integration module";
  }

  @Override
  protected void handleAll() {
    out.println("*** Updating installed integration modules for TC " + modules.tcVersion() + " ***\n");

    // construct list of updateable TIMs
    List<Module> manifest = new ArrayList<Module>();
    List<Reference> localModules = localModules();

    if (localModules.isEmpty()) {
      out.println("It appears to me that there are no integration modules installed - no updates can be performed.");
      out.println("Please check if the path to your local repository exists: " + modules.repository());
      return;
    }

    for (Reference entry : localModules) {
      List<Module> siblings = modules.getSiblings(entry.symbolicName());
      Module latest = ModuleHelper.getLatest(siblings);

      // installed but not available from the list -
      // or already installed and --force was not specified then skip it
      if ((latest == null) || (latest.isInstalled() && !installOptions.contains(InstallOption.FORCE))) continue;

      // installed and available from the list, install the latest
      manifest.add(latest);
    }

    if (manifest.isEmpty()) {
      out.println("No updates found.");
      return;
    }

    InstallListener listener = new DefaultInstallListener(report, out);
    for (Module module : manifest) {
      module.install(listener, installOptions);
    }
    printEpilogue();
  }

  @Override
  protected void handleOne(Module module) {
    if (module.isInstalled() && !installOptions.contains(InstallOption.FORCE)) {
      out.println("No updates found.");
      return;
    }

    // update found, install it
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

    // // --all was specified, update everything that is installed
    // if (cli.hasOption(LONGOPT_ALL)) {
    // updateAll();
    // return;
    // }
    //
    // // no args and --all not specified, ask user to be more specific
    // List<String> args = cli.getArgList();
    // if (args.isEmpty()) {
    // out.println("You need to at least specify the name of the integration module.");
    // out.println("You could also just use the --all option to update everything you have installed.");
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
    // update(module);
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
  }

  private Attributes readAttributes(File jarfile) {
    JarInputStream in = null;
    try {
      in = new JarInputStream(new FileInputStream(jarfile));
      Manifest manifest = in.getManifest();
      return (manifest == null) ? null : manifest.getMainAttributes();
    } catch (IOException e) {
      return null;
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  private List<Reference> localModules() {
    List<Reference> list = new ArrayList<Reference>();

    File repository = modules.repository();
    if (!repository.exists()) return list;

    Collection<File> jarfiles = FileUtils.listFiles(repository, new String[] { "jar" }, true);
    for (File jarfile : jarfiles) {
      Attributes manifest = readAttributes(jarfile);
      if ((manifest == null) || !"Terracotta Integration Module".equals(manifest.getValue("Bundle-Category"))) continue;

      String symbolicName = manifest.getValue("Bundle-SymbolicName");
      String version = manifest.getValue("Bundle-Version");
      String artifactId = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
      String groupId = OSGiToMaven.groupIdFromSymbolicName(symbolicName);

      Map<String, Object> attributes = new HashMap<String, Object>();
      attributes.put("groupId", groupId);
      attributes.put("artifactId", artifactId);
      attributes.put("version", version);
      list.add(new Reference(null, attributes));
    }

    Collections.sort(list);
    return list;
  }

  private void printEpilogue() {
    out.println("\nDone.");
  }

}
