/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.weblogic10x;

import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.StandardAppServerParameters;

import java.io.File;
import java.util.Properties;

/**
 * This class creates specific implementations of return values for the given methods.
 */
public final class Weblogic10xAppServerFactory extends AppServerFactory {

  // This class may only be instantiated by it's parent which contains the ProtectedKey
  public Weblogic10xAppServerFactory(ProtectedKey protectedKey) {
    super(protectedKey);
  }

  public AppServerParameters createParameters(String instanceName, Properties props) {
    return new StandardAppServerParameters(instanceName, props);
  }

  public AppServer createAppServer(AppServerInstallation installation) {
    return new Weblogic10xAppServer((Weblogic10xAppServerInstallation) installation);
  }

  public AppServerInstallation createInstallation(File home, File workingDir, AppServerInfo appServerInfo)
      throws Exception {
    return new Weblogic10xAppServerInstallation(home, workingDir, appServerInfo);
  }
}
