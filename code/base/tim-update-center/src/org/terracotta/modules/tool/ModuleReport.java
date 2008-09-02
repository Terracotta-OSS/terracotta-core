/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import java.io.PrintWriter;

public abstract class ModuleReport {

  public abstract String title(AbstractModule module);

  public abstract String headline(AbstractModule module);

  public abstract String digest(Module module);

  public abstract String summary(Module module);

  public abstract String footer(AbstractModule module);

  public abstract String header(AbstractModule module);

  public void printDigest(Module module, PrintWriter out) {
    out.println(digest(module));
  }

  public void printHeadline(AbstractModule module, PrintWriter out) {
    out.println(headline(module));
  }

  public void printSummary(Module module, PrintWriter out) {
    out.println(summary(module));
  }

  public void printFooter(AbstractModule module, PrintWriter out) {
    out.println(footer(module));
  }

  public void printHeader(AbstractModule module, PrintWriter out) {
    out.println(header(module));
  }
}
