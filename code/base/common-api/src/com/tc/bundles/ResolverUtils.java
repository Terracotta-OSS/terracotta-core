/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bundles;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ResolverUtils {

  public static List searchPathnames(List repositories, String groupId, String name, String version) {
    List paths = new ArrayList();
    for (Iterator i = repositories.iterator(); i.hasNext();) {
      String root = canonicalize((File) i.next());
      paths.add(OSGiToMaven.makeBundlePathname(root, groupId, name, version));
      paths.add(OSGiToMaven.makeFlatBundlePathname(root, name, version, false));
    }
    return paths;
  }

  public static String[] urlsToStrings(URL[] urls) {
    String[] strs = new String[urls.length];
    for (int i = 0; i < urls.length; i++) {
      File f = FileUtils.toFile(urls[i]);
      if (f != null) {
        strs[i] = f.getAbsolutePath();
      } else {
        strs[i] = urls[i].toExternalForm();
      }
    }
    return strs;
  }

  public static String canonicalize(URL url) {
    File path = FileUtils.toFile(url);
    if (path == null) return url.toString();
    return canonicalize(path);
  }

  public static String canonicalize(File path) {
    try {
      return path.getCanonicalPath();
    } catch (IOException e) {
      return path.toString();
    }
  }
  
  public static String canonicalize(String path) {
    return canonicalize(new File(path));
  }

}
