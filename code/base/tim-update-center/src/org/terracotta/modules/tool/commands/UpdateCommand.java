/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.ModuleId;
import org.terracotta.modules.tool.Modules;

import com.google.inject.Inject;
import com.tc.bundles.OSGiToMaven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class UpdateCommand extends AbstractCommand {

  private static final String LONGOPT_ALL      = "all";
  private static final String LONGOPT_OVERWRITE = "overwrite";
  private static final String LONGOPT_FORCE    = "force";
  private static final String LONGOPT_PRETEND  = "pretend";
  private static final String LONGOPT_GROUPID  = "group-id";

  private final Modules       modules;

  private boolean             force;
  private boolean             overwrite;
  private boolean             pretend;

  @Inject
  public UpdateCommand(Modules modules) {
    this.modules = modules;
    assert modules != null : "modules is null";
    options.addOption(buildOption(LONGOPT_ALL,
                                  "Update all installed TIMs, ignoring the name and version arguments if specified"));
    options.addOption(buildOption(LONGOPT_FORCE, "Update anyway, even if update is already installed"));
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
    return "Update to the latest version of an Integration Module";
  }

  private Attributes readAttributes(File jarfile) {
    JarInputStream in = null;
    try {
      in = new JarInputStream(new FileInputStream(jarfile));
      Manifest manifest = in.getManifest();
      return manifest.getMainAttributes();
    } catch (IOException e) {
      return null;
    } finally {
      if (in != null) try {
        in.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private List<ModuleId> installedModules() throws CommandException {
    File repository = Module.repositoryPath();
    if (!repository.exists()) {
      String msg = "The local TIM repository '" + repository + "' does not exist";
      throw new CommandException(msg);
    }

    Collection<File> jarfiles = FileUtils.listFiles(repository, new String[] { "jar" }, true);
    List<ModuleId> list = new ArrayList<ModuleId>();
    for (File jarfile : jarfiles) {
      Attributes attrs = readAttributes(jarfile);
      if (attrs == null) continue;

      String symbolicName = attrs.getValue("Bundle-SymbolicName");
      String version = attrs.getValue("Bundle-Version");
      String artifactId = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
      String groupId = OSGiToMaven.groupIdFromSymbolicName(symbolicName);
      list.add(ModuleId.create(groupId, artifactId, version));
    }

    Collections.sort(list);
    return list;
  }

  private void update(String groupId, String artifactId, boolean verbose) {
    Module module = modules.getLatest(groupId, artifactId);

    // installed but not available from the list, skip it.
    if (module == null) {
      if (!verbose) return;
      out.println("Integration Module '" + artifactId + "' not found");
      out.println("It might be using a groupId other than '" + groupId + "'");
      return;
    }

    // latest already installed, skip it (unless force flag is set)
    assert module.isLatest() : module + " is not the latest";
    if (module.isInstalled() && !force) {
      if (verbose) out.println("No updates found");
      return;
    }

    // update found, install it
    module.install(overwrite, pretend, out);
  }

  private void updateAll() throws CommandException {
    out.println("\n*** Updating installed Integration Modules for TC " + modules.tcVersion() + " ***\n");
    for (ModuleId entry : installedModules()) {
      update(entry.getGroupId(), entry.getArtifactId(), false);
    }
  }

  public void execute(CommandLine cli) throws CommandException {
    List<String> args = cli.getArgList();
    force = cli.hasOption(LONGOPT_FORCE);
    overwrite = cli.hasOption(LONGOPT_OVERWRITE) || force;
    pretend = cli.hasOption(LONGOPT_PRETEND);

    if (cli.hasOption(LONGOPT_ALL)) {
      updateAll();
      return;
    }

    if (args.isEmpty()) {
      String msg = "You need to at least specify the name of the Integration Module you wish to update";
      throw new CommandException(msg);
    }

    String artifactId = args.remove(0);
    String groupId = cli.getOptionValue(LONGOPT_GROUPID, ModuleId.DEFAULT_GROUPID);
    update(groupId, artifactId, true);
  }

}
