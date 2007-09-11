/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.weblogic9x;

import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.test.server.appserver.war.DescriptorXml;
import com.tc.test.server.appserver.war.DtdWar;
import com.tc.test.server.appserver.war.War;

import java.io.File;
import java.util.Properties;

/**
 * This class creates specific implementations of return values for the given methods. To obtain an instance you must
 * call {@link NewAppServerFactory.createFactoryFromProperties()}.
 */
public final class Weblogic9xAppServerFactory extends AppServerFactory {

  // This class may only be instantiated by it's parent which contains the ProtectedKey
  public Weblogic9xAppServerFactory(ProtectedKey protectedKey, TestConfigObject config) {
    super(protectedKey, config);
  }

  public AppServerParameters createParameters(String instanceName, Properties props) {
    return new StandardAppServerParameters(instanceName, props);
  }

  public AppServer createAppServer(AppServerInstallation installation) {
    return new Weblogic9xAppServer((Weblogic9xAppServerInstallation) installation);
  }

  public AppServerInstallation createInstallation(File home, File workingDir) throws Exception {
    return new Weblogic9xAppServerInstallation(home, workingDir, config.appserverMajorVersion(), config
        .appserverMinorVersion());
  }

  public War createWar(String appName) {
    War war = new DtdWar(appName);
    DescriptorXml weblogicWar = new Weblogic9xDescriptorXml();
    war.addContainerSpecificXml(weblogicWar.getFileName(), weblogicWar.getBytes());
    return war;
  }
}
