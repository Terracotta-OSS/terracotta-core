/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.tomcat5x;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.tomcat.Tomcat5xInstalledLocalContainer;

import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.test.server.util.AppServerUtil;

/**
 * Tomcat5x AppServer implementation
 */
public final class Tomcat5xAppServer extends CargoAppServer {

  public Tomcat5xAppServer(Tomcat5xAppServerInstallation installation) {
    super(installation);
  }

  protected String cargoServerKey() {
    return "tomcat5x";
  }

  protected InstalledLocalContainer container(LocalConfiguration config) {
    return new Tomcat5xInstalledLocalContainer(config);
  }

  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    config.setProperty(GeneralPropertySet.RMI_PORT, Integer.toString(AppServerUtil.getPort()));
  }
}
