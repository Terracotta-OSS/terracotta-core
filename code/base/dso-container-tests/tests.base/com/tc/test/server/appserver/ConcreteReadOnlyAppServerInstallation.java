/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver;

import com.tc.test.server.util.AppServerUtil;
import com.tc.util.ZipBuilder;
import com.tc.util.runtime.Os;

import java.io.BufferedInputStream;
import java.io.File;
import java.net.URL;

/**
 * This class manages the installation of the read-only portion of a running appserver. This class should not be
 * referenced by any class other than {@link AbstractAppServerInstallation}.
 */
final class ConcreteReadOnlyAppServerInstallation {

  static File create(URL host, File serverDir, String serverType, String majorVersion, String minorVersion)
      throws Exception {
    File serverInstallDir = new File(serverDir, AppServerUtil.getFullName(serverType, majorVersion, minorVersion));
    if (!serverInstallDir.exists()) {
      serverInstallDir.mkdirs();
      URL appUrl = appendPath(host, serverType, majorVersion, minorVersion);
      System.out.println("Downloading: " + appUrl);
      BufferedInputStream in = new BufferedInputStream(appUrl.openStream());
      ZipBuilder.unzip(in, serverDir);
      in.close();
      System.out.println("Unzip to: " + serverDir);
    } else {
      System.out.println("Cached version of appserver found: " + serverInstallDir);
    }
    return serverDir;
  }

  private static URL appendPath(URL host, String serverType, String majorVersion, String minorVersion) throws Exception {
    String baseUrl = host.toExternalForm();
    String appendedPath = serverType + "/" + Os.platform() + "/"
                          + AppServerUtil.getFullName(serverType, majorVersion, minorVersion) + ".zip";
    return new URL(baseUrl + "/" + appendedPath);
  }
}
