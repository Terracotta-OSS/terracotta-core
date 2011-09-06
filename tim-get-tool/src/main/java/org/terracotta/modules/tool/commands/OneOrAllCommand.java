/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang.StringUtils;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.ModuleHelper;
import org.terracotta.modules.tool.Modules;
import org.terracotta.modules.tool.exception.ModuleNotFoundException;

import java.util.List;

abstract class OneOrAllCommand extends ModuleOperatorCommand {

  protected static final String LONGOPT_ALL = "all";

  protected void handleTooMany(List<Module> candidates, List<String> args) {
    String arglist = StringUtils.join(args.iterator(), " ");
    out.println("There's more than one integration module found matching the arguments '" + arglist + "':\n");
    for (Module candidate : candidates) {
      out.println("  * " + candidate.artifactId() + " " + candidate.version() + " " + candidate.groupId());
    }
    out.println();
    out.println("Try to use both version and group-id arguments in the command to be more specific.");
  }

  protected void handleNoArgs() {
    out.println("You need to at least specify the name of the integration module as argument.");

    if (!isAllSupported()) return;
    out.println("Alternatively, you can use --" + LONGOPT_ALL + " option to " + this.name() + " everything.");
  }

  protected boolean isAllSupported() {
    return options.hasOption(LONGOPT_ALL);
  }

  protected void handleNoneFound(List<String> args) {
    String arglist = StringUtils.join(args.iterator(), " ");
    out.println("No module found matching the arguments specified: " + arglist);
    out.println("Check that you've spelled them correctly or are in the right order.");
    throw new ModuleNotFoundException(arglist);
  }

  protected boolean hasAllOption(CommandLine cli) {
    return isAllSupported() && cli.hasOption(LONGOPT_ALL);
  }

  protected abstract void handleOne(Module module);

  protected abstract void handleAll();

  protected void process(CommandLine cli, Modules list) {
    // --all was specified, install everything
    if (hasAllOption(cli)) {
      handleAll();
      return;
    }

    // no args and --all not specified, ask user to be more specific
    List<String> args = cli.getArgList();
    if (args.isEmpty()) {
      handleNoArgs();
      return;
    }

    // given the artifactId and maybe the version and groupId - find some candidates get candidates
    Module module = null;
    List<Module> candidates = list.find(args);

    // no candidates found, inform the user
    if (candidates.isEmpty()) {
      handleNoneFound(args);
      return;
    }

    // several candidates found, see if we can figure out which one we can retrieve
    module = ModuleHelper.getLatest(candidates);
    if (module != null) {
      handleOne(module);
      return;
    }

    // we can't figure out which one to retrieve so ask the user to be more specific
    handleTooMany(candidates, args);
  }
  //

}
