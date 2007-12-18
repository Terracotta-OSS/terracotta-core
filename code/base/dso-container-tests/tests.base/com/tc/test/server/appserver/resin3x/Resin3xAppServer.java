/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.resin3x;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.resin.Resin2xInstalledLocalContainer;

import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.test.server.util.AppServerUtil;

/**
 * Resin3x AppServer implementation
 */
public final class Resin3xAppServer extends CargoAppServer {

  public Resin3xAppServer(Resin3xAppServerInstallation installation) {
    super(installation);
  }

  protected String cargoServerKey() {
    return "resin3x";
  }

  protected InstalledLocalContainer container(LocalConfiguration config) {
    return new Resin2xInstalledLocalContainer(config);
  }

  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    config.setProperty(GeneralPropertySet.RMI_PORT, Integer.toString(AppServerUtil.getPort()));
  }
}
