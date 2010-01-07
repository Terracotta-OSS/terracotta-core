/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.tomcat6x;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.State;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.tomcat.Tomcat6xInstalledLocalContainer;

import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.test.server.appserver.tomcat.TomcatStartupActions;
import com.tc.test.server.util.AppServerUtil;

/**
 * Tomcat6x AppServer implementation
 */
public final class Tomcat6xAppServer extends CargoAppServer {

  public Tomcat6xAppServer(Tomcat6xAppServerInstallation installation) {
    super(installation);
  }

  @Override
  protected String cargoServerKey() {
    return "tomcat6x";
  }

  @Override
  protected InstalledLocalContainer container(LocalConfiguration config, AppServerParameters params) {
    return new TCTomcat6xInstalledLocalContainer(config, params);
  }

  @Override
  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    config.setProperty(GeneralPropertySet.RMI_PORT, Integer.toString(AppServerUtil.getPort()));
  }

  private static class TCTomcat6xInstalledLocalContainer extends Tomcat6xInstalledLocalContainer {

    private final AppServerParameters params;

    public TCTomcat6xInstalledLocalContainer(LocalConfiguration config, AppServerParameters params) {
      super(config);
      this.params = params;
    }

    @Override
    protected void setState(State state) {
      if (state.isStarting()) {
        TomcatStartupActions.modifyConfig(params, this, 47);
      }

      super.setState(state);
    }
  }

}
