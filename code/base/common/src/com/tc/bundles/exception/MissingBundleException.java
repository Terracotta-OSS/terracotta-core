/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles.exception;

import org.osgi.framework.BundleException;

import com.tc.bundles.MavenToOSGi;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class MissingBundleException extends BundleException implements Hints {

  private String groupId;
  private String name;
  private String version;
  private List   repositories;
  private Stack  dependencyStack;

  public MissingBundleException(final String msg) {
    super(msg);
  }

  public MissingBundleException(final String msg, final Throwable cause) {
    super(msg, cause);
  }

  public MissingBundleException(final String msg, final String groupId, final String name, final String version,
                                final List repositories, final Stack dependencyStack) {
    super(msg);
    this.groupId = groupId;
    this.name = name;
    this.version = version;
    this.repositories = repositories;
    this.dependencyStack = dependencyStack;
  }

  private String expectedJarFilename() {
    return name + "-" + MavenToOSGi.projectVersionToBundleVersion(version) + ".jar";
  }

  private String expectedRepositories() {
    return repositoriesToString("\n\t");
  }

  private String expectedLocations() {
    return repositoriesToString(File.separatorChar + groupId.replace('.', File.separatorChar) + File.separatorChar
                                + name + File.separatorChar + version + File.separatorChar + expectedJarFilename()
                                + "\n\t")
           + repositoriesToString(File.separatorChar + expectedJarFilename() + "\n\t");
  }

  private String expectedAttributes() {
    return "Bundle-SymbolicName: " + MavenToOSGi.artifactIdToSymbolicName(groupId, name) + "\n\t" + "Bundle-Version: "
           + version.replace('-', '.');
  }

  private String repositoriesToString(String sep) {
    final StringBuffer repos = new StringBuffer();
    for (int j = 0; j < repositories.size(); j++)
      repos.append(canonicalPath(repositories.get(j).toString())).append(sep);
    return repos.toString();
  }

  private String canonicalPath(String path) {
    try {
      return (new File(path)).getCanonicalPath();
    } catch (IOException e) {
      return path;
    }
  }

  public String getHints() {
    String msg = getMessage();
    msg += "\n\nRepositories searched:\n\n\t" + expectedRepositories();
    msg += "\n\nExpected jar filename:\n\n\t" + expectedJarFilename();
    msg += "\n\nExpected locations for jar file:\n\n\t" + expectedLocations();
    msg += "\n\nExpected manifest attributes:\n\n\t" + expectedAttributes();
    if (dependencyStack != null) msg += "\n\nTIM dependency stack:\n\n" + dependencyStackAsString(dependencyStack);
    return msg;
  }

  private String dependencyStackAsString(Stack dependencyStack) {
    ByteArrayOutputStream bas = new ByteArrayOutputStream();
    BufferedOutputStream buf = new BufferedOutputStream(bas);
    printDependencyStack(dependencyStack, 0, 4, buf);
    return bas.toString();
  }

  private void printDependencyStack(Stack dependencyStack, int depth, int indent, OutputStream out) {
    try {
      for (Iterator i = dependencyStack.iterator(); i.hasNext();) {
        Object entry = i.next();
        if (entry instanceof Stack) {
          printDependencyStack((Stack) entry, depth + 1, indent, out);
          continue;
        }
        if (depth == 0) {
          out.write((entry.toString() + "\n").getBytes());
          continue;
        }
        for (int j = 0; j < (depth - 1) * indent; j++)
          out.write(" ".getBytes());
        out.write((" +- " + entry.toString() + "\n").getBytes());
      }
      out.flush();
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

}
