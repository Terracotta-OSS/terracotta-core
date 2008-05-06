/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles.exception;

import org.osgi.framework.BundleException;

import com.tc.bundles.OSGiToMaven;
import com.tc.bundles.MavenToOSGi;
import com.tc.bundles.ResolverUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
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
    final StringBuffer repos = new StringBuffer();
    final List paths = ResolverUtils.searchPathnames(repositories, groupId, name, version);
    for (Iterator i = paths.iterator(); i.hasNext();) {
      repos.append("+ ").append((String) i.next()).append("\n").append(INDENT + INDENT);
    }
    return repos.toString();
  }

  private String searchAttributes() {
    return "groupId: " + groupId + "\n" + INDENT + INDENT + "name   : " + name + "\n" + INDENT + INDENT + "Version: "
           + version;
  }

  private String expectedAttributes() {
    return "Bundle-SymbolicName: " + MavenToOSGi.artifactIdToSymbolicName(groupId, name) + "\n" + INDENT + INDENT
           + "Bundle-Version     : " + MavenToOSGi.projectVersionToBundleVersion(version);
  }

  private String searchedRepositories() {
    final StringBuffer repos = new StringBuffer();
    for (int j = 0; j < repositories.size(); j++) {
      String root = ResolverUtils.canonicalize(repositories.get(j).toString());
      repos.append("+ ").append(root).append("\n").append(INDENT + INDENT);
    }
    return repos.toString();
  }

  public String getSummary() {
    StringBuffer buf = new StringBuffer(getMessage()).append("\n\n").append(INDENT);
    buf.append("Attempted to resolve the TIM using the following descriptors:\n\n").append(INDENT + INDENT);
    buf.append(searchAttributes()).append("\n\n").append(INDENT);
    buf.append("Expected the TIM's filename to be:\n\n").append(INDENT + INDENT);
    buf.append(OSGiToMaven.makeBundleFilename(name, version)).append("\n\n").append(INDENT);
    buf.append("Expected these attributes to be in the manifest:\n\n").append(INDENT + INDENT);
    buf.append(expectedAttributes()).append("\n\n").append(INDENT);
    buf.append("Searched using the following repositories:\n\n").append(INDENT + INDENT);
    buf.append(searchedRepositories()).append("\n").append(INDENT);
    buf.append("Tried to resolve the jar file using the following paths:\n\n").append(INDENT + INDENT);
    buf.append(expectedPaths()).append("\n").append(INDENT);
    if (dependencyStack != null) {
      buf.append("The following shows the dependencies path the resolver took and why it ");
      buf.append("needed to locate the missing TIM:\n\n");
      buf.append(dependencyStackAsString(dependencyStack)).append("\n").append(INDENT);
    }
    buf.append("If the jar file exists and is in one of the paths listed\n").append(INDENT);
    buf.append("above, make sure that the Bundle-SymbolicName and\n").append(INDENT);
    buf.append("Bundle-Version attribute in its manifest matches the ones\n").append(INDENT);
    buf.append("that the resolver expects.");
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
