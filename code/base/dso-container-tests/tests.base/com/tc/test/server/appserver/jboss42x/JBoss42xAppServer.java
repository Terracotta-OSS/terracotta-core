/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.jboss42x;

import org.apache.tools.ant.taskdefs.Java;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.jboss.JBoss42xInstalledLocalContainer;
import org.codehaus.cargo.container.property.GeneralPropertySet;

import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.test.server.appserver.jboss_common.JBossHelper;
import com.tc.test.server.util.AppServerUtil;

import java.io.File;
import java.util.Collection;

/**
 * JBoss42x AppServer implementation
 */
public final class JBoss42xAppServer extends CargoAppServer {

  public JBoss42xAppServer(JBoss42xAppServerInstallation installation) {
    super(installation);
  }

  protected String cargoServerKey() {
    return "jboss42x";
  }

  protected InstalledLocalContainer container(LocalConfiguration config, AppServerParameters params) {
    return new TCJBoss42xInstalledLocalContainer(config, params.sars());
  }

  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    config.setProperty(GeneralPropertySet.RMI_PORT, Integer.toString(AppServerUtil.getPort()));
  }

  private static class TCJBoss42xInstalledLocalContainer extends JBoss42xInstalledLocalContainer {
    private final Collection sars;

    public TCJBoss42xInstalledLocalContainer(LocalConfiguration configuration, Collection sars) {
      super(configuration);
      this.sars = sars;
    }

    protected void doStart(Java java) throws Exception {
      JBossHelper.startupActions(new File(getConfiguration().getHome()), sars);
      super.doStart(java);
    }
  }

}
