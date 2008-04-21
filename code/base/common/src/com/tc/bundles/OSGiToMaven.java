/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import java.io.File;

public class OSGiToMaven {

  private static String NAME  = "\\w([0-9a-zA-Z\\-_])+\\w";
  private static String GROUP = NAME + "\\.";

  public static String artifactIdFromSymbolicName(final String symbolicName) {
    String name = symbolicName;
    while (true) {
      String last = name;
      name = name.replaceFirst(GROUP, "");
      if ((name.equals(last)) || name.matches("[0-9\\-_].*")) {
        name = last;
        break;
      }
    }
    return name;
  }

  public static String groupIdFromSymbolicName(final String symbolicName) {
    String name = artifactIdFromSymbolicName(symbolicName);
    int i = symbolicName.lastIndexOf(name);
    return symbolicName.substring(0, i > 0 ? i - 1 : i);
  }

  public static String bundleVersionToProjectVersion(final String bundleVersion) {
    String qualifier = bundleVersion.replaceAll("\\d(\\.|-)?", "");
    int i = bundleVersion.lastIndexOf(qualifier) - 1;
    String version = qualifier.length() > 0 ? bundleVersion.substring(0, i) : bundleVersion;
    qualifier = qualifier.length() > 0 ? "-" + qualifier : qualifier;
    return version + qualifier;
  }

  public static String makeBundleFilename(final String symbolicName, final String version) {
    return artifactIdFromSymbolicName(symbolicName) + "-" + bundleVersionToProjectVersion(version) + ".jar";
  }

  public static String makeBundlePathname(final String root, final String symbolicName, final String version) {
    String groupId = groupIdFromSymbolicName(symbolicName);
    String artifactId = artifactIdFromSymbolicName(symbolicName);
    return makeBundlePathname(root, groupId, artifactId, version);
  }

  public static String makeBundlePathname(final String root, final String groupId, final String artifactId,
                                          final String version) {
    return root + File.separatorChar + groupId.replace('.', File.separatorChar) + File.separatorChar + artifactId
           + File.separatorChar + bundleVersionToProjectVersion(version) + File.separatorChar
           + makeBundleFilename(groupId + "." + artifactId, version);
  }

  public static String makeFlatBundlePathname(final String root, final String groupId, final String artifactId,
                                              final String version) {
    return root + File.separatorChar + makeBundleFilename(groupId + "." + artifactId, version);
  }

}
