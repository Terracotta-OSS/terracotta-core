/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.jboss4x;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.jboss.JBoss4xInstalledLocalContainer;
import org.codehaus.cargo.container.property.GeneralPropertySet;

import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.test.server.util.AppServerUtil;

import java.io.File;
import java.io.IOException;

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

  protected InstalledLocalContainer container(LocalConfiguration config) {
    return new JBoss4xInstalledLocalContainer(config);
  }

  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    config.setProperty(GeneralPropertySet.RMI_PORT, Integer.toString(AppServerUtil.getPort()));
  }
  
  protected void initiateStartupAppender(File sandboxDir) throws IOException {
    new JBoss4xStartupAppender().pack(sandboxDir);
  }
}
