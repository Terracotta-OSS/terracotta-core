/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.lang.StringUtils;
import org.terracotta.modules.tool.AbstractModule;
import org.terracotta.modules.tool.InstallListener;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.ModuleReport;

import java.io.PrintWriter;

class DefaultInstallListener implements InstallListener {

  private final PrintWriter  out;
  private final ModuleReport report;

  public DefaultInstallListener(ModuleReport report, PrintWriter out) {
    this.report = report;
    this.out = out;
  }

  public void notify(Object source, InstallNotification type, String message) {
    String line0 = StringUtils.repeat(" ", 3) + StringUtils.capitalize(type.toString().replaceAll("_", " "));

    if (InstallNotification.STARTING.equals(type)) {
      line0 = "Installing " + report.title((AbstractModule) source);
      if (!((Module) source).dependencies().isEmpty()) line0 += " and dependencies";
      line0 += "...";
    }

    String line1 = StringUtils.isEmpty(message) ? "" : message;
    if (InstallNotification.INSTALLED.equals(type)) line1 = " - " + line1;
    else if (InstallNotification.SKIPPED.equals(type)) line1 = " - " + line1;
    else line1 = "\n" + StringUtils.repeat(" ", line0.length() + 2) + line1;
    if (!StringUtils.isEmpty(message)) line0 += ": " + report.title((AbstractModule) source) + line1;

    out.println(line0);
    return;
  }
}
