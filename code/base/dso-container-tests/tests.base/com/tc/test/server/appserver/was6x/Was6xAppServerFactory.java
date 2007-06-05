/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.was6x;

import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.NewAppServerFactory;
import com.tc.test.server.appserver.war.DtdWar;
import com.tc.test.server.appserver.war.War;
import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;

import java.io.File;
import java.net.URL;
import java.util.Properties;

/**
 * This class creates specific implementations of return values for the given methods. To obtain an instance you must
 * call {@link NewAppServerFactory.createFactoryFromProperties()}.
 */
public final class Was6xAppServerFactory extends NewAppServerFactory {

  // This class may only be instantiated by its parent which contains the ProtectedKey
  public Was6xAppServerFactory(ProtectedKey protectedKey, TestConfigObject config) {
    super(protectedKey, config);
  }

  public AppServerParameters createParameters(String instanceName, Properties props) {
    return new Was6xAppServerParameters(instanceName, props, config.sessionClasspath());
  }

  public AppServer createAppServer(AppServerInstallation installation) {
    return new Was6xAppServer((Was6xAppServerInstallation) installation);
  }

  public AppServerInstallation createInstallation(URL host, File serverDir, File workingDir) throws Exception {
    Was6xAppServerInstallation install = new Was6xAppServerInstallation(host, serverDir, workingDir, config
        .appserverMajorVersion(), config.appserverMinorVersion());
    return install;
  }

  public AppServerInstallation createInstallation(File home, File workingDir) throws Exception {
    return new Was6xAppServerInstallation(home, workingDir, config.appserverMajorVersion(), config
        .appserverMinorVersion());
  }

  public War createWar(String appName) {
    return new DtdWar(appName);
  }

  public StandardTerracottaAppServerConfig createTcConfig(File baseDir) {
    return new Was6xAppServerConfig(baseDir);
  }
}
