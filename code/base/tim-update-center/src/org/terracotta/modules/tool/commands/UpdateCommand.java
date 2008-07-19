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

  private static final String LONGOPT_ALL       = "all";
  private static final String LONGOPT_OVERWRITE = "overwrite";
  private static final String LONGOPT_FORCE     = "force";
  private static final String LONGOPT_PRETEND   = "pretend";

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
    arguments.put("name", "The name of the integration module");
    arguments.put("group-id", "OPTIONAL. The group-id used to qualify the name");
  }

  public String syntax() {
    return "<name> [group-id] {options}";
  }

  public String description() {
    return "Update to the latest version of an integration module";
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

  private void update(Module module, boolean verbose) {
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
    out.println("\n*** Updating installed integration modules for TC " + modules.tcVersion() + " ***\n");
    for (ModuleId entry : installedModules()) {
      Module latest = modules.getLatest(entry.getGroupId(), entry.getArtifactId());

      // installed but not available from the list, skip it.
      if (latest == null) continue;

      // installed and available from the list, install the latest
      update(latest, false);
    }
  }

  public void execute(CommandLine cli) throws CommandException {
    List<String> args = cli.getArgList();
    force = cli.hasOption(LONGOPT_FORCE);
    overwrite = cli.hasOption(LONGOPT_OVERWRITE) || force;
    pretend = cli.hasOption(LONGOPT_PRETEND);

    // --all was specified, update everything that is installed
    if (cli.hasOption(LONGOPT_ALL)) {
      updateAll();
      return;
    }

    // no args and --all not specified, ask user to be more specific
    if (args.isEmpty()) {
      out.println("You need to at least specify the name of the integration module.");
      out.println("You could also just use the --all option to update everything you have installed.");
      return;
    }

    // given the artifactId and maybe the groupId - find some candidates
    String artifactId = args.remove(0);
    String groupId = args.isEmpty() ? null : args.remove(0);
    List<Module> candidates = modules.find(artifactId, null, groupId);

    // no candidates found, inform the user
    if (candidates.isEmpty()) {
      out.println("No module found matching the arguments you specified.");
      out.println("Check that you've spelled them correctly.");
      return;
    }

    // several candidates found, see if we can figure out which one we can install
    if (candidates.size() > 1) {
      // more than 1 found, they are not siblings if no groupId was specified
      // so ask the user to be more specific
      if (groupId == null) {
        out.println("There's more than one integration module found matching the name '" + artifactId + "':");
        out.println();
        for (Module candidate : candidates) {
          ModuleId id = candidate.getId();
          out.println("  * " + id.getArtifactId() + " " + id.getGroupId());
        }
        out.println();
        out.println("Pass the group-id argument in the command to be more specific.");
        return;
      }

      // more than 1 found, they are siblings (since group-id was specified)
      // so get the latest from the lot, and install it
      Module latest = modules.getLatest(groupId, artifactId);
      update(latest, true);
      return;
    }

    // only 1 candidate found, install it
    Module module = candidates.remove(0);
    update(module, true);
  }

}
