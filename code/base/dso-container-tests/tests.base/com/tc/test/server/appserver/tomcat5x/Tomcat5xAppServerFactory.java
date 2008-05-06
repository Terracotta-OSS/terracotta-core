/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.tomcat5x;

import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.StandardAppServerParameters;

import java.io.File;
import java.util.Properties;

/**
 * This class creates specific implementations of return values for the given methods. To obtain an instance you must
 * call {@link NewAppServerFactory.createFactoryFromProperties()}.
 */
public final class Tomcat5xAppServerFactory extends AppServerFactory {

  // This class may only be instantiated by its parent which contains the ProtectedKey
  public Tomcat5xAppServerFactory(ProtectedKey protectedKey) {
    super(protectedKey);
  }

  public AppServerParameters createParameters(String instanceName, Properties props) {
    return new StandardAppServerParameters(instanceName, props);
  }

  public AppServer createAppServer(AppServerInstallation installation) {
    return new Tomcat5xAppServer((Tomcat5xAppServerInstallation) installation);
  }

  public AppServerInstallation createInstallation(File home, File workingDir, AppServerInfo appServerInfo)
      throws Exception {
    return new Tomcat5xAppServerInstallation(home, workingDir, appServerInfo);
  }
}
