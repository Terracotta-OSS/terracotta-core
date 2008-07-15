/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.jboss4x;

import org.apache.tools.ant.taskdefs.Java;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.jboss.JBoss4xInstalledLocalContainer;
import org.codehaus.cargo.container.property.GeneralPropertySet;

import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.test.server.appserver.jboss_common.JBossHelper;
import com.tc.test.server.util.AppServerUtil;

import java.io.File;
import java.util.Collection;

/**
 * JBoss4x AppServer implementation
 */
public final class JBoss4xAppServer extends CargoAppServer {

  public JBoss4xAppServer(JBoss4xAppServerInstallation installation) {
    super(installation);
  }

  protected String cargoServerKey() {
    return "jboss4x";
  }

  protected InstalledLocalContainer container(LocalConfiguration config, AppServerParameters params) {
    return new TCJBoss4xInstalledLocalContainer(config, params.sars());
  }

  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    config.setProperty(GeneralPropertySet.RMI_PORT, Integer.toString(AppServerUtil.getPort()));
  }

  private static class TCJBoss4xInstalledLocalContainer extends JBoss4xInstalledLocalContainer {
    private final Collection sars;

    public TCJBoss4xInstalledLocalContainer(LocalConfiguration configuration, Collection sars) {
      super(configuration);
      this.sars = sars;
    }

    protected void doStart(Java java) throws Exception {
      JBossHelper.startupActions(new File(getConfiguration().getHome()), sars);
      super.doStart(java);
    }
  }

}
