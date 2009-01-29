/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command class implementing the <code>list</code> command.
 */
public class ListCommand extends ModuleOperatorCommand {

  private static final String OPTION_DETAILS  = "v";
  private static final String LONGOPT_DETAILS = "details";
  private static final String OPTION_ALL      = "a";
  private static final String LONGOPT_ALL     = "all";

  public ListCommand() {
    options.addOption(OPTION_DETAILS, LONGOPT_DETAILS, false, "Display detailed information");
    options.addOption(OPTION_ALL, LONGOPT_ALL, false, "Include internal integration modules (hidden by default)");
    arguments.put("keywords", "(OPTIONAL) Space delimited list of keywords used to filter the list.");
  }

  @Override
  public String syntax() {
    return "[keywords] {options}";
  }

  @Override
  public String description() {
    return "List all available integration modules for TC " + modules.tcVersion();
  }

  private void displayWithDetails(List<Module> list) {
    for (Module module : list) {
      report.printDigest(module, out);
      out.println();
    }
    report.printFooter(null, out);
  }

  private void display(List<Module> list) {
    for (Module module : list) {
      report.printHeadline(module, out);
    }
    out.println();
    report.printFooter(null, out);
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

    boolean includeTcInternal = includeTcInternal(cli);
    List<Module> list = new ArrayList<Module>();
    for (Module module : latest) {
      if (!isQualified(keywords, module.symbolicName())) continue;
      if (includeTcInternal) {
        list.add(module);
      } else {
        if (!module.tcInternalTIM()) {
          list.add(module);
        }
      }
    }

    out.println("*** Terracotta Integration Modules for TC " + modules.tcVersion() + " ***\n");
    if (list.isEmpty()) {
      out.println("None listed.");
      return;//
    }

    if (cli.hasOption('v') || cli.hasOption(LONGOPT_DETAILS)) displayWithDetails(list);
    else display(list);
  }

  private boolean includeTcInternal(CommandLine cli) {
    return cli.hasOption("a") || cli.hasOption(LONGOPT_ALL);
  }
}
