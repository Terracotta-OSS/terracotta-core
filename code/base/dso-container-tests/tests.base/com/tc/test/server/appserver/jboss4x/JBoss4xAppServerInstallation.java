/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.jboss4x;

import com.tc.test.server.appserver.AbstractAppServerInstallation;

import java.io.File;
import java.net.URL;

/**
 * Defines the appserver name used by the installation process.
 */
public final class JBoss4xAppServerInstallation extends AbstractAppServerInstallation {

  public JBoss4xAppServerInstallation(URL host, File serverDir, File workingDir, String majorVersion, String minorVersion) throws Exception {
    super(host, serverDir, workingDir, majorVersion, minorVersion);
  }
  
  public JBoss4xAppServerInstallation(File home, File workingDir, String majorVersion, String minorVersion) throws Exception {
    super(home, workingDir, majorVersion, minorVersion);
  }

  public String serverType() {
    return "jboss4";
  }
}
