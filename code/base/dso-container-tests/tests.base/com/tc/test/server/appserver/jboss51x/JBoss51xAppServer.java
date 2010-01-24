/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.jboss51x;

import org.apache.tools.ant.taskdefs.Java;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.jboss.JBoss51xInstalledLocalContainer;
import org.codehaus.cargo.container.jboss.JBossPropertySet;
import org.codehaus.cargo.container.property.GeneralPropertySet;

import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.test.server.appserver.jboss_common.JBossHelper;
import com.tc.test.server.util.AppServerUtil;

import java.io.File;
import java.util.Collection;

/**
 * JBoss42x AppServer implementation
 */
public final class JBoss51xAppServer extends CargoAppServer {

  public JBoss51xAppServer(JBoss51xAppServerInstallation installation) {
    super(installation);
  }

  @Override
  protected String cargoServerKey() {
    return "jboss51x";
  }

  @Override
  protected InstalledLocalContainer container(LocalConfiguration config, AppServerParameters params) {
    return new TCJBoss51xInstalledLocalContainer(config, params.sars(), appServerInfo(), params.tomcatServerJars());
  }

  @Override
  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    config.setProperty(GeneralPropertySet.RMI_PORT, Integer.toString(AppServerUtil.getPort()));
    config.setProperty(JBossPropertySet.JBOSS51X_AJP_CONNECTOR_PORT, Integer.toString(AppServerUtil.getPort()));
    config.setProperty(JBossPropertySet.JBOSS51X_HTTP_CONNECTOR_PORT, Integer.toString(AppServerUtil.getPort()));
  }

  private static class TCJBoss51xInstalledLocalContainer extends JBoss51xInstalledLocalContainer {
    private final Collection         sars;
    private final AppServerInfo      appServerInfo;
    private final Collection<String> tomcatServerJars;

    public TCJBoss51xInstalledLocalContainer(LocalConfiguration configuration, Collection sars,
                                             AppServerInfo appServerInfo, Collection<String> tomcatServerJars) {
      super(configuration);
      this.sars = sars;
      this.appServerInfo = appServerInfo;
      this.tomcatServerJars = tomcatServerJars;
    }

    @Override
    protected void doStart(Java java) throws Exception {
      JBossHelper.startupActions(new File(getConfiguration().getHome()), sars, appServerInfo, tomcatServerJars);
      super.doStart(java);
    }
  }

}
