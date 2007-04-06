/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.weblogic8x;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.weblogic.WebLogic8xInstalledLocalContainer;

import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.cargo.CargoAppServer;

import java.io.File;

/**
 * Weblogic8x AppServer implementation
 */
public final class Weblogic8xAppServer extends CargoAppServer {

  public Weblogic8xAppServer(Weblogic8xAppServerInstallation installation) {
    super(installation);
  }
  
  protected String cargoServerKey() {
    return "weblogic8x";
  }

  protected InstalledLocalContainer container(LocalConfiguration config) {
    return new WebLogic8xInstalledLocalContainer(config);
  }
  
  protected void setExtraClasspath(AppServerParameters params) {
    container().setExtraClasspath(params.classpath().split(String.valueOf(File.pathSeparatorChar)));
  }
  
  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    //config.setProperty(WebLogicPropertySet.DOMAIN, "domain");
  }
  
  protected void initiateStartupAppender(File sandboxDir) throws Exception {
    new Weblogic8xStartupAppender().pack(sandboxDir);
  }
}