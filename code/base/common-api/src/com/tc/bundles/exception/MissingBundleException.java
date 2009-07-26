/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles.exception;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleException;

import com.tc.bundles.DependencyStack;
import com.tc.bundles.MavenToOSGi;
import com.tc.bundles.OSGiToMaven;
import com.tc.bundles.Repository;
import com.tc.bundles.ResolverUtils;
import com.tc.util.Assert;
import com.tc.util.runtime.Os;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

public class MissingBundleException extends BundleException implements BundleExceptionSummary {

  private final String           groupId;
  private final String           name;
  private final String           version;
  private final List<Repository> repositories;
  private final DependencyStack  dependencyStack;

  public MissingBundleException(final String msg, final String groupId, final String name, final String version,
                                final List<Repository> repositories, final DependencyStack dependencyStack) {
    super(msg);
    Assert.assertNotNull(groupId);
    Assert.assertNotNull(name);
    Assert.assertNotNull(version);
    Assert.assertNotNull(repositories);
    Assert.assertNotNull(dependencyStack);
    this.groupId = groupId;
    this.name = name;
    this.version = version;
    this.repositories = repositories;
    this.dependencyStack = dependencyStack;
  }

  public MissingBundleException(final String msg, final String groupId, final String name, final String version,
                                final List<Repository> repositories) {
    this(msg, groupId, name, version, repositories, new DependencyStack());
  }

  private String expectedPaths() {
    final StringBuffer repos = new StringBuffer();
    final List<URL> urls = ResolverUtils.searchRepos(repositories, groupId, name, version);
    for (URL url : urls) {
      repos.append("+ ").append(url).append("\n").append(INDENT + INDENT);
    }
    return repos.toString();
  }

  private String searchAttributes() {
    return "groupId: " + groupId + "\n" + INDENT + INDENT + "name   : " + name + "\n" + INDENT + INDENT + "version: "
           + OSGiToMaven.bundleVersionToProjectVersion(version);
  }

  private String expectedAttributes() {
    return "Bundle-SymbolicName: " + MavenToOSGi.artifactIdToSymbolicName(groupId, name) + "\n" + INDENT + INDENT
           + "Bundle-Version     : " + MavenToOSGi.projectVersionToBundleVersion(version);
  }

  private String searchedRepositories() {
    final StringBuffer repos = new StringBuffer();
    for (int j = 0; j < repositories.size(); j++) {
      String root = ResolverUtils.canonicalize(repositories.get(j).describe());
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

    if (repositories.size() == 0) {
      buf.append("There were no repositories declared where the TIM might have been installed.\n\n");
    } else {
      buf.append("Searched using the following repositories:\n\n").append(INDENT + INDENT);
      buf.append(searchedRepositories()).append("\n").append(INDENT);
      buf.append("Tried to resolve the jar file using the following paths:\n\n").append(INDENT + INDENT);
      buf.append(expectedPaths()).append("\n").append(INDENT);
    }

    if (dependencyStack.size() > 0) {
      buf.append("The following shows the dependencies path the resolver took and why it ");
      buf.append("needed to locate the missing TIM:\n\n");
      buf.append(dependencyStackAsString()).append("\n").append(INDENT);
    }

    buf.append("If the jar file exists and is in one of the paths listed ");
    buf.append("above, make sure that the Bundle-SymbolicName and\n").append(INDENT);
    buf.append("Bundle-Version attribute in its manifest matches the ones ");
    buf.append("that the resolver expects.\n\n").append(INDENT);

    buf.append("If you do not have this particular TIM or any of its ");
    buf.append("dependencies installed, try using the tim-get tool's \n").append(INDENT);
    buf.append("'install' command:\n\n").append(INDENT + INDENT);

    String promptname = Os.isWindows() ? "C:\\> " : "$ ";
    String scriptname = Os.isWindows() ? "tim-get.bat" : "tim-get.sh";

    buf.append(promptname).append(scriptname).append(" install ");
    buf.append(name).append(" ").append(OSGiToMaven.bundleVersionToProjectVersion(version)).append(" ").append(groupId);
    buf.append("\n\n").append(INDENT);

    buf.append("You can also use the tool's 'list' command to see if it's actually available:\n\n")
        .append(INDENT + INDENT);
    buf.append(promptname).append(scriptname).append(" list ").append(name);
    buf.append(INDENT).append("# list anything that has '").append(name).append("' in it's name");
    buf.append("\n").append(INDENT + INDENT);
    buf.append(promptname).append(scriptname).append(" list ").append(StringUtils.repeat(" ", name.length()));
    buf.append(INDENT).append("# or, list everything that is available");
    buf.append("\n\n").append(INDENT);

    buf.append("For more information on how to use the tim-get tool, invoke:\n\n").append(INDENT + INDENT);
    buf.append(promptname).append(scriptname).append(" help ");
    return StringUtils.replace(buf.toString(), "\n", System.getProperty("line.separator"));
  }

  private String dependencyStackAsString() {
    ByteArrayOutputStream bas = new ByteArrayOutputStream();
    BufferedOutputStream buf = new BufferedOutputStream(bas);
    printDependencyStack(dependencyStack, 0, 4, buf);
    return bas.toString();
  }

  private static void printDependencyStack(DependencyStack dependencies, int depth, int indent, OutputStream out) {
    try {
      for (Object entry : dependencies) {
        if (entry instanceof DependencyStack) {
          printDependencyStack((DependencyStack) entry, depth + 1, indent, out);
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
