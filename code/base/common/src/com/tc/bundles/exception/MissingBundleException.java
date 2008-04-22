/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles.exception;

import org.osgi.framework.BundleException;

import com.tc.bundles.OSGiToMaven;
import com.tc.bundles.MavenToOSGi;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class MissingBundleException extends BundleException implements BundleExceptionSummary {

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

  private String expectedPaths() {
    String mavenStyle = repositoriesToString(OSGiToMaven.makeBundlePathname("", groupId, name, version) + "\n" + INDENT
                                             + INDENT);
    String flatStyle = repositoriesToString(OSGiToMaven.makeFlatBundlePathname("", name, version, false) + "\n"
                                            + INDENT + INDENT);
    return mavenStyle + flatStyle;
  }

  private String searchAttributes() {
    return "groupId: " + groupId + "\n" + INDENT + INDENT + "name   : " + name + "\n" + INDENT + INDENT + "Version: "
           + version;
  }

  private String expectedAttributes() {
    return "Bundle-SymbolicName: " + MavenToOSGi.artifactIdToSymbolicName(groupId, name) + "\n" + INDENT + INDENT
           + "Bundle-Version     : " + MavenToOSGi.projectVersionToBundleVersion(version);
  }

  private String repositoriesToString(String sep) {
    final StringBuffer repos = new StringBuffer();
    for (int j = 0; j < repositories.size(); j++)
      repos.append("+ ").append(canonicalPath(repositories.get(j).toString())).append(sep);
    return repos.toString();
  }

  private String canonicalPath(String path) {
    try {
      return (new File(path)).getCanonicalPath();
    } catch (IOException e) {
      return path;
    }
  }

  public String getSummary() {
    StringBuffer buf = new StringBuffer(getMessage());
    buf.append("\n\n").append(INDENT).append("Attempted to resolve the TIM using the following descriptors:\n\n");
    buf.append(INDENT + INDENT).append(searchAttributes());
    buf.append("\n\n").append(INDENT).append("Expected the TIM's filename to be:\n\n");
    buf.append(INDENT + INDENT).append(OSGiToMaven.makeBundleFilename(name, version));
    buf.append("\n\n").append(INDENT).append("Expected these attributes to be in the manifest:\n\n");
    buf.append(INDENT + INDENT).append(expectedAttributes());
    buf.append("\n\n").append(INDENT).append("Searched using the following repositories:\n\n");
    buf.append(INDENT + INDENT).append(repositoriesToString("\n" + INDENT + INDENT));
    buf.append("\n").append(INDENT).append("Tried to resolve the jar file using the following paths:\n\n");
    buf.append(INDENT + INDENT).append(expectedPaths());
    if (dependencyStack != null) {
      buf
          .append("\n")
          .append(INDENT)
          .append(
                  "The following shows the dependencies path the resolver took and why it needed to locate the missing TIM:\n\n");
      buf.append(dependencyStackAsString(dependencyStack));
    }
    buf.append("\n").append(INDENT);
    buf
        .append(
                "If the jar file exists and is in one of the paths listed above, make sure that the Bundle-SymbolicName\n")
        .append(INDENT);
    buf.append("and Bundle-Version attribute in its manifest matches the ones that the resolver expects.");
    return buf.toString();
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
          out.write((INDENT + INDENT + entry.toString() + "\n").getBytes());
          continue;
        }
        out.write((INDENT + INDENT).getBytes());
        for (int j = 0; j < (depth - 1) * indent; j++)
          out.write(" ".getBytes());
        out.write(("+- " + entry.toString() + "\n").getBytes());
      }
      out.flush();
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

}
