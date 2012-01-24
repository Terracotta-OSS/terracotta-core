/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import java.io.File;

public class OSGiToMaven {

  public static String artifactIdFromSymbolicName(final String symbolicName) {
    String name = symbolicName;
    while (true) {
      String last = name;
      name = name.replaceFirst("^[a-zA-Z][0-9a-zA-Z\\-_]*\\.", "");
      if ((name.equals(last)) || name.matches("[0-9\\-_].*")) {
        name = last;
        break;
      }
    }
    return name;
  }

  public static void main(String[] args) {
    System.err.println(mavenVersionFromOsgiVersion("1.0.SNAPSHOT"));
    System.err.println(mavenVersionFromOsgiVersion("1.0.0.SNAPSHOT"));
    System.err.println(mavenVersionFromOsgiVersion("1.0.0.patch1"));
  }

  public static String mavenVersionFromOsgiVersion(String version) {
    version = version.replace(".SNAPSHOT", "-SNAPSHOT");
    version = version.replace(".patch", "-patch");

    if (!version.startsWith("[") && !version.startsWith("(")) {
      version = "[" + version + ",]";
    }
    return version;
  }

  public static String groupIdFromSymbolicName(final String symbolicName) {
    String name = artifactIdFromSymbolicName(symbolicName);
    int i = symbolicName.lastIndexOf(name);
    return symbolicName.substring(0, i > 0 ? i - 1 : i);
  }

  public static String bundleVersionToProjectVersion(final String bundleVersion) {
    String version = bundleVersion.replaceAll("(\\.|-)[a-zA-Z][0-9a-zA-Z\\-_\\.]*$", "");
    String qualifier = bundleVersion.substring(version.length());
    qualifier = qualifier.length() > 0 ? "-" + qualifier.substring(1) : qualifier;
    return version + qualifier;
  }

  public static String makeBundleFilename(final String symbolicName, final String version) {
    return makeBundleFilename(symbolicName, version, true);
  }

  public static String makeBundleFilename(final String name, final String version, boolean isSymbolicName) {
    return (isSymbolicName ? artifactIdFromSymbolicName(name) : name) + "-" + bundleVersionToProjectVersion(version)
           + ".jar";
  }

  public static String makeBundlePathname(final String root, final String symbolicName, final String version) {
    String groupId = groupIdFromSymbolicName(symbolicName);
    String artifactId = artifactIdFromSymbolicName(symbolicName);
    return makeBundlePathname(root, groupId, artifactId, version);
  }

  public static String makeBundlePathname(final String root, final String groupId, final String artifactId,
                                          final String version) {
    StringBuffer buf = new StringBuffer(root).append('/');
    if (groupId.length() > 0) buf.append(groupId.replace('.', '/')).append('/');
    buf.append(artifactId).append('/');
    buf.append(bundleVersionToProjectVersion(version)).append('/');
    buf.append(makeBundleFilename(artifactId, version, false));
    return buf.toString().replace('/', File.separatorChar);
  }

  public static String makeBundlePathnamePrefix(final String root, final String groupId, final String artifactId) {
    StringBuffer buf = new StringBuffer(root).append('/');
    if (groupId.length() > 0) buf.append(groupId.replace('.', '/')).append('/');
    buf.append(artifactId).append('/');
    return buf.toString().replace('/', File.separatorChar);
  }

  public static String makeFlatBundlePathname(final String root, final String symbolicName, final String version) {
    return makeFlatBundlePathname(root, symbolicName, version, true);
  }

  public static String makeFlatBundlePathname(final String root, final String name, final String version,
                                              final boolean isSymbolicName) {
    String artifactId = isSymbolicName ? artifactIdFromSymbolicName(name) : name;
    StringBuffer buf = new StringBuffer(root).append('/');
    buf.append(makeBundleFilename(artifactId, version, isSymbolicName));
    return buf.toString().replace('/', File.separatorChar);
  }

}
