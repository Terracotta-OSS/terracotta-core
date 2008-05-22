/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.glassfish;

import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.AbstractAppServerInstallation;

import java.io.File;

/**
 * Defines the appserver name used by the installation process.
 */
public final class GlassfishAppServerInstallation extends AbstractAppServerInstallation {

  public GlassfishAppServerInstallation(File home, File workingDir, AppServerInfo appServerInfo) throws Exception {
    super(home, workingDir, appServerInfo);
  }

  public String serverType() {
    return "glassfish";
  }
}
