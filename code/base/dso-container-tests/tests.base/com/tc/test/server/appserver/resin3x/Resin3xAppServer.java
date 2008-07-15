/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.resin3x;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.resin.Resin3xInstalledLocalContainer;
import org.codehaus.cargo.container.resin.ResinPropertySet;

import com.tc.test.server.appserver.AppServerParameters;
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

  protected InstalledLocalContainer container(LocalConfiguration config, AppServerParameters params) {
    return new Resin3xInstalledLocalContainer(config);

  }

  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    config.setProperty(GeneralPropertySet.RMI_PORT, String.valueOf(AppServerUtil.getPort()));
    config.setProperty(ResinPropertySet.CLUSTER_PORT, String.valueOf(AppServerUtil.getPort()));
    config.setProperty(ResinPropertySet.INTERNAL_SOCKET_PORT, String.valueOf(AppServerUtil.getPort()));
    config.setProperty(ResinPropertySet.KEEP_ALIVE_SOCKET_PORT, String.valueOf(AppServerUtil.getPort()));
  }
}
