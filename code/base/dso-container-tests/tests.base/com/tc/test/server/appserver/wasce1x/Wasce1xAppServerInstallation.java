/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.wasce1x;

import com.tc.test.server.appserver.AbstractAppServerInstallation;

import java.io.File;
import java.net.URL;

/**
 * Defines the appserver name used by the installation process.
 */
public final class Wasce1xAppServerInstallation extends AbstractAppServerInstallation {

  public Wasce1xAppServerInstallation(URL host, File serverDir, File workingDir, String majorVersion,
                                      String minorVersion) throws Exception {
    super(host, serverDir, workingDir, majorVersion, minorVersion);
  }

  public Wasce1xAppServerInstallation(File home, File workingDir, String majorVersion, String minorVersion)
      throws Exception {
    super(home, workingDir, majorVersion, minorVersion);
  }

  public String serverType() {
    return "wasce";
  }
}
