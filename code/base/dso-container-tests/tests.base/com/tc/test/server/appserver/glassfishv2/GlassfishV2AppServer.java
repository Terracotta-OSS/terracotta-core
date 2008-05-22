/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.glassfishv2;

import com.tc.test.server.appserver.glassfish.AbstractGlassfishAppServer;
import com.tc.test.server.appserver.glassfish.GlassfishAppServerInstallation;

import java.util.List;

public class GlassfishV2AppServer extends AbstractGlassfishAppServer {

  public GlassfishV2AppServer(GlassfishAppServerInstallation installation) {
    super(installation);
  }

  protected String[] getDisplayCommand(String script) {
    return new String[] { script, "cli", "display" };
  }

  protected void modifyStartupCommand(List cmd) {
    cmd.add(0, "-Dcom.sun.aas.promptForIdentity=true");
  }

}
