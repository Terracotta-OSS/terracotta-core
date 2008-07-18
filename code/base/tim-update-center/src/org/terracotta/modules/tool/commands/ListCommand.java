/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.Modules;

import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command class implementing the <code>list</code> command.
 */
public class ListCommand extends AbstractCommand {

  private static final String OPTION_DETAILS  = "v";
  private static final String LONGOPT_DETAILS = "details";

  private final Modules       modules;

  @Inject
  public ListCommand(Modules modules) {
    this.modules = modules;
    assert modules != null : "modules is null";
    options.addOption(OPTION_DETAILS, LONGOPT_DETAILS, false, "Display detailed information");
    this.arguments
        .put("keyword",
             "OPTIONAL. Filters the list to those that contain this keyword. Multiple keywords may be specified");
  }

  public String syntax() {
    return "[keyword] [options]";
  }

  public String description() {
    return "List all available Integration Modules for TC " + modules.tcVersion();
  }

  private void displayWithDetails(List<Module> list) {
    for (Module module : list) {
      out.println();
      module.printSummary(out);
    }
  }

  private void display(List<Module> list) {
    out.println();
    for (Module module : list) {
      module.printDigest(out);
    }
  }

  private boolean isQualified(List<String> keywords, String text) {
    if (keywords.isEmpty()) return true;
    for (String keyword : keywords) {
      Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(text);
      if (matcher.find()) return true;
    }
    return false;
  }

  public void execute(CommandLine cli) {
    List<Module> latest = modules.listLatest();
    List<String> keywords = cli.getArgList();

    List<Module> list = new ArrayList<Module>();
    for (Module module : latest) {
      if (!isQualified(keywords, module.getSymbolicName())) continue;
      list.add(module);
    }

    out.println("\n*** Terracotta Integration Modules for TC " + modules.tcVersion() + " ***");
    if (cli.hasOption('v') || cli.hasOption(LONGOPT_DETAILS)) displayWithDetails(list);
    else display(list);

    if (!list.isEmpty()) {
      out.println();
      out.println("legends: [+] already installed  [!] installed but newer version exists  [-] not installed");
      
    }
       
  }
}
