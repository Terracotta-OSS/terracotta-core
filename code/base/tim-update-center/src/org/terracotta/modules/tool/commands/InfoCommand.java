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

  private static final String LONGOPT_GROUPID = "group-id";

  private final Modules       modules;

  @Inject
  public InfoCommand(Modules modules) {
    this.modules = modules;
    assert modules != null : "modules is null";
    options.addOption(buildOption(LONGOPT_GROUPID,
                                  "Use this option to qualify the name of the TIM you are looking for", String.class));
  }

  public void execute(CommandLine cli) throws CommandException {
    List<String> args = cli.getArgList();
    if (args.isEmpty()) {
      String msg = "You need to at least specify the name of the Integration Module you wish to inspect";
      throw new CommandException(msg);
    }

    String artifactId = args.remove(0);
    String version = args.isEmpty() ? null : args.remove(0);
    String groupId = cli.getOptionValue(LONGOPT_GROUPID, ModuleId.DEFAULT_GROUPID);
    Module module = (version == null) ? modules.getLatest(groupId, artifactId) : modules.get(ModuleId
        .create(groupId, artifactId, version));
    if (module == null) {
      out.println("Integration Module '" + artifactId + "' not found");
      out.println("It might be using a groupId other than '" + groupId + "'");
      return;
    }
    module.printDetails(out);
  }

}
